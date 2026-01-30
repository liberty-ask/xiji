package com.xiji.entity.dto.response;

import lombok.Data;
import java.util.List;

/**
 * 分类统计响应DTO
 */
@Data
public class StatisticsByCategoryResponse {
    /**
     * 总金额（字符串格式）
     */
    private String total;
    
    /**
     * 分类统计项列表
     */
    private List<CategoryStatisticsItem> items;
    
    @Data
    public static class CategoryStatisticsItem {
        /**
         * 分类ID
         */
        private String categoryId;
        
        /**
         * 分类名称
         */
        private String categoryName;
        
        /**
         * 图标
         */
        private String icon;
        
        /**
         * 颜色
         */
        private String color;
        
        /**
         * 金额
         */
        private java.math.BigDecimal amount;
        
        /**
         * 金额字符串（格式化）
         */
        private String amountStr;
        
        /**
         * 占比
         */
        private java.math.BigDecimal percentage;
        
        /**
         * 占比字符串
         */
        private String percentageStr;
        
        /**
         * 交易笔数
         */
        private Integer count;
    }
}

