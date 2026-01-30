package com.xiji.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiji.entity.domain.Family;
import com.xiji.mapper.FamilyMapper;
import com.xiji.service.FamilyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 家庭服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyServiceImpl extends ServiceImpl<FamilyMapper, Family> implements FamilyService {
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Family createFamily(String name, Long ownerId) {
        Family family = new Family();
        family.setName(name);
        family.setOwnerId(ownerId);
        family.setCreatedAt(LocalDateTime.now());
        family.setUpdatedAt(LocalDateTime.now());
        
        if (save(family)) {
            log.info("创建家庭成功，家庭ID={}，创建者ID={}", family.getId(), ownerId);
            return family;
        } else {
            log.error("创建家庭失败，创建者ID={}", ownerId);
            throw new RuntimeException("创建家庭失败");
        }
    }
}

