package com.xiji.entity.dto.response;

import lombok.Data;
import java.util.List;

/**
 * 手机端交易列表响应DTO
 */
@Data
public class MobileTransactionListResponse {
    /**
     * 交易列表
     */
    private List<MobileTransactionResponse> list;
    
    /**
     * 总记录数
     */
    private Integer total;
    
    /**
     * 当前页码
     */
    private Integer page;
    
    /**
     * 每页数量
     */
    private Integer pageSize;
}

