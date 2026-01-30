package com.xiji.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新分类请求DTO
 * @author liberty
 */
@Data
public class UpdateCategoryRequest {
    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(min = 1, max = 50, message = "分类名称长度必须在1-50位之间")
    private String name;
    
    /**
     * 分类图标
     */
    private String icon;
    
    /**
     * 排序序号（可选）
     */
    private Integer sortOrder;
    
    /**
     * 是否启用：1-启用，0-禁用
     */
    private Integer status;
}




