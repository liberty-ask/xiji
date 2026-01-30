package com.xiji.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 登录尝试工具类
 * 用于记录和检查登录失败次数，防止暴力破解
 * @author liberty
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginAttemptUtils {

    private final RedisUtils redisUtils;
    private final com.xiji.config.CustomConfig customConfig;

    /**
     * 登录失败次数的最大阈值
     */
    private static final int MAX_ATTEMPTS = 5;

    /**
     * 锁定时间（秒）- 5分钟
     */
    private static final long LOCK_TIME = 300;

    /**
     * 获取登录尝试的Redis key
     */
    private String getLoginAttemptKey(String identifier) {
        String prefix = customConfig.getRedis() != null 
            ? customConfig.getRedis().getKeyPrefix() 
            : "family:";
        return prefix + "login:attempt:" + identifier;
    }

    /**
     * 记录登录失败
     * @param identifier 用户标识（用户名或手机号）
     */
    public void recordLoginFailure(String identifier) {
        String key = getLoginAttemptKey(identifier);
        Long attempts = redisUtils.increment(key, 1);
        
        // 如果是第一次失败，设置过期时间
        if (attempts != null && attempts == 1) {
            redisUtils.expire(key, LOCK_TIME);
        }
        
        log.warn("登录失败记录，用户：{}，失败次数：{}", identifier, attempts);
    }

    /**
     * 清除登录失败记录（登录成功时调用）
     * @param identifier 用户标识（用户名或手机号）
     */
    public void clearLoginFailure(String identifier) {
        String key = getLoginAttemptKey(identifier);
        redisUtils.delete(key);
        log.debug("清除登录失败记录，用户：{}", identifier);
    }

    /**
     * 检查是否超过登录失败次数限制
     * @param identifier 用户标识（用户名或手机号）
     * @return true-超过限制，false-未超过限制
     */
    public boolean isLoginBlocked(String identifier) {
        String key = getLoginAttemptKey(identifier);
        Integer attempts = redisUtils.get(key, Integer.class);
        
        if (attempts == null) {
            return false;
        }
        
        boolean blocked = attempts >= MAX_ATTEMPTS;
        if (blocked) {
            Long remainingTime = redisUtils.getExpire(key);
            log.warn("登录被锁定，用户：{}，失败次数：{}，剩余锁定时间：{}秒", 
                identifier, attempts, remainingTime);
        }
        
        return blocked;
    }

    /**
     * 获取剩余锁定时间（秒）
     * @param identifier 用户标识（用户名或手机号）
     * @return 剩余锁定时间（秒），-1表示未锁定
     */
    public Long getRemainingLockTime(String identifier) {
        String key = getLoginAttemptKey(identifier);
        Long expire = redisUtils.getExpire(key);
        return expire != null && expire > 0 ? expire : -1;
    }
}




