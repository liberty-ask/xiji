package com.xiji.entity.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 手机端用户信息响应DTO
 * @author liberty
 */
@Data
public class MobileUserResponse {
    /**
     * 用户ID
     * 雪花ID超JS 2^53精度，序列化为字符串
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 角色：1-管理员, 0-普通成员
     */
    private Integer role;

    /**
     * 邮箱
     */
    private String email;
}

