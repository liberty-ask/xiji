package com.xiji.entity.dto.response;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 对比统计响应DTO
 */
@Data
public class StatisticsCompareResponse {
    /**
     * 对比类型
     */
    private String type;
    
    /**
     * 第一个时期数据
     */
    private PeriodData period1;
    
    /**
     * 第二个时期数据
     */
    private PeriodData period2;
    
    /**
     * 变化信息
     */
    private Changes changes;
    
    @Data
    public static class PeriodData {
        /**
         * 年份
         */
        private Integer year;
        
        /**
         * 月份
         */
        private Integer month;
        
        /**
         * 标签
         */
        private String label;
        
        /**
         * 总收入
         */
        private BigDecimal totalIncome;
        
        /**
         * 总支出
         */
        private BigDecimal totalExpense;
        
        /**
         * 净收入
         */
        private BigDecimal netIncome;
        
        /**
         * 交易笔数
         */
        private Integer transactionCount;
    }
    
    @Data
    public static class Changes {
        /**
         * 收入变化率（%）
         */
        private BigDecimal incomeChange;
        
        /**
         * 支出变化率（%）
         */
        private BigDecimal expenseChange;
        
        /**
         * 净收入变化率（%）
         */
        private BigDecimal netChange;
        
        /**
         * 交易笔数变化率（%）
         */
        private BigDecimal countChange;
    }
}

