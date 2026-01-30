package com.xiji.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiji.entity.domain.FamilyApplication;

import java.util.List;

/**
 * 家庭申请服务接口
 */
public interface FamilyApplicationService extends IService<FamilyApplication> {
    
    /**
     * 申请加入家庭
     * @param familyId 家庭ID
     * @param userId 用户ID
     * @param note 申请备注
     * @return 是否成功
     */
    boolean applyToJoinFamily(Long familyId, Long userId, String note);
    
    /**
     * 处理申请（批准或拒绝）
     * @param applicationId 申请ID
     * @param status 状态：1-批准，2-拒绝
     * @return 是否成功
     */
    boolean processApplication(Long applicationId, Integer status);
    
    /**
     * 获取待审核申请列表
     * @param familyId 家庭ID
     * @return 申请列表
     */
    List<FamilyApplication> getPendingApplications(Long familyId);
}

