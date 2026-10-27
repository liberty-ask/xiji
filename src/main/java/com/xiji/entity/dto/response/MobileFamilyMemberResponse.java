package com.xiji.entity.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 手机端家庭成员响应DTO
 */
@Data
public class MobileFamilyMemberResponse {
    /**
     * 用户ID
     * 雪花ID超JS 2^53精度，序列化为字符串
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    
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

    /**
     * 同步开关投影：1-开启云备份同步，0-休眠（未开启成员的家庭账单
     * 不会上云，休眠标记供其他成员感知数据可见范围）
     */
    private Integer syncEnabled;
}

