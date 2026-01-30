package com.xiji.entity.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体类
 * @author liberty
 * @TableName user
 */
@TableName(value ="user")
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户实体")
public class User extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String username;

    private String password;

    private String email;
    
    /**
     * 手机号
     */
    private String phone;

    private String name;

    private String avatar;

    private Integer status;

    /**
     * 当前选择的家庭ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long currentFamilyId;
}

