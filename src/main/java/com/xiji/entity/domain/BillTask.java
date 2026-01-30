package com.xiji.entity.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 账单任务实体类
 * 用于记录账单上传、解析和导入的任务状态
 * @author liberty
 */
@TableName(value = "bill_task")
@Data
@EqualsAndHashCode(callSuper = true)
public class BillTask extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 家庭ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long familyId;

    /**
     * 账单上传ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long billUploadId;

    /**
     * 原始文件名
     */
    private String originalFileName;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * OSS文件路径
     */
    private String ossFilePath;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 任务类型：1-上传解析，2-导入
     */
    private Integer taskType;

    /**
     * 任务状态：0-待处理，1-处理中，2-成功，3-失败
     */
    private Integer status;

    /**
     * 处理进度（0-100）
     */
    private Integer progress;

    /**
     * 总记录数
     */
    private Integer totalCount;

    /**
     * 成功记录数
     */
    private Integer successCount;

    /**
     * 失败记录数
     */
    private Integer failCount;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 开始处理时间
     */
    private LocalDateTime startTime;

    /**
     * 结束处理时间
     */
    private LocalDateTime endTime;

    /**
     * 平台类型
     */
    private String platform;
}