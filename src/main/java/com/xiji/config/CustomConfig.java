package com.xiji.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 自定义配置类, 用于读取application.yaml配置文件中的自定义配置项
 * @author liberty
 * @since 2024-12-23
 */
@Data
@Component
@ConfigurationProperties(prefix = "custom")
public class CustomConfig {
    
    // JWT配置
    private JwtConfig jwt = new JwtConfig();
    
    // Redis配置
    private RedisConfig redis = new RedisConfig();
    
    // 短信配置
    private SmsConfig sms = new SmsConfig();
    
    // OSS配置
    private OssConfig oss = new OssConfig();
    
    // 智谱AI配置
    private ZhipuAiConfig zhipuAi = new ZhipuAiConfig();
    
    // CORS配置
    private CorsConfig cors = new CorsConfig();
    
    @Data
    public static class JwtConfig {
        private String signKey;
        private Long expire;
    }
    
    @Data
    public static class RedisConfig {
        // 验证码过期时间（秒）
        private Long captchaExpire = 300L;
        // 缓存前缀
        private String keyPrefix = "family:";
    }
    
    @Data
    public static class SmsConfig {
        // 是否启用短信验证码（true-发送短信，false-返回验证码给前端）
        private Boolean enable = true;
        // 阿里云AccessKey ID
        private String accessKeyId;
        // 阿里云AccessKey Secret
        private String accessKeySecret;
        // 短信签名
        private String signName;
        // 短信模板代码
        private String templateCode;
        // 验证码过期时间（秒）
        private Long codeExpire = 300L;
        // 发送间隔（秒）
        private Long sendInterval = 60L;
    }
    
    @Data
    public static class OssConfig {
        // OSS Endpoint（如：https://oss-cn-hangzhou.aliyuncs.com 或 oss-cn-hangzhou.aliyuncs.com）
        private String endpoint;
        // 阿里云AccessKey ID
        private String accessKeyId;
        // 阿里云AccessKey Secret
        private String accessKeySecret;
        // Bucket名称
        private String bucketName;
        // 自定义域名（可选，如果配置了自定义域名，文件URL将使用此域名）
        private String customDomain;
        // 文件上传目录，用于区分环境（如：prod/uploads/ 或 test/uploads/）
        private String folder;
    }
    
    @Data
    public static class ZhipuAiConfig {
        // 智谱AI API Key
        private String apiKey;
        // 模型名称（默认使用glm-4-flash）
        private String model = "glm-4-flash";
    }
    
    @Data
    public static class CorsConfig {
        // 允许的源，多个源用逗号分隔
        private String allowedOrigins = "http://localhost:3000";
        // 是否允许凭证
        private Boolean allowCredentials = true;
        // 允许的HTTP方法
        private String allowedMethods = "*";
        // 允许的请求头
        private String allowedHeaders = "*";
        // 暴露的响应头
        private String exposedHeaders = "*";
    }
    
}
