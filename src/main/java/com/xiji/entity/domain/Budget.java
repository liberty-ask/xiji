package com.xiji.entity.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
    @TableId(type = IdType.INPUT)
    private String id;

    /**
     * 家庭ID
     */
    private String familyId;
    
    /**
     * 预算金额
     */
    private BigDecimal amount;
}




