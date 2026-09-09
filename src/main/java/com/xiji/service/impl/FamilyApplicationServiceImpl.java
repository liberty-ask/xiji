package com.xiji.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiji.entity.domain.FamilyApplication;
import com.xiji.entity.domain.User;
import com.xiji.mapper.FamilyApplicationMapper;
import com.xiji.service.FamilyApplicationService;
import com.xiji.service.FamilyMemberService;
import com.xiji.service.FamilyService;
import com.xiji.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 家庭申请服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyApplicationServiceImpl extends ServiceImpl<FamilyApplicationMapper, FamilyApplication> 
        implements FamilyApplicationService {
    
    private final UserService userService;
    private final FamilyMemberService familyMemberService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean applyToJoinFamily(Long familyId, Long userId, String note) {
        // 检查用户是否已经在家庭中
        User user = userService.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (familyMemberService.isMemberOfFamily(familyId, userId)) {
            throw new RuntimeException("您已经是该家庭的成员");
        }
        
        // 检查是否已有待审核的申请
        LambdaQueryWrapper<FamilyApplication> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FamilyApplication::getFamilyId, familyId)
                   .eq(FamilyApplication::getUserId, userId)
                   .eq(FamilyApplication::getStatus, 0);
        if (count(queryWrapper) > 0) {
            throw new RuntimeException("您已经提交过申请，请等待审核");
        }
        
        // 创建申请
        FamilyApplication application = new FamilyApplication();
        application.setFamilyId(familyId);
        application.setUserId(userId);
        application.setStatus(0); // 待审核
        application.setNote(note);
        application.setCreatedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());
        
        boolean success = save(application);
        if (success) {
            log.info("申请加入家庭成功，familyId={}，userId={}", familyId, userId);
        } else {
            log.error("申请加入家庭失败，familyId={}，userId={}", familyId, userId);
        }
        return success;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean processApplication(Long applicationId, Integer status) {
        FamilyApplication application = getById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        
        if (application.getStatus() != 0) {
            throw new RuntimeException("申请已被处理");
        }
        
        if (status != 1 && status != 2) {
            throw new RuntimeException("状态参数不正确");
        }
        
        application.setStatus(status);
        application.setUpdatedAt(LocalDateTime.now());
        
        boolean success = updateById(application);
        
        // 如果批准，将用户加入家庭
        if (success && status == 1) {
            familyMemberService.addMemberToFamily(application.getFamilyId(), application.getUserId(), 0);
            log.info("批准加入家庭，familyId={}，userId={}，applicationId={}", application.getFamilyId(), application.getUserId(), applicationId);
        } else if (success && status == 2) {
            log.info("拒绝加入家庭，familyId={}，userId={}，applicationId={}", application.getFamilyId(), application.getUserId(), applicationId);
        } else if (!success) {
            log.error("处理家庭申请失败，applicationId={}，status={}", applicationId, status);
        }
        
        return success;
    }
    
    @Override
    public List<FamilyApplication> getPendingApplications(Long familyId) {
        LambdaQueryWrapper<FamilyApplication> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FamilyApplication::getFamilyId, familyId)
                   .eq(FamilyApplication::getStatus, 0)
                   .orderByDesc(FamilyApplication::getCreatedAt);
        return list(queryWrapper);
    }
}

