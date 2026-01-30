package com.xiji.entity.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 切换家庭请求DTO
 * @author liberty
 */
@Data
public class SwitchFamilyRequest {
    
    @NotNull(message = "家庭ID不能为空")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long familyId;
}
