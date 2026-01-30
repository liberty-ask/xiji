package com.xiji.entity.dto.response;

import lombok.Data;

/**
 * 分页响应数据
 */
@Data
public class PageData {
    private int current;
    private int pages;
    private int size;
    private int total;
    private Object records;
}

