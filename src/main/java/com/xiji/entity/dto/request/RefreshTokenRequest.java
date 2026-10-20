package com.xiji.entity.dto.request;

import lombok.Data;

/**
 * 刷新token请求DTO
 */
@Data
public class RefreshTokenRequest {
    /**
     * refresh token
     */
    private String refreshToken;
}
