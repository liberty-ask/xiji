package com.xiji.entity.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 家庭申请实体类
 * @author liberty
 */
@TableName(value = "family_application")
@Data
@EqualsAndHashCode(callSuper = true)
public class FamilyApplication extends BaseEntity {
    
    /**
     * 申请ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 家庭ID
     */
    private Long familyId;

    /**
     * 申请人ID
     */
    private Long userId;
    
    /**
     * 状态：0-待审核，1-已批准，2-已拒绝
     */
    private Integer status;
    
    /**
     * 申请备注
     */
    private String note;
}

