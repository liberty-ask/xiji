package com.xiji.entity.dto.response;

import lombok.Data;

/**
 * 手机端用户资料响应DTO
 * @author liberty
 */
@Data
public class MobileProfileResponse {
    /**
     * 用户ID
     */
    private String id;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 角色：1-管理员, 0-普通成员
     */
    private Integer role;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 家庭ID
     */
    private String familyId;
    
    /**
     * 待审核申请数量（仅管理员有此字段）
     */
    private Integer auditCount;
    //邮箱
    private String email;
}

