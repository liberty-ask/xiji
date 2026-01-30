package com.xiji.entity.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建分类请求DTO
 * @author liberty
 */
@Data
public class CreateCategoryRequest {
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
     * 类别类型：0-收入，1-支出
     */
    @NotNull(message = "分类类型不能为空")
    private Integer type;
    
    /**
     * 家庭ID（可选，如果为null则创建系统默认分类，有值则创建家庭自定义分类）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long familyId;
    
    /**
     * 排序序号（可选）
     */
    private Integer sortOrder;
    
    /**
     * 是否启用：1-启用，0-禁用（可选，默认启用）
     */
    private Integer status;
}

