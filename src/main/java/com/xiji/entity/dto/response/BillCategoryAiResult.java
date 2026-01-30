package com.xiji.entity.dto.response;

import lombok.Data;

/**
 * 账单AI归类结果DTO
 * @author liberty
 */
@Data
public class BillCategoryAiResult {
    
    /**
     * 交易索引（用于批量处理时对应原始交易）
     */
    private Integer index;
    
    /**
     * 归类后的分类名称
     */
    private String category;
    
    /**
     * 置信度（0-1）
     */
    private Double confidence;
}

