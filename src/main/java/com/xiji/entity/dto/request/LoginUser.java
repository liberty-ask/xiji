package com.xiji.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 登录请求DTO
 * @author liberty
 */
@Data
public class LoginUser {
    
    private String username;
    
    /**
     * 手机号（用于手机号登录）
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确", groups = {SmsLogin.class})
    private String phone;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空", groups = {PasswordLogin.class})
    private String password;
    
    /**
     * 图片验证码
     */
    @NotBlank(message = "图片验证码不能为空", groups = {PasswordLogin.class})
    private String captcha;
    
    /**
     * 图片验证码token（从获取验证码接口返回）
     */
    @NotBlank(message = "图片验证码token不能为空", groups = {PasswordLogin.class})
    private String captchaToken;
    
    /**
     * 短信验证码（用于手机号登录）
     */
    @NotBlank(message = "短信验证码不能为空", groups = {SmsLogin.class})
    private String smsCode;
    
    /**
     * 登录方式：password-密码登录，sms-短信验证码登录
     */
    private String loginType;
    
    /**
     * 权限
     */
    private Integer role;
    
    /**
     * 验证分组：密码登录
     */
    public interface PasswordLogin {}
    
    /**
     * 验证分组：短信登录
     */
    public interface SmsLogin {}
}
