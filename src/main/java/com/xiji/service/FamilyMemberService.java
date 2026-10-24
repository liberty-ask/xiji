package com.xiji.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiji.entity.domain.FamilyMember;

import java.util.List;

/**
 * 家庭成员服务接口
 */
public interface FamilyMemberService extends IService<FamilyMember> {
    
    /**
     * 获取用户的所有家庭ID列表
     * @param userId 用户ID
     * @return 家庭ID列表
     */
    List<String> getUserFamilyIds(String userId);
    
    /**
     * 将用户加入家庭
     * @param familyId 家庭ID
     * @param userId 用户ID
     * @param role 角色：1-管理员，0-普通成员
     * @return 是否成功
     */
    boolean addMemberToFamily(String familyId, String userId, Integer role);
    
    /**
     * 从家庭中移除用户
     * @param familyId 家庭ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean removeMemberFromFamily(String familyId, String userId);
    
    /**
     * 检查用户是否属于某个家庭
     * @param familyId 家庭ID
     * @param userId 用户ID
     * @return 是否属于
     */
    boolean isMemberOfFamily(String familyId, String userId);
    
    /**
     * 获取用户在家庭中的角色
     * @param familyId 家庭ID
     * @param userId 用户ID
     * @return 角色：1-管理员，0-普通成员，null-不属于该家庭
     */
    Integer getMemberRole(String familyId, String userId);

    /**
     * 更新用户在全部家庭的同步开关投影（客户端云备份开关切换时上报，
     * 开关是账号级语义，故不按家庭区分）
     * @param userId 用户ID
     * @param syncEnabled 1-开启同步，0-休眠
     * @return 是否成功
     */
    boolean updateSyncEnabledForAllFamilies(String userId, Integer syncEnabled);
}

