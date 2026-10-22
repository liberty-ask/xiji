package com.xiji.entity.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 手机端交易记录响应DTO
 * @author liberty
 */
@Data
public class MobileTransactionResponse {

    private String id;
    
    /**
     * 交易类型：1(支出) 或 0(收入)
     */
    private Integer type;
    
    /**
     * 金额
     */
    private BigDecimal amount;
    
    /**
     * 分类名称
     */
    private String category;

    /**
     * 分类图标名（前端据此渲染分类图标）
     */
    private String icon;

    /**
     * 日期
     */
    private LocalDate date;
    
    /**
     * 备注
     */
    private String description;
    
    /**
     * 创建人ID
     */
    private String userId;

    /**
     * 交易对方
     */
    private String counterparty;
}
