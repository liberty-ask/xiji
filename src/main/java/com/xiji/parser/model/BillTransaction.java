package com.xiji.parser.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 账单交易记录模型
 * @author liberty
 */
@Data
public class BillTransaction {
    
    /**
     * 交易单号（用于去重）
     */
    private String tradeNo;
    
    /**
     * 交易日期
     */
    private LocalDate date;
    
    /**
     * 交易类型（0-收入，1-支出）
     */
    private Integer type;
    
    /**
     * 交易金额
     */
    private BigDecimal amount;
    
    /**
     * 分类名称（自动匹配或默认）
     */
    private String category;
    
    /**
     * 交易描述/备注
     */
    private String description;
    
    /**
     * 支付方式
     */
    private String payMethod;
    
    /**
     * 交易对方
     */
    private String counterparty;
    
    /**
     * 交易状态
     */
    private String status;
    
    /**
     * 原始数据（保留所有字段，用于调试和扩展）
     */
    private Map<String, String> rawData = new HashMap<>();
}


