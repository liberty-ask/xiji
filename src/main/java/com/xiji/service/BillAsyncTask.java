package com.xiji.service;

import com.aliyun.oss.model.OSSObject;
import com.google.gson.Gson;
import com.xiji.entity.domain.BillTask;
import com.xiji.entity.dto.request.BillImportRequest;
import com.xiji.entity.dto.response.BillImportResult;
import com.xiji.entity.dto.response.BillParseResult;
import com.xiji.parser.model.BillTransaction;
import com.xiji.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

/**
 * 账单异步任务类
 * 用于处理耗时的账单解析和导入操作
 * @author liberty
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillAsyncTask {

    private final BillUploadService billUploadService;
    private final BillParserService billParserService;
    private final BillImportService billImportService;
    private final OssService ossService;
    private final RedisUtils redisUtils;
    private final BillTaskService billTaskService;
    private final Gson gson;

    // Redis缓存key前缀
    private static final String BILL_PARSE_CACHE_PREFIX = "bill:parse:";
    // 缓存过期时间（小时）
    private static final long BILL_PARSE_CACHE_EXPIRE_HOURS = 24;

    /**
     * 异步处理账单上传和解析
     * @param taskId 任务ID
     * @param billTask 任务实体
     * @param originalFileName 原始文件名
     * @param fileExtension 文件扩展名
     * @param platform 平台类型
     */
    @Async
    public void processBillUploadAndParse(Long taskId, BillTask billTask, String originalFileName, String fileExtension, String platform) {
        try {
            // 更新任务状态为处理中
            billTaskService.updateTaskStatus(taskId, 1, 0, 0, 0, 0, null);

            log.info("开始异步处理账单，taskId：{}，originalFileName：{}", taskId, originalFileName);

            // 1. 从OSS下载文件并解析
            OSSObject ossObject = ossService.downloadFile(billTask.getOssFilePath());
            try(InputStream inputStream = ossObject.getObjectContent();) {
                // 更新进度为20%
                billTaskService.updateTaskStatus(taskId, 1, 20, 0, 0, 0, null);

                // 解析文件
                BillParseResult parseResult = billParserService.parseBill(
                        originalFileName, inputStream, fileExtension, platform
                );

                // 更新进度为50%
                billTaskService.updateTaskStatus(taskId, 1, 50, parseResult.getTotalCount(), 0, 0, null);

                // 2. 更新账单上传记录
                com.xiji.entity.domain.BillUpload billUpload = billUploadService.getById(billTask.getBillUploadId());
                if (billUpload != null) {
                    billUpload.setPlatform(parseResult.getPlatform());
                    billUpload.setStatus(1); // 已解析
                    billUpload.setTotalCount(parseResult.getTotalCount());
                    billUpload.setSuccessCount(parseResult.getSuccessCount());
                    billUpload.setErrorCount(parseResult.getErrorCount());
                    billUpload.setParseResult(gson.toJson(parseResult.getMetadata()));
                    billUploadService.updateById(billUpload);
                }
                
                // 3. 更新任务记录的平台信息
                billTask.setPlatform(parseResult.getPlatform());
                billTaskService.updateById(billTask);

                // 3. 将完整的解析结果（包括所有交易记录）缓存到Redis
                String cacheKey = BILL_PARSE_CACHE_PREFIX + billTask.getBillUploadId();
                String cacheValue = gson.toJson(parseResult);
                redisUtils.set(cacheKey, cacheValue, BILL_PARSE_CACHE_EXPIRE_HOURS * 3600L); // 24小时（秒）

                // 更新进度为100%
                billTaskService.updateTaskStatus(taskId, 2, 100, parseResult.getTotalCount(), parseResult.getSuccessCount(), parseResult.getErrorCount(), null);

                log.info("异步处理账单完成，taskId：{}，解析记录数：{}", taskId, parseResult.getSuccessCount());

            }
        } catch (Exception e) {
            log.error("异步处理账单失败，taskId：{}", taskId, e);
            // 更新任务状态为失败
            billTaskService.updateTaskStatus(taskId, 3, 100, 0, 0, 0, e.getMessage());
        }
    }

    /**
     * 异步处理账单导入
     * @param taskId 任务ID
     * @param userId 用户ID
     * @param familyId 家庭ID
     * @param billUploadId 账单上传ID
     * @param request 导入请求
     */
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void processBillImport(Long taskId, Long userId, Long familyId, Long billUploadId, BillImportRequest request) {
        try {
            // 更新任务状态为处理中
            billTaskService.updateTaskStatus(taskId, 1, 0, 0, 0, 0, null);

            log.info("开始异步导入账单，taskId：{}，billUploadId：{}", taskId, billUploadId);

            // 1. 查询账单上传记录
            com.xiji.entity.domain.BillUpload billUpload = billUploadService.getById(billUploadId);

            if (billUpload == null) {
                billTaskService.updateTaskStatus(taskId, 3, 100, 0, 0, 0, "账单上传记录不存在");
                return;
            }

            // 2. 从Redis缓存中获取解析结果
            String cacheKey = BILL_PARSE_CACHE_PREFIX + billUploadId;
            Object cachedData = redisUtils.get(cacheKey);

            if (cachedData == null) {
                billTaskService.updateTaskStatus(taskId, 3, 100, 0, 0, 0, "解析结果已过期，请重新上传并解析文件");
                return;
            }

            // 3. 反序列化解析结果
            String parseResultJson;
            if (cachedData instanceof String) {
                parseResultJson = (String) cachedData;
            } else {
                parseResultJson = gson.toJson(cachedData);
            }

            BillParseResult parseResult = gson.fromJson(parseResultJson, BillParseResult.class);
            if (parseResult == null || parseResult.getTransactions() == null || parseResult.getTransactions().isEmpty()) {
                billTaskService.updateTaskStatus(taskId, 3, 100, 0, 0, 0, "解析结果数据为空");
                return;
            }

            // 总记录数
            int totalCount = parseResult.getTransactions().size();
            
            // 更新进度为30%
            billTaskService.updateTaskStatus(taskId, 1, 30, totalCount, 0, 0, null);

            // 4. 导入交易记录（使用缓存中的完整数据）
            // 从解析结果或账单上传记录中获取平台信息
            String platform = parseResult.getPlatform();
            if (platform == null || platform.isEmpty()) {
                platform = billUpload.getPlatform();
            }

            // 更新进度为50%
            billTaskService.updateTaskStatus(taskId, 1, 50, totalCount, 0, 0, null);

            // 分批导入交易记录
            BillImportResult importResult = batchImportTransactions(userId, familyId, parseResult.getTransactions(), platform, request, taskId);

            // 5. 更新账单上传记录状态
            billUpload.setStatus(importResult.getFailCount() > 0 ? 3 : 2); // 2-已导入，3-导入失败
            billUploadService.updateById(billUpload);

            // 6. 导入成功后删除缓存
            redisUtils.delete(cacheKey);

            // 7. 处理错误信息
            String errorMessage = null;
            if (importResult.getFailCount() > 0 && importResult.getErrors() != null && !importResult.getErrors().isEmpty()) {
                // 将错误列表转换为JSON字符串，限制长度不超过1000字符
                String errorsJson = gson.toJson(importResult.getErrors());
                if (errorsJson.length() > 1000) {
                    errorsJson = errorsJson.substring(0, 997) + "...";
                }
                errorMessage = errorsJson;
            }

            // 更新进度为100%
            billTaskService.updateTaskStatus(taskId, 2, 100, totalCount, importResult.getSuccessCount(), importResult.getFailCount(), errorMessage);

            log.info("异步导入账单完成，taskId：{}，成功：{}，失败：{}", 
                    taskId, importResult.getSuccessCount(), importResult.getFailCount());

        } catch (Exception e) {
            log.error("异步导入账单失败，taskId：{}", taskId, e);
            // 更新任务状态为失败
            billTaskService.updateTaskStatus(taskId, 3, 100, 0, 0, 0, e.getMessage());
        }
    }

    /**
     * 分批导入交易记录
     * @param userId 用户ID
     * @param familyId 家庭ID
     * @param transactions 交易记录列表
     * @param platform 平台类型
     * @param request 导入请求
     * @param taskId 任务ID
     * @return 导入结果
     */
    private BillImportResult batchImportTransactions(Long userId, Long familyId, List<BillTransaction> transactions, 
                                                    String platform, BillImportRequest request, Long taskId) {
        // 定义批次大小
        int batchSize = 500;
        int totalCount = transactions.size();
        int totalSuccess = 0;
        int totalFail = 0;

        // 创建导入结果对象
        BillImportResult result = new BillImportResult();
        result.setErrors(new java.util.ArrayList<>());
        result.setTransactionIds(new java.util.ArrayList<>());
        result.setTotalCount(totalCount);

        // 分批处理
        for (int i = 0; i < totalCount; i += batchSize) {
            // 计算当前批次的结束索引
            int endIndex = Math.min(i + batchSize, totalCount);
            // 获取当前批次的交易记录
            List<BillTransaction> batchTransactions = transactions.subList(i, endIndex);

            try {
                // 调用导入服务
                BillImportResult batchResult = billImportService.importTransactions(
                        userId, familyId, batchTransactions, platform, request
                );

                // 更新统计信息
                totalSuccess += batchResult.getSuccessCount();
                totalFail += batchResult.getFailCount();
                // 将跳过的重复记录计入失败数量
                totalFail += batchResult.getSkipCount();

                // 合并错误信息和交易ID
                result.getErrors().addAll(batchResult.getErrors());
                result.getTransactionIds().addAll(batchResult.getTransactionIds());

                // 更新进度
                int progress = (int) Math.min(80 + (i / (double) totalCount) * 20, 95);
                billTaskService.updateTaskStatus(taskId, 1, progress, totalCount, totalSuccess, totalFail, null);

            } catch (Exception e) {
                log.error("批次导入失败，批次范围：{} - {}", i, endIndex, e);
                totalFail += batchTransactions.size();
            }
        }

        // 设置最终结果
        result.setSuccessCount(totalSuccess);
        result.setFailCount(totalFail);
        result.setSkipCount(0);

        return result;
    }
}