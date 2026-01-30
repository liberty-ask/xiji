package com.xiji.entity.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 处理家庭申请请求DTO
 * @author liberty
 */
@Data
public class ProcessApplicationRequest {
    
    @NotNull(message = "申请ID不能为空")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id; // Application ID
    
    @NotBlank(message = "操作类型不能为空")
    @Pattern(regexp = "^(approve|reject)$", message = "操作类型不正确，应为approve或reject")
    private String action; // "approve" or "reject"
}
