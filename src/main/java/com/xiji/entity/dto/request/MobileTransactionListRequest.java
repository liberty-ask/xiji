package com.xiji.entity.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 手机端交易列表查询请求DTO
 * @author liberty
 */
@Data
public class MobileTransactionListRequest {
    /**
     * 当前页码
     */
    private Integer page;
    
    /**
     * 每页数量
     */
    private Integer pageSize;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 交易类型：1-支出，0-收入
     */
    private Integer type;
    
    /**
     * 开始日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    /**
     * 结束日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 关键词
     */
    private String keyword;
}




