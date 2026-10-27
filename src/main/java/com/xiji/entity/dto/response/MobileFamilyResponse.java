package com.xiji.entity.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 手机端家庭信息响应DTO
 */
@Data
public class MobileFamilyResponse {
    /**
     * 家庭ID
     * 雪花ID超JS 2^53精度，序列化为字符串
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    
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

