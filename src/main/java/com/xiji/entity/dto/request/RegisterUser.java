package com.xiji.entity.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 注册用户请求DTO
 * @author liberty
 */
@Data
public class RegisterUser implements Serializable {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "用户名格式不正确，只能包含字母、数字、下划线，长度3-20位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6-50位之间")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    /**
     * 短信验证码
     */
    @NotBlank(message = "短信验证码不能为空")
    private String smsCode;

    private String name;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 角色: 1为管理员/0为家庭成员
     */
    private Integer role;

    private static final long serialVersionUID = 1L;
}
