package com.xiji.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiji.entity.domain.Family;

/**
 * 家庭服务接口
 * @author liberty
 */
public interface FamilyService extends IService<Family> {
    
    /**
     * 创建家庭
     * @param name 家庭名称
     * @param ownerId 创建者ID
     * @return 创建的家庭
     */
    Family createFamily(String name, Long ownerId);
}

