package com.xiji.controller;

import com.xiji.common.response.ResultVo;
import com.xiji.entity.domain.BillUpload;
import com.xiji.entity.dto.request.BillImportRequest;
import com.xiji.entity.dto.response.BillImportResult;
import com.xiji.entity.dto.response.BillParseResult;
import com.xiji.service.BillImportService;
import com.xiji.service.BillParserService;
import com.xiji.service.BillUploadService;
import com.xiji.service.OssService;
import com.xiji.utils.RedisUtils;
import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 账单上传控制器
 * @author liberty
 */
@RestController
@RequestMapping("/api/v1/mobile/bills")
@Tag(name = "账单导入 V1", description = "账单上传、解析和导入相关接口（同步处理）")
@Slf4j
@RequiredArgsConstructor
public class BillUploadController extends BaseController {

    private final BillUploadService billUploadService;
    private final BillParserService billParserService;
    private final OssService ossService;
    private final RedisUtils redisUtils;
    private final Gson gson;
    private final BillImportService billImportService;

    // Redis缓存key前缀
    private static final String BILL_PARSE_CACHE_PREFIX = "bill:parse:";
    // 账单文件专用OSS目录
    private static final String BILL_OSS_FOLDER = "bills/";

    /**
     * 上传并解析账单文件（同步接口）
     */
    @Operation(summary = "上传并解析账单文件", description = "上传账单文件到OSS，同步解析并返回解析结果。解析结果会缓存24小时。")
    @PostMapping("/upload")
    public ResultVo uploadAndParseBill(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "platform", required = false) String platform,
            HttpServletRequest httpRequest) {

        Long userId = getCurrentUserId(httpRequest);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        Long familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        // 验证文件
        if (file == null || file.isEmpty()) {
            return ResultVo.error("文件不能为空");
        }

        String originalFileName = file.getOriginalFilename();

        // 验证文件类型
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf('.') + 1).toLowerCase();
        if (!Arrays.asList("xlsx", "xls", "csv").contains(fileExtension)) {
            return ResultVo.error("只支持Excel和CSV格式的文件");
        }

        // 验证文件大小（50MB）
        if (file.getSize() > 50 * 1024 * 1024) {
            return ResultVo.error("文件大小不能超过50MB");
        }

        try {
            // 1. 上传文件到OSS（使用账单专用目录）
            String objectKey = ossService.uploadFile(file, BILL_OSS_FOLDER);
            String fileUrl = ossService.buildFileUrl(objectKey);

            log.info("账单文件上传成功，用户ID：{}，文件名：{}，ObjectKey：{}", userId, originalFileName, objectKey);

            // 2. 创建或更新账单上传记录
            BillUpload billUpload = billUploadService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BillUpload>()
                    .eq(BillUpload::getFileId, objectKey)
                    .eq(BillUpload::getUserId, userId)
            );

            if (billUpload == null) {
                billUpload = new BillUpload();
                billUpload.setUserId(userId);
                billUpload.setFamilyId(familyId);
                billUpload.setFileId(objectKey);
                billUpload.setFileName(originalFileName);
                billUpload.setFileUrl(fileUrl);
                billUpload.setFileSize(file.getSize());
                billUpload.setStatus(0); // 已上传
                billUploadService.save(billUpload);
            } else {
                billUpload.setFileName(originalFileName);
                billUpload.setFileUrl(fileUrl);
                billUpload.setFileSize(file.getSize());
                billUpload.setStatus(0);
                billUploadService.updateById(billUpload);
            }

            // 3. 同步解析文件
            BillParseResult parseResult;
            try (InputStream inputStream = file.getInputStream()) {
                parseResult = billParserService.parseBill(
                        originalFileName, inputStream, fileExtension, platform
                );
            }

            // 4. 更新账单上传记录
            billUpload.setPlatform(parseResult.getPlatform());
            billUpload.setStatus(1); // 已解析
            billUpload.setTotalCount(parseResult.getTotalCount());
            billUpload.setSuccessCount(parseResult.getSuccessCount());
            billUpload.setErrorCount(parseResult.getErrorCount());
            billUpload.setParseResult(gson.toJson(parseResult.getMetadata()));
            billUploadService.updateById(billUpload);

            // 5. 将完整的解析结果（包括所有交易记录）缓存到Redis
            String cacheKey = BILL_PARSE_CACHE_PREFIX + billUpload.getId();
            String cacheValue = gson.toJson(parseResult);
            redisUtils.set(cacheKey, cacheValue, 24 * 3600L); // 24小时（秒）

            log.info("账单同步解析完成，用户ID：{}，billUploadId：{}，解析记录数：{}",
                userId, billUpload.getId(), parseResult.getSuccessCount());

            // 6. 返回解析结果（包含全部预览数据）
            BillParseResult response = new BillParseResult();
            response.setPlatform(parseResult.getPlatform());
            response.setTotalCount(parseResult.getTotalCount());
            response.setSuccessCount(parseResult.getSuccessCount());
            response.setErrorCount(parseResult.getErrorCount());
            response.setPreview(parseResult.getPreview());
            response.setErrors(parseResult.getErrors());
            response.setMetadata(parseResult.getMetadata());

            // 在metadata中添加billUploadId，用于后续导入
            if (response.getMetadata() == null) {
                response.setMetadata(new java.util.HashMap<>());
            }
            response.getMetadata().put("billUploadId", billUpload.getId().toString());

            return ResultVo.success(response);

        } catch (Exception e) {
            log.error("上传账单文件失败", e);
            return ResultVo.error("上传账单文件失败：" + e.getMessage());
        }
    }

    /**
     * 导入交易记录（同步接口）
     */
    @PostMapping("/import")
    @Operation(summary = "导入交易记录", description = "将解析的账单数据导入为交易记录。使用缓存数据，无需重新解析。")
    public ResultVo importBill(@Valid @RequestBody BillImportRequest request, HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        Long familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        try {
            // 1. 查询账单上传记录
            BillUpload billUpload = billUploadService.getById(request.getBillUploadId());

            if (billUpload == null) {
                return ResultVo.error("账单上传记录不存在");
            }

            if (!billUpload.getUserId().equals(userId)) {
                return ResultVo.error("无权访问该账单记录");
            }

            if (billUpload.getStatus() != 1) {
                return ResultVo.error("请先解析账单文件");
            }

            // 2. 验证Redis缓存是否存在
            String cacheKey = BILL_PARSE_CACHE_PREFIX + request.getBillUploadId();
            Object cachedData;
            try {
                cachedData = redisUtils.get(cacheKey);
            } catch (Exception e) {
                log.error("获取Redis缓存失败，billUploadId：{}", request.getBillUploadId(), e);
                return ResultVo.error("系统错误：Redis连接失败，请稍后重试或联系管理员");
            }

            if (cachedData == null) {
                return ResultVo.error("解析结果已过期，请重新上传并解析文件");
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
                return ResultVo.error("解析结果数据为空");
            }

            // 4. 从解析结果或账单上传记录中获取平台信息
            String platform = parseResult.getPlatform();
            if (platform == null || platform.isEmpty()) {
                platform = billUpload.getPlatform();
            }

            // 5. 同步导入交易记录
            BillImportResult importResult = billImportService.importTransactions(
                    userId, familyId, parseResult.getTransactions(), platform, request
            );

            // 6. 更新账单上传记录状态
            billUpload.setStatus(importResult.getFailCount() > 0 ? 3 : 2); // 2-已导入，3-导入失败
            billUploadService.updateById(billUpload);

            // 7. 导入成功后删除缓存
            redisUtils.delete(cacheKey);

            log.info("账单同步导入完成，用户ID：{}，billUploadId：{}，成功：{}，失败：{}",
                userId, billUpload.getId(), importResult.getSuccessCount(), importResult.getFailCount());

            return ResultVo.success(importResult);

        } catch (Exception e) {
            log.error("同步导入账单失败", e);
            return ResultVo.error("同步导入账单失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取支持的平台列表
     */
    @GetMapping("/platforms")
    @Operation(summary = "获取支持的平台列表", description = "获取系统支持的账单平台列表")
    public ResultVo getPlatforms() {
        List<com.xiji.parser.BillParser> parsers = billParserService.getAllParsers();
        List<com.xiji.entity.dto.response.BillPlatformInfo> platforms = parsers.stream()
                .filter(parser -> !"cmb".equals(parser.getPlatformCode()))
                .map(parser -> {
                    // 根据平台代码确定支持的文件格式
                    List<String> supportedFormats;
                    String sampleFileExtension;

                    String platformCode = parser.getPlatformCode();
                    if ("jd".equals(platformCode)) {
                        // 京东：只支持CSV
                        supportedFormats = Arrays.asList("csv");
                        sampleFileExtension = "csv";
                    } else if ("alipay".equals(platformCode)) {
                        // 支付宝：只支持CSV
                        supportedFormats = Arrays.asList("csv");
                        sampleFileExtension = "csv";
                    } else if ("wechat".equals(platformCode)) {
                        // 微信：只支持XLSX
                        supportedFormats = Arrays.asList("xlsx", "xls");
                        sampleFileExtension = "xlsx";
                    } else if ("cmb".equals(platformCode)) {
                        // 招商银行：只支持PDF
                        supportedFormats = Arrays.asList("pdf");
                        sampleFileExtension = "pdf";
                    } else {
                        // 其他平台：默认支持所有格式
                        supportedFormats = Arrays.asList("xlsx", "xls", "csv");
                        sampleFileExtension = "xlsx";
                    }

                    return new com.xiji.entity.dto.response.BillPlatformInfo(
                        parser.getPlatformCode(),
                        parser.getPlatformName(),
                        supportedFormats,
                        parser.getPlatformCode() + "_bill_sample." + sampleFileExtension
                    );
                })
                .collect(Collectors.toList());
        
        return ResultVo.success(platforms);
    }
}
