package com.xiji.entity.dto.response;

import lombok.Data;

/**
 * 手机端家庭信息响应DTO
 */
@Data
public class MobileFamilyResponse {
    /**
     * 家庭ID
     */
    private String id;
    
    /**
     * 家庭名称
     */
    private String name;
    
    /**
     * 是否为当前选择的家庭
     */
    private Boolean isCurrent;
    
    /**
     * 用户在家庭中的角色：1-管理员, 0-普通成员
     */
    private Integer role;
}

