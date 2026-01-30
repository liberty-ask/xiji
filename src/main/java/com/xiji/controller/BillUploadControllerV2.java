package com.xiji.controller;

import com.xiji.common.response.ResultVo;
import com.xiji.config.CustomConfig;
import com.xiji.entity.domain.BillTask;
import com.xiji.entity.domain.BillUpload;
import com.xiji.entity.dto.request.BillImportRequest;
import com.xiji.entity.dto.response.BillParseResult;
import com.xiji.entity.dto.response.BillTaskResponse;
import com.xiji.entity.dto.response.BillTaskWithResultResponse;
import com.xiji.service.BillAsyncTask;
import com.xiji.service.BillParserService;
import com.xiji.service.BillTaskService;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 账单上传控制器 V2版本
 * 处理异步账单上传、解析和导入相关接口
 * @author liberty
 */
@RestController
@RequestMapping("/api/v2/mobile/bills")
@Tag(name = "账单导入 V2", description = "账单上传、解析和导入相关接口（异步处理）")
@Slf4j
@RequiredArgsConstructor
public class BillUploadControllerV2 extends BaseController {
    
    private final BillUploadService billUploadService;
    private final BillParserService billParserService;
    private final OssService ossService;
    private final RedisUtils redisUtils;
    private final Gson gson;
    private final BillAsyncTask billAsyncTask;
    private final BillTaskService billTaskService;
    
    // Redis缓存key前缀
    private static final String BILL_PARSE_CACHE_PREFIX = "bill:parse:";
    // 账单文件专用OSS目录
    private static final String BILL_OSS_FOLDER = "bills/";
    
    /**
     * 上传并解析账单文件（异步接口）
     */
    @Operation(summary = "上传并解析账单文件", description = "上传账单文件到OSS，异步解析并返回任务ID。解析结果会缓存24小时。")
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
            
            // 3. 创建账单任务记录
            BillTask billTask = new BillTask();
            billTask.setUserId(userId);
            billTask.setFamilyId(familyId);
            billTask.setBillUploadId(billUpload.getId());
            billTask.setOriginalFileName(originalFileName);
            billTask.setFileSize(file.getSize());
            billTask.setOssFilePath(objectKey);
            billTask.setFileUrl(fileUrl);
            billTask.setTaskType(1); // 1-上传解析
            billTask.setStatus(0); // 0-待处理
            billTask.setProgress(0);
            billTask.setPlatform(platform); // 设置平台类型（可能为null，解析后会更新）
            billTaskService.save(billTask);
            
            // 4. 异步处理账单解析
            billAsyncTask.processBillUploadAndParse(billTask.getId(), billTask, originalFileName, fileExtension, platform);
            
            // 5. 立即返回任务ID给前端
            BillTaskResponse response = new BillTaskResponse();
            response.setTaskId(billTask.getId());
            response.setMessage("账单已开始处理，请稍后查询结果");
            
            log.info("账单处理任务已创建，用户ID：{}，taskId：{}，billUploadId：{}", 
                userId, billTask.getId(), billUpload.getId());
            
            return ResultVo.success(response);
            
        } catch (Exception e) {
            log.error("上传账单文件失败", e);
            return ResultVo.error("上传账单文件失败：" + e.getMessage());
        }
    }
    
    /**
     * 导入交易记录（异步接口）
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
            
            // 3. 创建账单任务记录
            BillTask billTask = new BillTask();
            billTask.setUserId(userId);
            billTask.setFamilyId(familyId);
            billTask.setBillUploadId(billUpload.getId());
            billTask.setOriginalFileName(billUpload.getFileName());
            billTask.setFileSize(billUpload.getFileSize());
            billTask.setOssFilePath(billUpload.getFileId());
            billTask.setFileUrl(billUpload.getFileUrl());
            billTask.setTaskType(2); // 2-导入
            billTask.setStatus(0); // 0-待处理
            billTask.setProgress(0);
            billTask.setPlatform(billUpload.getPlatform());
            billTaskService.save(billTask);
            
            // 4. 异步处理账单导入
            billAsyncTask.processBillImport(billTask.getId(), userId, familyId, billUpload.getId(), request);
            
            // 5. 立即返回任务ID给前端
            BillTaskResponse response = new BillTaskResponse();
            response.setTaskId(billTask.getId());
            response.setMessage("账单导入已开始，请稍后查询结果");
            
            log.info("账单导入任务已创建，用户ID：{}，taskId：{}，billUploadId：{}", 
                userId, billTask.getId(), billUpload.getId());
            
            return ResultVo.success(response);

        } catch (Exception e) {
            log.error("创建账单导入任务失败", e);
            return ResultVo.error("创建账单导入任务失败：" + e.getMessage());
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
    
    /**
     * 查询任务状态
     */
    @GetMapping("/task/status/{taskId}")
    @Operation(summary = "查询任务状态", description = "查询账单处理任务的状态和进度")
    public ResultVo getTaskStatus(@PathVariable Long taskId, HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }
        
        // 查询任务
        BillTask billTask = billTaskService.getById(taskId);
        if (billTask == null) {
            return ResultVo.error("任务不存在");
        }
        
        // 验证任务归属
        if (!billTask.getUserId().equals(userId)) {
            return ResultVo.error("无权访问该任务");
        }
        
        // 创建响应对象
        BillTaskWithResultResponse response = new BillTaskWithResultResponse();
        response.setTask(billTask);
        
        // 如果是上传解析任务且已成功，返回解析结果
        if (billTask.getTaskType() == 1 && billTask.getStatus() == 2) {
            // 从Redis中获取解析结果
            String cacheKey = BILL_PARSE_CACHE_PREFIX + billTask.getBillUploadId();
            Object cachedData = redisUtils.get(cacheKey);
            if (cachedData != null) {
                try {
                    String parseResultJson = cachedData instanceof String ? (String) cachedData : gson.toJson(cachedData);
                    BillParseResult parseResult = gson.fromJson(parseResultJson, BillParseResult.class);
                    response.setParseResult(parseResult);
                } catch (Exception e) {
                    log.error("解析缓存数据失败，taskId：{}", taskId, e);
                }
            }
        }
        
        return ResultVo.success(response);
    }
    
    /**
     * 查询任务列表
     */
    @GetMapping("/task/list")
    @Operation(summary = "查询任务列表", description = "查询用户的所有账单处理任务")
    public ResultVo getTaskList(HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }
        
        List<BillTask> taskList = billTaskService.getTaskListByUser(userId);
        return ResultVo.success(taskList);
    }
}