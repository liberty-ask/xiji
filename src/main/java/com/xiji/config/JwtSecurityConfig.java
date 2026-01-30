package com.xiji.config;

import com.xiji.config.CustomConfig.JwtConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * JWT安全配置检查
 * 应用启动时检查JWT密钥配置
 * @author liberty
 */
@Slf4j
@Component
public class JwtSecurityConfig implements CommandLineRunner {

    @Autowired
    private CustomConfig customConfig;

    @Autowired
    private Environment environment;

    @Override
    public void run(String... args) {
        JwtConfig jwtConfig = customConfig.getJwt();
        String signKey = jwtConfig != null ? jwtConfig.getSignKey() : null;
        String activeProfile = environment.getActiveProfiles().length > 0 
            ? environment.getActiveProfiles()[0] 
            : "default";

        // 检查是否为生产环境
        boolean isProduction = "active".equals(activeProfile) || "prod".equals(activeProfile);

        // 检查JWT密钥
        if (signKey == null || signKey.trim().isEmpty()) {
            log.error("========== JWT安全警告 ==========");
            log.error("JWT签名密钥未配置！");
            log.error("请立即在配置文件中设置 custom.jwt.sign-key");
            log.error("密钥长度建议至少32个字符（256位）");
            log.error("==================================");
            if (isProduction) {
                throw new IllegalStateException("生产环境JWT密钥未配置，应用无法启动");
            }
        } else if (signKey.equals("your-secret-key-change-in-production-min-length-256-bits")) {
            log.warn("========== JWT安全警告 ==========");
            log.warn("JWT签名密钥仍使用默认值！");
            log.warn("生产环境必须修改为强密钥");
            log.warn("密钥长度建议至少32个字符（256位）");
            log.warn("==================================");
            if (isProduction) {
                throw new IllegalStateException("生产环境不能使用默认JWT密钥，请修改配置");
            }
        } else if (signKey.length() < 32) {
            log.warn("========== JWT安全警告 ==========");
            log.warn("JWT签名密钥长度不足！");
            log.warn("当前长度：{}，建议长度：至少32个字符（256位）", signKey.length());
            log.warn("==================================");
            if (isProduction) {
                log.warn("生产环境建议使用更长的密钥");
            }
        } else {
            log.info("JWT密钥配置检查通过，密钥长度：{}", signKey.length());
        }
    }
}




