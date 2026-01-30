package com.xiji.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 发送短信验证码请求DTO
 * @author liberty
 */
@Data
public class SendSmsCodeRequest {
    
    @NotBlank(message = "手机号不能为空")
    private String phone;
}
