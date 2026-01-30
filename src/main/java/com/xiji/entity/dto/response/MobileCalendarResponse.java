package com.xiji.entity.dto.response;

import lombok.Data;
import java.util.List;

/**
 * 手机端日历响应DTO
 * @author liberty
 */
@Data
public class MobileCalendarResponse {
    /**
     * 月份（格式：YYYY-MM）
     */
    private String month;
    
    /**
     * 月收入（字符串，已格式化）
     */
    private String monthlyIncome;
    
    /**
     * 月支出（字符串，已格式化）
     */
    private String monthlyExpense;
    
    /**
     * 每日详情列表
     */
    private List<DayDetail> dailyDetails;
    
    @Data
    public static class DayDetail {
        /**
         * 日期（格式：YYYY-MM-DD）
         */
        private String date;
        
        /**
         * 当日收入（字符串，已格式化）
         */
        private String income;
        
        /**
         * 当日支出（字符串，已格式化）
         */
        private String expense;
        
        /**
         * 交易详情列表
         */
        private List<TransactionDetail> transactions;
    }
    
    @Data
    public static class TransactionDetail {
        /**
         * 交易ID
         */
        private String id;
        
        /**
         * 分类ID
         */
        private String category;
        
        /**
         * 备注
         */
        private String note;
        
        /**
         * 金额（字符串，已格式化，如：+ 128.00 或 - 128.00）
         */
        private String amount;
    }
}
