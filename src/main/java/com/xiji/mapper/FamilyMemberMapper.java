package com.xiji.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiji.entity.domain.FamilyMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * 家庭成员Mapper接口
 */
@Mapper
public interface FamilyMemberMapper extends BaseMapper<FamilyMember> {
}

