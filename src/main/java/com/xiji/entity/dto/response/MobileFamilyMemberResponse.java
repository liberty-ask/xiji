package com.xiji.entity.dto.response;

import lombok.Data;

/**
 * 手机端家庭成员响应DTO
 */
@Data
public class MobileFamilyMemberResponse {
    /**
     * 用户ID
     */
    private String id;
    
    /**
     * 姓名
     */
    private String name;
    
    /**
     * 角色：1-管理员, 0-普通成员
     */
    private Integer role;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 标签（可选）
     */
    private String label;
}

