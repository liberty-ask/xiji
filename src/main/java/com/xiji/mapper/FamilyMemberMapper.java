package com.xiji.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiji.entity.domain.FamilyMember;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 家庭成员Mapper接口
 */
@Mapper
public interface FamilyMemberMapper extends BaseMapper<FamilyMember> {

    /**
     * 物理删除成员关系行（family_member 为服务端权威关系表，不参与云同步，
     * 退出直接物理删除不留墓碑，(family_id, user_id, deleted) 三列唯一索引天然成立）
     */
    @Delete("DELETE FROM family_member WHERE family_id = #{familyId} AND user_id = #{userId}")
    int deleteByFamilyAndUser(@Param("familyId") Long familyId, @Param("userId") Long userId);
}

