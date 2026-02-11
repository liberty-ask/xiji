package com.xiji.entity.dto.response;

import com.xiji.parser.model.BillTransaction;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 账单解析结果DTO
 * @author liberty
 */
@Data
public class BillParseResult {
    
    /**
     * 平台名称（支付宝、微信、招商银行等）
     */
    private String platform;
    
    /**
     * 总记录数
     */
    private Integer totalCount;
    
    /**
     * 成功解析数
     */
    private Integer successCount;
    
    /**
     * 错误记录数
     */
    private Integer errorCount;
    
    /**
     * 解析出的交易记录列表（预览，最多返回前100条）
     */
    private List<BillTransactionPreview> preview;
    
    /**
     * 错误列表
     */
    private List<BillParseError> errors;
    
    /**
     * 元数据（账单日期范围、总金额等）
     */
    private Map<String, Object> metadata;
    
    /**
     * 完整的交易记录列表（用于缓存和导入，不包含在预览中）
     * 注意：此字段在返回给前端时通常不包含，仅用于内部缓存
     */
    private List<BillTransaction> transactions;
    
    /**
     * 账单交易预览
     */
    @Data
    public static class BillTransactionPreview {
        /**
         * 交易单号（用于去重）
         */
        private String tradeNo;
        
        /**
         * 交易日期
         */
        private LocalDate date;
        
        /**
         * 交易类型（0-收入，1-支出）
         */
        private Integer type;
        
        /**
         * 交易金额
         */
        private BigDecimal amount;
        
        /**
         * 分类名称（自动匹配或默认）
         */
        private String category;
        
        /**
         * 交易描述/备注
         */
        private String description;
        
        /**
         * 支付方式
         */
        private String payMethod;
        
        /**
         * 交易对方
         */
        private String counterparty;
        
        /**
         * 交易状态
         */
        private String status;
    }
    
    /**
     * 解析错误信息
     */
    @Data
    public static class BillParseError {
        /**
         * 错误行号
         */
        private Integer row;
        
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

