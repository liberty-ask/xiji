package com.xiji.utils;

import com.xiji.config.CustomConfig;
import com.xiji.service.OssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 头像URL处理工具类
 * 所有文件都存储到OSS，数据库中存储OSS的objectKey（相对路径）
 */
@Slf4j
@Component
public class AvatarUtils {
    
    private static CustomConfig customConfig;
    private static OssService ossService;
    
    @Autowired
    public void setCustomConfig(CustomConfig customConfig) {
        AvatarUtils.customConfig = customConfig;
    }
    
    @Autowired
    public void setOssService(OssService ossService) {
        AvatarUtils.ossService = ossService;
    }
    
    /**
     * 处理头像URL用于存储到数据库
     * - 如果传入的是完整OSS URL，提取objectKey（相对路径）
     * - 如果传入的已经是objectKey（相对路径），直接返回
     * 
     * @param avatarUrl 头像URL（可能是完整URL或objectKey）
     * @return OSS objectKey（相对路径），用于存储到数据库
     */
    public static String processAvatarForStorage(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            return null;
        }
        
        String trimmedUrl = avatarUrl.trim();
        
        // 如果已经是相对路径（不包含协议），直接返回（视为objectKey）
        if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            return trimmedUrl;
        }
        
        // 处理完整URL，提取objectKey
        CustomConfig.OssConfig ossConfig = customConfig != null ? customConfig.getOss() : null;
        if (ossConfig == null) {
            log.warn("OSS配置不存在，无法处理头像URL：{}", avatarUrl);
            return trimmedUrl;
        }
        
        String bucketName = ossConfig.getBucketName();
        String endpoint = ossConfig.getEndpoint();
        String customDomain = ossConfig.getCustomDomain();
        
        // 提取endpoint的host部分（去除协议）
        String endpointHost = endpoint != null ? endpoint.trim() : "";
        if (endpointHost.startsWith("http://")) {
            endpointHost = endpointHost.substring(7);
        } else if (endpointHost.startsWith("https://")) {
            endpointHost = endpointHost.substring(8);
        }
        if (endpointHost.endsWith("/")) {
            endpointHost = endpointHost.substring(0, endpointHost.length() - 1);
        }
        
        // 检查是否匹配OSS默认域名格式：https://bucket-name.endpoint/objectKey
        String objectKey = null;
        String defaultDomainPattern = bucketName + "." + endpointHost;
        if (trimmedUrl.contains(defaultDomainPattern + "/")) {
            int keyStartIndex = trimmedUrl.indexOf(defaultDomainPattern + "/") + defaultDomainPattern.length() + 1;
            objectKey = trimmedUrl.substring(keyStartIndex);
        }
        
        // 检查是否匹配自定义域名
        if (objectKey == null && customDomain != null && !customDomain.trim().isEmpty()) {
            String domain = customDomain.trim();
            if (!domain.startsWith("http://") && !domain.startsWith("https://")) {
                domain = "https://" + domain;
            }
            if (domain.endsWith("/")) {
                domain = domain.substring(0, domain.length() - 1);
            }
            if (trimmedUrl.startsWith(domain + "/")) {
                objectKey = trimmedUrl.substring(domain.length() + 1);
            }
        }
        
        // 移除查询参数（如果有）
        if (objectKey != null) {
            int queryIndex = objectKey.indexOf('?');
            if (queryIndex >= 0) {
                objectKey = objectKey.substring(0, queryIndex);
            }
            return objectKey;
        }
        
        // 如果无法提取objectKey，返回原始值（兼容性处理）
        log.warn("无法从URL中提取OSS objectKey，返回原始值：{}", avatarUrl);
        return trimmedUrl;
    }
    
    /**
     * 处理头像URL用于返回给前端
     * - 将数据库中的objectKey（相对路径）转换为完整OSS URL
     * - 如果已经是完整URL，直接返回（兼容旧数据）
     * 
     * @param avatarUrl 数据库中存储的头像URL（objectKey或完整URL）
     * @return 完整头像URL
     */
    public static String processAvatarForResponse(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            return null;
        }
        
        String trimmedUrl = avatarUrl.trim();
        
        // 如果已经是完整URL，直接返回（兼容旧数据）
        if (trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")) {
            return trimmedUrl;
        }
        
        // 将objectKey转换为完整OSS URL
        if (ossService != null) {
            try {
                return ossService.buildFileUrl(trimmedUrl);
            } catch (Exception e) {
                log.error("构建OSS URL失败，objectKey：{}", trimmedUrl, e);
                return trimmedUrl;
            }
        }
        
        // 如果OssService未初始化，返回原始值
        log.warn("OssService未初始化，无法构建OSS URL，返回原始值：{}", avatarUrl);
        return trimmedUrl;
    }
}
