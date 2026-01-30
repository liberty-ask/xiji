package com.xiji.entity.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 收支类别实体类
 * @author liberty
 */
@TableName(value = "category")
@Data
@EqualsAndHashCode(callSuper = true)
public class Category extends BaseEntity {
    
    /**
     * 类别ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    
    /**
     * 类别名称
     */
    private String name;
    
    /**
     * 类别图标（可选）
     */
    private String icon;
    
    /**
     * 排序序号（数字越小越靠前）
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

