package com.xiji.entity.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 手机端预算响应DTO
 * @author liberty
 */
@Data
public class MobileBudgetResponse {
    /**
     * 预算总额
     */
    private BigDecimal budget;
    
    /**
     * 已使用金额
     */
    private BigDecimal used;
    
    /**
     * 剩余金额
     */
    private BigDecimal remaining;
    
    /**
     * 使用百分比（0-100）
     */
    private Double percentage;
}
