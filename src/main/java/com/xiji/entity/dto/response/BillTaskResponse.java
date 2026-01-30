package com.xiji.entity.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 账单任务响应类
 * 用于上传账单和导入账单接口的返回值
 * @author liberty
 */
@Data
@Schema(description = "账单任务响应")
public class BillTaskResponse {

    /**
     * 任务ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "任务ID")
    private Long taskId;

    /**
     * 响应消息
     */
    @Schema(description = "响应消息")
    private String message;
}
