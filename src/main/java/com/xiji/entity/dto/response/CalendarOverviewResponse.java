package com.xiji.entity.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 日历概览响应DTO
 * @author liberty
 */
@Data
public class CalendarOverviewResponse {
    /**
     * 月收入（字符串，已格式化）
     */
    private BigDecimal monthlyIncome;
    
    /**
     * 月支出（字符串，已格式化）
     */
    private BigDecimal monthlyExpense;
    
    /**
     * 结余（字符串，已格式化）
     */
    private BigDecimal surplus;
    
    /**
     * 每日收支汇总（key为日期，value为当天的收入和支出）
     */
    private Map<String, DailySummary> dailySummary;
    
    /**
     * 每日详情（key为日期，value为当天的交易详情列表）
     */
    private Map<String, List<DailyDetail>> dailyDetails;
    
    /**
     * 每日收支汇总内部类
     */
    @Data
    public static class DailySummary {
        /**
         * 当天收入
         */
        private BigDecimal income;
        
        /**
         * 当天支出
         */
        private BigDecimal expense;
    }
    
    /**
     * 每日详情内部类
     */
    @Data
    public static class DailyDetail {
        /**
         * 交易描述或分类名称
         */
        private String name;
        
        /**
         * 分类名称
         */
        private String cat;
        
        /**
         * 时间（HH:mm格式）
         */
        private String time;
        
        /**
         * 金额（字符串，已格式化，如：- 128.00 或 + 128.00）
         */
        private String amount;
        /**
         * 交易类型（0收入，1支出）
         */
        private Integer type;
        
        /**
         * 图标名称
         */
        private String icon;

        /**
         * 交易描述
         */
        private String description;

        /**
         * 交易对方
         */
        private String counterparty;
    }
}




