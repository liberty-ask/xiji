package com.xiji.entity.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 手机端待审核申请响应DTO
 */
@Data
public class MobilePendingApplicationResponse {
    /**
     * 申请ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    
    /**
     * 申请人姓名
     */
    private String name;
    
    /**
     * 申请时间（格式：HH:mm 或 昨天 HH:mm）
     */
    private String time;
    
    /**
     * 申请备注（可选）
     */
    private String note;
    
    /**
     * 是否为新申请
     */
    private Boolean isNew;
    
    /**
     * 头像URL
     */
    private String avatar;
}

