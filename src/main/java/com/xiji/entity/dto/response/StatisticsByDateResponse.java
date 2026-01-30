package com.xiji.entity.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 日期统计响应DTO（日历视图）
 */
@Data
public class StatisticsByDateResponse {
    /**
     * 年份
     */
    private Integer year;
    
    /**
     * 月份
     */
    private Integer month;
    
    /**
     * 日期数据列表
     */
    private List<DateStatisticsItem> days;
    
    /**
     * 汇总信息
     */
    private DateSummary summary;
    
    @Data
    public static class DateStatisticsItem {
        /**
         * 日期（YYYY-MM-DD）
         */
        private String date;
        
        /**
         * 日期（日）
         */
        private Integer day;
        
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
         * 交易笔数
         */
        private Integer count;
        
        /**
         * 热力值（1-5）
         */
        private Integer intensity;
    }
    
    @Data
    public static class DateSummary {
        /**
         * 单日最大金额
         */
        private BigDecimal maxAmount;
        
        /**
         * 最大金额日期
         */
        private String maxDate;
    }
}

