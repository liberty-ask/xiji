package com.xiji.service;

import com.xiji.config.CustomConfig;
import com.xiji.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 验证码服务类
 * 使用Redis存储验证码
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final RedisUtils redisUtils;
    private final CustomConfig customConfig;

    /**
     * 生成并存储验证码
     * @param code 验证码
     * @return 验证码token
     */
    public String generateCaptchaToken(String code) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = getCaptchaKey(token);
        Long expire = customConfig.getRedis() != null 
            ? customConfig.getRedis().getCaptchaExpire() 
            : 300L;
        redisUtils.set(key, code, expire);
        log.debug("验证码已存入Redis，token={}", token);
        return token;
    }

    /**
     * 验证验证码
     * @param token 验证码token
     * @param code 用户输入的验证码
     * @return 是否验证通过
     */
    public boolean verifyCaptcha(String token, String code) {
        if (token == null || code == null) {
            return false;
        }
        String key = getCaptchaKey(token);
        String storedCode = redisUtils.get(key, String.class);
        if (storedCode == null) {
            log.warn("验证码已过期或不存在，token={}", token);
            return false;
        }
        boolean matches = storedCode.equalsIgnoreCase(code);
        if (matches) {
            // 验证成功后删除验证码（一次性使用）
            redisUtils.delete(key);
            log.debug("验证码验证成功，token={}", token);
        } else {
            log.warn("验证码错误，token={}, 输入={}, 正确={}", token, code, storedCode);
        }
        return matches;
    }

    /**
     * 删除验证码
     * @param token 验证码token
     */
    public void deleteCaptcha(String token) {
        if (token != null) {
            String key = getCaptchaKey(token);
            redisUtils.delete(key);
        }
    }

    /**
     * 获取验证码Redis key
     * @param token 验证码token
     * @return Redis key
     */
    private String getCaptchaKey(String token) {
        String prefix = customConfig.getRedis() != null 
            ? customConfig.getRedis().getKeyPrefix() 
            : "family:";
        return prefix + "captcha:" + token;
    }
}

