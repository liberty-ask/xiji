package com.xiji.entity.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 修改密码请求DTO
 * @author liberty
 */
@Data
public class UserPassword {
    
    @NotNull(message = "用户ID不能为空")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String username;

    @NotNull(message = "旧密码不能为空")
    private String password;

    private String email;

    private String name;

    private String avatar;

    /**
     * 角色: 1为管理员/0为家庭成员
     */
    private Integer role;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    
    /**
     * 确认密码
     */
    private String confirmPassword;
    
    /**
     * 新密码
     */
    @NotNull(message = "新密码不能为空")
    @Size(min = 6, max = 50, message = "新密码长度必须在6-50位之间")
    private String newPassword;
}
