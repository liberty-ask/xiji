package com.xiji.entity.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 家庭实体类
 * @author liberty
 */
@TableName(value = "family")
@Data
@EqualsAndHashCode(callSuper = true)
public class Family extends BaseEntity {
    
    /**
     * 家庭ID
     */
    @TableId(type = IdType.INPUT)
    private String id;
    
    /**
     * 家庭名称
     */
    private String name;
    
    /**
     * 创建者ID（家庭管理员）
     */
    private String ownerId;
}

