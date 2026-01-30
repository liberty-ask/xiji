package com.xiji.entity.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 时间趋势统计响应DTO
 */
@Data
public class StatisticsTrendResponse {
    /**
     * 统计粒度
     */
    private String period;
    
    /**
     * 趋势数据项列表
     */
    private List<TrendItem> items;
    
    /**
     * 汇总信息
     */
    private Summary summary;
    
    @Data
    public static class TrendItem {
        /**
         * 日期/时间标识
         */
        private String date;
        
        /**
         * 显示标签
         */
        private String dateLabel;
        
        /**
         * 收入
         */
        private BigDecimal income;
        
        /**
         * 支出
         */
        private BigDecimal expense;
        
        /**
         * 净收入
         */
        private BigDecimal net;
        
        /**
         * 收入变化率（%）
         */
        private BigDecimal incomeChange;
        
        /**
         * 支出变化率（%）
         */
        private BigDecimal expenseChange;
    }
    
    @Data
    public static class Summary {
        /**
         * 平均收入
         */
        private BigDecimal avgIncome;
        
        /**
         * 平均支出
         */
        private BigDecimal avgExpense;
        
        /**
         * 总收入
         */
        private BigDecimal totalIncome;
        
        /**
         * 总支出
         */
        private BigDecimal totalExpense;
        
        /**
         * 最大收入
         */
        private BigDecimal maxIncome;
        
        /**
         * 最大支出
         */
        private BigDecimal maxExpense;
    }
}

