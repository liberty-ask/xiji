package com.xiji.entity.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 分类排名响应DTO
 * @author liberty
 */
@Data
public class CategoryRankResponse {
    /**
     * 分类收入金额（key为分类ID，value为金额）
     */
    private Map<Long, BigDecimal> categoryIncome;
    
    /**
     * 分类支出金额（key为分类ID，value为金额）
     */
    private Map<Long, BigDecimal> categoryExpense;
}
