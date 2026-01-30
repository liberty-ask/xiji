package com.xiji.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码工具类
 * 使用BCrypt进行密码加密和验证
 */
@Component
public class PasswordUtils {
    
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    /**
     * 加密密码
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public static String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
    
    /**
     * 验证密码
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码（BCrypt格式）
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        // 只使用BCrypt验证
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    /**
     * 判断密码是否为BCrypt格式
     * @param password 密码
     * @return 是否为BCrypt格式
     */
    public static boolean isBCryptFormat(String password) {
        return password != null && password.startsWith("$2a$") && password.length() == 60;
    }
}

