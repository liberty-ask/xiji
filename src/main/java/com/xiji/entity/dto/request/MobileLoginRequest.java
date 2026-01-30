package com.xiji.entity.dto.request;

import lombok.Data;

/**
 * 手机端登录请求DTO
 */
@Data
public class MobileLoginRequest {
    /**
     * 登录方式：password-密码登录，code-验证码登录
     */
    private String mode;
    
    /**
     * 账号（手机号或用户名，用于密码登录）
     */
    private String account;
    
    /**
     * 密码（用于密码登录）
     */
    private String password;
    
    /**
     * 手机号（用于验证码登录）
     */
    private String phone;
    
    /**
     * 验证码（用于验证码登录）
     */
    private String code;
}

