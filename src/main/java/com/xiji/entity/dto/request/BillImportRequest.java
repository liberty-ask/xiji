package com.xiji.entity.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 账单导入请求DTO
 * @author liberty
 */
@Data
public class BillImportRequest {
    
    /**
     * 账单上传记录ID（从上传解析接口返回的metadata中获取）
     */
    @NotNull(message = "账单上传记录ID不能为空")
    private Long billUploadId;
    
    /**
     * 文件ID（已废弃，使用billUploadId）
     * @deprecated 使用billUploadId替代
     */
    @Deprecated
    private String fileId;
    
    /**
     * 是否跳过重复记录（默认true）
     */
    private Boolean skipDuplicates = true;
    
    /**
     * 是否自动匹配分类（默认true）
     */
    private Boolean autoMatchCategory = true;
}

