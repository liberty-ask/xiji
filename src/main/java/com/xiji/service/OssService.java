package com.xiji.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.xiji.config.CustomConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * 阿里云OSS服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssService {

    private final CustomConfig customConfig;

    /**
     * 创建OSS客户端
     */
    private OSS createOssClient() {
        CustomConfig.OssConfig ossConfig = customConfig.getOss();
        if (ossConfig == null ||
            ossConfig.getEndpoint() == null ||
            ossConfig.getAccessKeyId() == null ||
            ossConfig.getAccessKeySecret() == null) {
            throw new RuntimeException("阿里云OSS配置不完整");
        }

        return new OSSClientBuilder().build(
            ossConfig.getEndpoint(),
            ossConfig.getAccessKeyId(),
            ossConfig.getAccessKeySecret()
        );
    }

    /**
     * 上传文件到OSS
     * @param file 上传的文件
     * @param folder 文件夹路径（可为空，如：images/）
     * @return OSS对象键（objectKey），用于存储到数据库
     */
    public String uploadFile(MultipartFile file, String folder) {
        CustomConfig.OssConfig ossConfig = customConfig.getOss();
        if (ossConfig == null || ossConfig.getBucketName() == null) {
            throw new RuntimeException("OSS配置不完整，缺少Bucket名称");
        }

        OSS ossClient = null;
        try {
            ossClient = createOssClient();
            String bucketName = ossConfig.getBucketName();

            // 生成文件名：UUID + 原始文件名
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || originalFileName.trim().isEmpty()) {
                throw new IllegalArgumentException("文件名不能为空");
            }

            // 获取文件扩展名
            String extension = "";
            int lastDotIndex = originalFileName.lastIndexOf('.');
            if (lastDotIndex > 0) {
                extension = originalFileName.substring(lastDotIndex);
            }

            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString().replaceAll("-", "") + extension;

            // 构建OSS对象键（完整路径）
            String objectKey;
            if (folder != null && !folder.trim().isEmpty()) {
                // 确保folder以/结尾
                String normalizedFolder = folder.trim();
                if (!normalizedFolder.endsWith("/")) {
                    normalizedFolder += "/";
                }
                objectKey = normalizedFolder + fileName;
            } else {
                objectKey = fileName;
            }

            // 上传文件
            InputStream inputStream = file.getInputStream();
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectKey, inputStream);
            ossClient.putObject(putObjectRequest);

            log.info("文件上传OSS成功，Bucket：{}，ObjectKey：{}", bucketName, objectKey);

            // 返回objectKey用于存储到数据库
            return objectKey;
        } catch (Exception e) {
            log.error("上传文件到OSS失败", e);
            throw new RuntimeException("文件上传失败：" + e.getMessage(), e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 上传文件到OSS（默认上传到根目录）
     * @param file 上传的文件
     * @return OSS对象键（objectKey），用于存储到数据库
     */
    public String uploadFile(MultipartFile file) {
        return uploadFile(file, null);
    }

    /**
     * 根据objectKey构建文件访问URL
     * @param objectKey OSS对象键
     * @return 文件访问URL
     */
    public String buildFileUrl(String objectKey) {
        CustomConfig.OssConfig ossConfig = customConfig.getOss();
        if (ossConfig == null || ossConfig.getBucketName() == null) {
            throw new RuntimeException("OSS配置不完整，缺少Bucket名称");
        }
        return buildFileUrl(ossConfig, objectKey);
    }

    /**
     * 构建文件访问URL
     * @param ossConfig OSS配置
     * @param objectKey OSS对象键
     * @return 文件访问URL
     */
    private String buildFileUrl(CustomConfig.OssConfig ossConfig, String objectKey) {
        String bucketName = ossConfig.getBucketName();
        String customDomain = ossConfig.getCustomDomain();

        // 如果配置了自定义域名，使用自定义域名
        if (customDomain != null && !customDomain.trim().isEmpty()) {
            String domain = customDomain.trim();
            if (!domain.startsWith("http://") && !domain.startsWith("https://")) {
                domain = "https://" + domain;
            }
            if (!domain.endsWith("/")) {
                domain += "/";
            }
            return domain + objectKey;
        }

        // 否则使用默认的OSS域名
        // endpoint格式：https://oss-cn-hangzhou.aliyuncs.com 或 oss-cn-hangzhou.aliyuncs.com
        String endpoint = ossConfig.getEndpoint().trim();
        String host;
        String protocol = "https://";
        
        // 处理endpoint，提取host部分
        if (endpoint.startsWith("http://")) {
            protocol = "http://";
            host = endpoint.substring(7);
        } else if (endpoint.startsWith("https://")) {
            host = endpoint.substring(8);
        } else {
            host = endpoint;
        }
        
        // 移除末尾的斜杠
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }

        // OSS URL格式：https://bucket-name.oss-cn-hangzhou.aliyuncs.com/object-key
        return protocol + bucketName + "." + host + "/" + objectKey;
    }

    /**
     * 删除OSS文件
     * @param objectKey OSS对象键（完整路径）
     */
    public void deleteFile(String objectKey) {
        CustomConfig.OssConfig ossConfig = customConfig.getOss();
        if (ossConfig == null || ossConfig.getBucketName() == null) {
            throw new RuntimeException("OSS配置不完整，缺少Bucket名称");
        }

        OSS ossClient = null;
        try {
            ossClient = createOssClient();
            String bucketName = ossConfig.getBucketName();
            ossClient.deleteObject(bucketName, objectKey);
            log.info("删除OSS文件成功，Bucket：{}，ObjectKey：{}", bucketName, objectKey);
        } catch (Exception e) {
            log.error("删除OSS文件失败，ObjectKey：{}", objectKey, e);
            throw new RuntimeException("删除文件失败：" + e.getMessage(), e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 从OSS下载文件
     * @param objectKey OSS对象键（完整路径）
     * @return 文件输入流（注意：调用方需要在使用完后关闭OSS客户端）
     */
    public com.aliyun.oss.model.OSSObject downloadFile(String objectKey) {
        CustomConfig.OssConfig ossConfig = customConfig.getOss();
        if (ossConfig == null || ossConfig.getBucketName() == null) {
            throw new RuntimeException("OSS配置不完整，缺少Bucket名称");
        }

        OSS ossClient = createOssClient();
        String bucketName = ossConfig.getBucketName();
        try {
            return ossClient.getObject(bucketName, objectKey);
        } catch (Exception e) {
            ossClient.shutdown();
            log.error("从OSS下载文件失败，ObjectKey：{}", objectKey, e);
            throw new RuntimeException("下载文件失败：" + e.getMessage(), e);
        }
    }
}
