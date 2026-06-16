package com.xiji.entity.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 首页数据响应DTO
 * @author liberty
 */
@Data
public class HomeResponse {
    /**
     * 可用余额（字符串，已格式化）
     */
    private String balance;
    
    /**
     * 总收入（字符串，已格式化）
     */
    private String income;
    
    /**
     * 总支出（字符串，已格式化）
     */
    private String expense;
    
    /**
     * 预算信息
     */
    private BudgetInfo budget;
    
    /**
     * 今日支出（字符串，已格式化）
     */
    private String todayExpense;
    
    /**
     * 昨日支出（字符串，已格式化）
     */
    private String yesterdayExpense;
    
    /**
     * 最近活动列表
     */
    private List<ActivityItem> activities;
    
    /**
     * 预算信息内部类
     */
    @Data
    public static class BudgetInfo {
        /**
         * 已使用预算（数字，单位：元）
         */
        private BigDecimal used;
        
        /**
         * 总预算（数字，单位：元）
         */
        private BigDecimal total;
    }
    
    /**
     * 活动项内部类
     */
    @Data
    public static class ActivityItem {
        /**
         * 标题（分类名称或描述）
         */
        private String title;
        
        /**
         * 用户名称
         */
        private String user;
        
        /**
         * 时间（HH:mm格式）
         */
        private String time;

        /**
         * 日期（yyyy-MM-dd格式）
         */
        private String date;

        /**
         * 金额（字符串，带+/-号）
         */
        private String amount;
        
        /**
         * 图标
         */
        private String icon;

        /**
         * 描述
         */
        private String description;
        
        /**
         * 是否为收入
         */
        private Boolean isIncome;

        /**
         * 交易对方
         */
        private String counterparty;
    }
}





