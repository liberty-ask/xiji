package com.xiji.controller;

import com.xiji.common.response.ResultVo;
import com.xiji.config.CustomConfig;
import com.xiji.service.OssService;
import com.xiji.utils.FileUploadValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 * 支持多种文件类型上传，包括图片、文档、视频、音频等
 * 禁止上传可执行文件、脚本文件等危险文件类型
 */
@RestController
@RequestMapping("/api/v1/upload")
@Tag(name = "上传文件", description = "文件上传相关接口")
@Slf4j
@RequiredArgsConstructor
public class UploadController {
    
    private final CustomConfig customConfig;
    private final OssService ossService;

    /**
     * 上传文件
     * 支持的文件类型：
     * - 图片：jpg, jpeg, png, gif, bmp, webp, svg, ico
     * - 文档：pdf, doc, docx, xls, xlsx, ppt, pptx, txt, rtf, csv
     * - 视频：mp4, avi, mov, wmv, flv, mkv, webm
     * - 音频：mp3, wav, ogg, aac, flac, m4a
     * - 压缩文件：zip, rar, 7z, tar, gz
     * - 其他：json, xml, html, css, md
     * 
     * 禁止的文件类型：
     * - 可执行文件：exe, bat, cmd, com, scr, msi, dll等
     * - 脚本文件：sh, js, php, py, rb等
     * - Java可执行文件：jar, war, ear, class
     * 
     * 最大文件大小：50MB
     */
    @Operation(summary = "上传文件", description = "上传文件到OSS，支持多种文件类型，最大50MB")
    @PostMapping("/file")
    public ResultVo uploadFile(@RequestParam("file") MultipartFile file) {
        log.info("上传文件：{}", file.getOriginalFilename());
        
        // 使用文件验证工具类进行验证
        String validationError = FileUploadValidator.validateFile(file);
        if (validationError != null) {
            return ResultVo.error(validationError);
        }

        try {
            // 检查OSS配置
            CustomConfig.OssConfig ossConfig = customConfig.getOss();
            if (ossConfig == null || ossConfig.getEndpoint() == null ||
                ossConfig.getAccessKeyId() == null || ossConfig.getAccessKeySecret() == null ||
                ossConfig.getBucketName() == null) {
                return ResultVo.error("OSS配置不完整");
            }
            
            // 使用OSS上传
            String folder = ossConfig.getFolder();
            String objectKey = ossService.uploadFile(file, folder);
            
            // 构建文件访问URL返回给前端
            String fileUrl = ossService.buildFileUrl(objectKey);
            log.info("文件上传OSS成功，文件名：{}，ObjectKey：{}，URL：{}", 
                file.getOriginalFilename(), objectKey, fileUrl);
            
            return ResultVo.success("文件上传成功", fileUrl);
        } catch (Exception ex) {
            log.error("文件上传失败，文件名：{}", file.getOriginalFilename(), ex);
            return ResultVo.error("文件上传失败：" + ex.getMessage());
        }
    }
}
