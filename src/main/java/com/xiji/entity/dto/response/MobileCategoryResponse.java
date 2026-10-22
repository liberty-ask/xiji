package com.xiji.entity.dto.response;

import lombok.Data;

/**
 * 手机端分类响应DTO
 * @author liberty
 */
@Data
public class MobileCategoryResponse {
    /**
     * 分类ID
     */
    private String id;
    
    /**
     * 分类名称
     */
    private String name;
    
    /**
     * 分类图标
     */
    private String icon;
    
    /**
     * 分类类型：0-收入，1-支出
     */
    private Integer type;
}
