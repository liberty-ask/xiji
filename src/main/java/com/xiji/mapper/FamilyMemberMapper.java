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
     * 物理删除历史墓碑行（逻辑删除前清理，避免 (family_id, user_id, deleted) 三列唯一索引冲突）
     */
    @Delete("DELETE FROM family_member WHERE family_id = #{familyId} AND user_id = #{userId} AND deleted = 1")
    int deleteTombstones(@Param("familyId") Long familyId, @Param("userId") Long userId);
}

