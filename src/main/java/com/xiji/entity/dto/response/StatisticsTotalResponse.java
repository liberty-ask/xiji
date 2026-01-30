package com.xiji.entity.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 统计总额响应DTO
 * @author liberty
 */
@Data
public class StatisticsTotalResponse {
    /**
     * 总收入
     */
    private BigDecimal totalIncome;
    
    /**
     * 总支出
     */
    private BigDecimal totalExpense;
    
    /**
     * 收入条数
     */
    private Integer incomeCount;
    
    /**
     * 支出条数
     */
    private Integer expenseCount;
}
