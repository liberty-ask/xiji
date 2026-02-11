package com.xiji.entity.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 收支表实体类
 * @author liberty
 */
@TableName(value ="transactions")
@Data
@EqualsAndHashCode(callSuper = true)
public class Transactions extends BaseEntity {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    
    /**
     * 家庭ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long familyId;
    // 0：收入，1：支出
    private Integer type;
    // 金额
    private BigDecimal amount;
    // 类别ID（关联category表）
    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;
    // 支付方式
    private String payMethod;
    private LocalDate date;
    private String description;
    
    /**
     * 交易单号（用于去重，账单导入时使用）
     */
    private String tradeNo;

    /**
     * 商家订单号/商户单号
     */
    private String merchantOrderNo;
    
    /**
     * 平台来源（支付宝、微信、京东、招商银行等，账单导入时使用）
     */
    private String platform;

    /**
     * 交易对方
     */
    private String counterparty;
}
