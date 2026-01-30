package com.xiji.entity.dto.request;

import lombok.Data;

/**
 * 分页查询请求参数
 */
@Data
public class PageParam {
    private Integer currentPage;
    private Integer pageSize;
    // 搜索值
    private String value;
}

