package com.xiji.entity.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 申请加入家庭请求DTO
 * @author liberty
 */
@Data
public class ApplyToJoinFamilyRequest {

    @NotNull(message = "家庭ID不能为空")
    private Long familyId;
    
    @Size(max = 200, message = "备注长度不能超过200个字符")
    private String note;
}
