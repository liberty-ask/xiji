package com.xiji.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiji.entity.domain.FamilyMember;
import com.xiji.mapper.FamilyMemberMapper;
import com.xiji.service.FamilyMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 家庭成员服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyMemberServiceImpl extends ServiceImpl<FamilyMemberMapper, FamilyMember> 
        implements FamilyMemberService {
    
    @Override
    public List<Long> getUserFamilyIds(Long userId) {
        LambdaQueryWrapper<FamilyMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FamilyMember::getUserId, userId);
        List<FamilyMember> members = list(queryWrapper);
        return members.stream()
            .map(FamilyMember::getFamilyId)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addMemberToFamily(Long familyId, Long userId, Integer role) {
        // 检查是否已经存在
        LambdaQueryWrapper<FamilyMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FamilyMember::getFamilyId, familyId)
                   .eq(FamilyMember::getUserId, userId);
        if (count(queryWrapper) > 0) {
            log.warn("用户已经是该家庭的成员，家庭ID={}，用户ID={}", familyId, userId);
            return false;
        }
        
        FamilyMember member = new FamilyMember();
        member.setFamilyId(familyId);
        member.setUserId(userId);
        member.setRole(role != null ? role : 0);
        member.setCreatedAt(LocalDateTime.now());
        
        boolean success = save(member);
        if (success) {
            log.info("用户加入家庭成功，家庭ID={}，用户ID={}，角色={}", familyId, userId, role);
        }
        return success;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeMemberFromFamily(Long familyId, Long userId) {
        LambdaQueryWrapper<FamilyMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FamilyMember::getFamilyId, familyId)
                   .eq(FamilyMember::getUserId, userId);
        
        boolean success = remove(queryWrapper);
        if (success) {
            log.info("用户退出家庭成功，家庭ID={}，用户ID={}", familyId, userId);
        }
        return success;
    }
    
    @Override
    public boolean isMemberOfFamily(Long familyId, Long userId) {
        LambdaQueryWrapper<FamilyMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FamilyMember::getFamilyId, familyId)
                   .eq(FamilyMember::getUserId, userId);
        return count(queryWrapper) > 0;
    }
    
    @Override
    public Integer getMemberRole(Long familyId, Long userId) {
        LambdaQueryWrapper<FamilyMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FamilyMember::getFamilyId, familyId)
                   .eq(FamilyMember::getUserId, userId);
        FamilyMember member = getOne(queryWrapper);
        return member != null ? member.getRole() : null;
    }
}

