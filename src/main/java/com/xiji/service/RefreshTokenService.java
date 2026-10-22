package com.xiji.service;

import com.xiji.config.CustomConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Refresh Token 服务
 * 不透明随机串 + Redis 双 key 存储：
 *   family:refresh:{token}        -> userId   TTL=30d   单token有效性（存在即有效）
 *   family:refresh-user:{userId}  -> Set<token>         用户活跃refresh索引，TTL滚动30d
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final RedisTemplate<String, Object> redisTemplate;
    private final CustomConfig customConfig;

    /**
     * 为用户生成新的refresh token并入库
     * @param userId 用户ID
     * @return refresh token
     */
    public String generate(String userId) {
        String token = generateTokenString();
        // 写入顺序固定：先token key，后索引
        redisTemplate.opsForValue().set(tokenKey(token), userId, getRefreshExpire(), TimeUnit.SECONDS);
        redisTemplate.opsForSet().add(userKey(userId), token);
        redisTemplate.expire(userKey(userId), getRefreshExpire(), TimeUnit.SECONDS);
        return token;
    }

    /**
     * 校验refresh token
     * @param token refresh token
     * @return 有效返回对应userId，无效/过期/被撤销返回null
     */
    public String validate(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        Object userId = redisTemplate.opsForValue().get(tokenKey(token));
        if (userId == null) {
            return null;
        }
        return userId.toString();
    }

    /**
     * 轮换：删除旧token并签发新token
     * @param oldToken 旧refresh token
     * @param userId 用户ID
     * @return 新refresh token
     */
    public String rotate(String oldToken, String userId) {
        // 删除顺序固定：先token key，后索引
        redisTemplate.delete(tokenKey(oldToken));
        redisTemplate.opsForSet().remove(userKey(userId), oldToken);
        return generate(userId);
    }

    /**
     * 全端撤销：撤销该用户全部refresh token（登出/修改密码）
     * @param userId 用户ID
     */
    public void revokeAll(String userId) {
        String userKey = userKey(userId);
        Set<Object> tokens = redisTemplate.opsForSet().members(userKey);
        if (tokens != null && !tokens.isEmpty()) {
            List<String> tokenKeys = tokens.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(this::tokenKey)
                    .collect(Collectors.toList());
            if (!tokenKeys.isEmpty()) {
                redisTemplate.delete(tokenKeys);
            }
        }
        redisTemplate.delete(userKey);
    }

    private String generateTokenString() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String tokenKey(String token) {
        return keyPrefix() + "refresh:" + token;
    }

    private String userKey(String userId) {
        return keyPrefix() + "refresh-user:" + userId;
    }

    private String keyPrefix() {
        return customConfig.getRedis() != null && customConfig.getRedis().getKeyPrefix() != null
                ? customConfig.getRedis().getKeyPrefix()
                : "family:";
    }

    /**
     * refresh token有效期（秒），默认30天
     */
    private long getRefreshExpire() {
        long refreshExpireMs = customConfig.getJwt() != null && customConfig.getJwt().getRefreshExpire() != null
                ? customConfig.getJwt().getRefreshExpire()
                : 2592000000L;
        return Duration.ofMillis(refreshExpireMs).getSeconds();
    }
}
