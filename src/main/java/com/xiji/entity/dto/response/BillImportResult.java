package com.xiji.entity.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 账单导入结果DTO
 * @author liberty
 */
@Data
public class BillImportResult {
    
    /**
     * 总记录数
     */
    private Integer totalCount;
    
    /**
     * 成功导入数
     */
    private Integer successCount;
    
    /**
     * 跳过数（重复等）
     */
    private Integer skipCount;
    
    /**
     * 失败数
     */
    private Integer failCount;
    
    /**
     * 导入错误列表
     */
    private List<BillImportError> errors;
    
    /**
     * 成功导入的交易记录ID列表
     */
    private List<Long> transactionIds;
    
    /**
     * 导入错误信息
     */
    @Data
    public static class BillImportError {
        /**
         * 交易单号
         */
        private String tradeNo;
        
        /**
         * 错误原因
         */
        private String reason;
        
        /**
         * 原始数据
         */
        private String rawData;
    }
}


