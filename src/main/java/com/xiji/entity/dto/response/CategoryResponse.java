package com.xiji.entity.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 分类响应DTO（用于API接口返回）
 * @author liberty
 */
@Data
public class CategoryResponse {
    /**
     * 分类ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    
    /**
     * 分类名称
     */
    private String name;
    
    /**
     * 分类图标
     */
    private String icon;
    
    /**
     * 排序序号
     */
    private Integer sortOrder;
    
    /**
     * 是否启用：1-启用，0-禁用
     */
    private Integer status;
    
    /**
     * 家庭ID（null表示系统默认分类，有值表示该家庭的自定义分类）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long familyId;
    
    /**
     * 类别类型：0-收入，1-支出
     */
    private Integer type;
}




