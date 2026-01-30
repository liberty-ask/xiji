package com.xiji.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 添加分类请求DTO
 * @author liberty
 */
@Data
public class AddCategoryRequest {
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
     * 分类类型：0-收入，1-支出
     */
    @NotNull(message = "分类类型不能为空")
    private Integer type;
    
    /**
     * 排序序号（可选）
     */
    private Integer sortOrder;
}




