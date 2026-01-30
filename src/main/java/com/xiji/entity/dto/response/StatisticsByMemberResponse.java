package com.xiji.entity.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 成员统计响应DTO
 */
@Data
public class StatisticsByMemberResponse {
    /**
     * 总金额（字符串格式）
     */
    private String total;
    
    /**
     * 成员统计项列表
     */
    private List<MemberStatisticsItem> items;
    
    @Data
    public static class MemberStatisticsItem {
        /**
         * 成员ID
         */
        private String memberId;
        
        /**
         * 成员名称
         */
        private String memberName;
        
        /**
         * 头像
         */
        private String avatar;
        
        /**
         * 金额
         */
        private BigDecimal amount;
        
        /**
         * 金额字符串（格式化）
         */
        private String amountStr;
        
        /**
         * 占比
         */
        private BigDecimal percentage;
        
        /**
         * 占比字符串
         */
        private String percentageStr;
        
        /**
         * 交易笔数
         */
        private Integer count;
        
        /**
         * 平均金额
         */
        private BigDecimal avgAmount;
    }
}

