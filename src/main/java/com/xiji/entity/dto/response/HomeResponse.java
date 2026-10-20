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
     * 可用余额（数值，格式化由客户端负责）
     */
    private BigDecimal balance;

    /**
     * 总收入（数值，格式化由客户端负责）
     */
    private BigDecimal income;

    /**
     * 总支出（数值，格式化由客户端负责）
     */
    private BigDecimal expense;

    /**
     * 预算信息
     */
    private BudgetInfo budget;

    /**
     * 今日支出（数值，格式化由客户端负责）
     */
    private BigDecimal todayExpense;

    /**
     * 昨日支出（数值，格式化由客户端负责）
     */
    private BigDecimal yesterdayExpense;
    
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
         * 金额（数值，正负号由客户端根据isIncome拼接）
         */
        private BigDecimal amount;
        
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





