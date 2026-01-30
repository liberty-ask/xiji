package com.xiji.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 语音记账请求DTO
 * @author liberty
 */
@Data
public class VoiceTransactionRequest {
    
    /**
     * 语音转文字后的文本内容
     */
    @NotBlank(message = "文本内容不能为空")
    private String text;
}

