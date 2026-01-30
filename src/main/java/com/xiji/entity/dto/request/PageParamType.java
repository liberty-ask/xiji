package com.xiji.entity.dto.request;

import lombok.Data;

/**
 * 收支分页查询请求参数
 */
@Data
public class PageParamType {
    private Integer currentPage;
    private Integer pageSize;
    // 搜索值
    private String value;
    // 类型:0 收入 1 支出
    private Integer type;
}

