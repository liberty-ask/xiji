package com.xiji.entity.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 切换家庭请求DTO
 * @author liberty
 */
@Data
public class SwitchFamilyRequest {

    @NotNull(message = "家庭ID不能为空")
    private String familyId;
}
