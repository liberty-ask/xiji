package com.xiji.entity.dto.response;

import lombok.Data;
import java.util.List;

/**
 * 手机端统计数据响应DTO
 */
@Data
public class MobileStatisticsResponse {
    /**
     * 总金额（字符串，已格式化）
     */
    private String total;
    
    /**
     * 变化百分比（字符串，如：-12%）
     */
    private String change;
    
    /**
     * 分类统计列表
     */
    private List<CategoryStatisticsItem> items;
    
    @Data
    public static class CategoryStatisticsItem {
        /**
         * 分类名称
         */
        private String name;
        
        /**
         * 金额（字符串，已格式化，如：¥3,200）
         */
        private String amount;
        
        /**
         * 占比（字符串，如：45%）
         */
        private String pct;
        
        /**
         * 颜色（十六进制，如：#13ec5b）
         */
        private String color;
        
        /**
         * 图标名称
         */
        private String icon;
    }
}

