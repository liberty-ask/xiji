package com.xiji.entity.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 家庭成员关联实体类
 * @author liberty
 */
@TableName(value = "family_member")
@Data
@EqualsAndHashCode(callSuper = true)
public class FamilyMember extends BaseEntity {
    
    /**
     * 关联ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 家庭ID
     */
    private Long familyId;

    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 角色：1-管理员，0-普通成员
     */
    private Integer role;

    /**
     * 同步开关投影：1-开启云备份同步，0-休眠（机制状态明文，仅展示用途，
     * 账号级真值在客户端 vault.enabled，由客户端开关切换时上报）
     */
    private Integer syncEnabled;
}

