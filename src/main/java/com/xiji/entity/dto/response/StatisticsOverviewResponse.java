package com.xiji.entity.dto.response;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 统计概览响应DTO
 */
@Data
public class StatisticsOverviewResponse {
    /**
     * 总收入
     */
    private BigDecimal totalIncome;
    
    /**
     * 总支出
     */
    private BigDecimal totalExpense;
    
    /**
     * 净收入（收入-支出）
     */
    private BigDecimal netIncome;
    
    /**
     * 收入变化率（%）
     */
    private BigDecimal incomeChange;
    
    /**
     * 支出变化率（%）
     */
    private BigDecimal expenseChange;
    
    /**
     * 日均收入
     */
    private BigDecimal avgDailyIncome;
    
    /**
     * 日均支出
     */
    private BigDecimal avgDailyExpense;
    
    /**
     * 交易笔数
     */
    private Integer transactionCount;
}

