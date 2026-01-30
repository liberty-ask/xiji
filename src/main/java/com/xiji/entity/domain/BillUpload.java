package com.xiji.entity.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 账单上传记录表实体类
 * @author liberty
 */
@TableName(value = "bill_upload")
@Data
@EqualsAndHashCode(callSuper = true)
public class BillUpload extends BaseEntity {
    
    @TableId(value = "id", type = IdType.ASSIGN_ID)
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
     * 文件ID（OSS objectKey）
     */
    private String fileId;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件URL
     */
    private String fileUrl;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 平台名称（支付宝、微信、招商银行等）
     */
    private String platform;
    
    /**
     * 状态：0-已上传，1-已解析，2-已导入，3-导入失败
     */
    private Integer status;
    
    /**
     * 总记录数
     */
    private Integer totalCount;
    
    /**
     * 成功数
     */
    private Integer successCount;
    
    /**
     * 错误数
     */
    private Integer errorCount;
    
    /**
     * 解析结果（JSON字符串，存储解析的元数据）
     */
    private String parseResult;
}


