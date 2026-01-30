package com.xiji.utils;

import com.xiji.config.CustomConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtUtils {
    
    private static CustomConfig customConfig;
    
    @Autowired
    public void setCustomConfig(CustomConfig customConfig) {
        JwtUtils.customConfig = customConfig;
    }
    
    /**
     * 获取签名密钥
     * @return SecretKey
     */
    private static SecretKey getSignKey() {
        String signKey = customConfig != null && customConfig.getJwt() != null 
            ? customConfig.getJwt().getSignKey() 
            : "your-secret-key-change-in-production-min-length-256-bits";
        // 确保密钥长度至少256位（32字节）
        if (signKey.length() < 32) {
            int paddingLength = 32 - signKey.length();
            StringBuilder padding = new StringBuilder();
            for (int i = 0; i < paddingLength; i++) {
                padding.append("0");
            }
            signKey = signKey + padding.toString();
        }
        return Keys.hmacShaKeyFor(signKey.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 获取过期时间
     * @return 过期时间（毫秒）
     */
    private static Long getExpire() {
        return customConfig != null && customConfig.getJwt() != null 
            ? customConfig.getJwt().getExpire() 
            : 43200000L; // 默认12小时
    }

    /**
     * 生成JWT令牌
     * @param claims JWT第二部分负载 payload 中存储的内容
     * @return JWT令牌
     */
    public static String generateJwt(Map<String, Object> claims){
        String jwt = Jwts.builder()
                .claims(claims)
                .signWith(getSignKey())
                .expiration(new Date(System.currentTimeMillis() + getExpire()))
                .compact();
        return jwt;
    }

    /**
     * 解析JWT令牌
     * @param jwt JWT令牌
     * @return JWT第二部分负载 payload 中存储的内容
     */
    public static Claims parseJwt(String jwt){
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSignKey())
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
            return claims;
        } catch (ExpiredJwtException e) {
            log.error("JWT已过期: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("JWT解析失败: {}", e.getMessage());
            return null;
        }
    }
}
