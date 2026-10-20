package com.xiji.entity.dto.response;

import lombok.Data;

/**
 * 手机端登录响应DTO
 */
@Data
public class MobileLoginResponse {
    /**
     * access token（短有效期）
     */
    private String accessToken;

    /**
     * refresh token（长有效期，用于换取新token）
     */
    private String refreshToken;

    /**
     * 用户信息
     */
    private MobileUserResponse user;
}

