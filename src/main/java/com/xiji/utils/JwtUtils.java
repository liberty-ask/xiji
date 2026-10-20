package com.xiji.utils;

import com.xiji.config.CustomConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
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

    private static final long DEFAULT_ACCESS_EXPIRE = 7200000L;
    private static final long DEFAULT_WEB_EXPIRE = 43200000L;

    private static CustomConfig customConfig;

    // 签名密钥与解析器只初始化一次复用（JJWT parser 线程安全）
    private static volatile SecretKey signKey;
    private static volatile JwtParser jwtParser;

    @Autowired
    public void setCustomConfig(CustomConfig customConfig) {
        JwtUtils.customConfig = customConfig;
        initKeyAndParser();
    }

    /**
     * 初始化签名密钥与解析器（配置注入后调用一次）
     */
    private static synchronized void initKeyAndParser() {
        String key = customConfig != null && customConfig.getJwt() != null
                ? customConfig.getJwt().getSignKey()
                : null;
        if (key == null || key.isEmpty()) {
            throw new IllegalStateException("custom.jwt.sign-key未配置，无法初始化JWT签名密钥");
        }
        // 确保密钥长度至少256位（32字节）
        if (key.length() < 32) {
            key = key + "0".repeat(32 - key.length());
        }
        SecretKey secretKey = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
        signKey = secretKey;
        jwtParser = Jwts.parser().verifyWith(secretKey).build();
    }

    private static SecretKey getSignKey() {
        if (signKey == null) {
            initKeyAndParser();
        }
        return signKey;
    }

    private static JwtParser getJwtParser() {
        if (jwtParser == null) {
            initKeyAndParser();
        }
        return jwtParser;
    }

    /**
     * 获取web端token过期时间（毫秒）
     */
    private static long getExpire() {
        return customConfig != null && customConfig.getJwt() != null && customConfig.getJwt().getExpire() != null
                ? customConfig.getJwt().getExpire()
                : DEFAULT_WEB_EXPIRE;
    }

    /**
     * 获取access token过期时间（毫秒）
     */
    public static long getAccessExpire() {
        return customConfig != null && customConfig.getJwt() != null && customConfig.getJwt().getAccessExpire() != null
                ? customConfig.getJwt().getAccessExpire()
                : DEFAULT_ACCESS_EXPIRE;
    }

    /**
     * 生成JWT令牌（使用web端过期时间，仅供web端登录）
     * @param claims JWT第二部分负载 payload 中存储的内容
     * @return JWT令牌
     */
    public static String generateJwt(Map<String, Object> claims){
        return generateJwt(claims, getExpire());
    }

    /**
     * 生成JWT令牌（指定过期时长）
     * @param claims JWT第二部分负载 payload 中存储的内容
     * @param expireMs 过期时长（毫秒）
     * @return JWT令牌
     */
    public static String generateJwt(Map<String, Object> claims, long expireMs) {
        return Jwts.builder()
                .claims(claims)
                .signWith(getSignKey())
                .expiration(new Date(System.currentTimeMillis() + expireMs))
                .compact();
    }

    /**
     * 解析JWT令牌
     * @param jwt JWT令牌
     * @return JWT第二部分负载 payload 中存储的内容，过期或无效返回null
     */
    public static Claims parseJwt(String jwt){
        try {
            return getJwtParser().parseSignedClaims(jwt).getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("JWT已过期");
            return null;
        } catch (Exception e) {
            log.warn("JWT解析失败，reason={}", e.getClass().getSimpleName());
            return null;
        }
    }
}
