package com.xiji.entity.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志实体类
 *
 * @author liberty
 * since 2024-12-10
 */
@TableName(value ="operation_logs")
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "操作日志实体")
public class OperationLogs extends BaseEntity {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String description;
    private String url;
    private String method;
    private String ip;
    private String params;
    private String exception;
}

