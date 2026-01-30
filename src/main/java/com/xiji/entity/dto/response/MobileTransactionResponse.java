package com.xiji.entity.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 手机端交易记录响应DTO
 * @author liberty
 */
@Data
public class MobileTransactionResponse {
    
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    
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
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 交易对方
     */
    private String counterparty;
}
