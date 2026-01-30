package com.xiji.entity.dto.response;

import lombok.Data;

/**
 * 手机端登录响应DTO
 */
@Data
public class MobileLoginResponse {
    /**
     * JWT Token
     */
    private String token;
    
    /**
     * 用户信息
     */
    private MobileUserResponse user;
}

