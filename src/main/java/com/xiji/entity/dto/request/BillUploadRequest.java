package com.xiji.entity.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 账单上传请求DTO
 * @author liberty
 */
@Data
public class BillUploadRequest {
    
    /**
     * 文件ID（上传后返回）
     */
    @NotNull(message = "文件ID不能为空")
    private String fileId;
    
    /**
     * 平台类型（可选，如果指定则使用指定解析器，否则自动识别）
     * 可选值：alipay, wechat, cmb
     */
    private String platform;
}


