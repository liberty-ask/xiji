package com.xiji.entity.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 预算实体类
 * @author liberty
 */
@TableName(value = "budget")
@Data
@EqualsAndHashCode(callSuper = true)
public class Budget extends BaseEntity {
    
    /**
     * 预算ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    
    /**
     * 家庭ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long familyId;
    
    /**
     * 年份
     */
    private Integer year;
    
    /**
     * 月份（1-12，如果为null则表示年度预算）
     */
    private Integer month;
    
    /**
     * 预算金额
     */
    private BigDecimal amount;
    
    /**
     * 预算类型：0-收入预算，1-支出预算
     */
    private Integer type;
}




