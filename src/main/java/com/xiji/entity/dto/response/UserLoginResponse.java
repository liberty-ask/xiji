package com.xiji.entity.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户登录响应
 */
@Data
public class UserLoginResponse implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String username;
    private String email;
    private String name;
    private String avatar;
    /**
     * 角色: 1为管理员/0为家庭成员
     */
    private Integer role;
    private Integer status;
    /**
     * JWT token
     */
    private String token;
    private Date createdAt;
    private Date updatedAt;

    private static final long serialVersionUID = 1L;
}

