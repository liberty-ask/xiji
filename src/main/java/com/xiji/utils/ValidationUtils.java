package com.xiji.utils;

import java.util.regex.Pattern;

/**
 * 验证工具类
 */
public class ValidationUtils {
    
    // 邮箱正则表达式
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    // 用户名正则表达式（字母、数字、下划线，3-20位）
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    
    /**
     * 验证邮箱格式
     * @param email 邮箱
     * @return 是否有效
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * 验证用户名格式
     * @param username 用户名
     * @return 是否有效
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username).matches();
    }
    
    /**
     * 验证密码强度
     * @param password 密码
     * @return 是否满足强度要求（至少6位）
     */
    public static boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        // 至少6位，建议包含字母和数字
        return password.length() >= 6 && password.length() <= 50;
    }
    
    /**
     * 验证密码强度（强密码：至少8位，包含大小写字母、数字）
     * @param password 密码
     * @return 是否满足强密码要求
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        return hasUpper && hasLower && hasDigit;
    }
    
    /**
     * 验证手机号格式（中国大陆）
     * @param phone 手机号
     * @return 是否有效
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        // 11位数字，以1开头，第二位为3-9
        return phone.matches("^1[3-9]\\d{9}$");
    }
}

