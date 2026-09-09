package com.xiji.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiji.entity.domain.Budget;

import java.math.BigDecimal;

/**
 * 预算服务接口
 * @author liberty
 */
public interface BudgetService extends IService<Budget> {
    
    /**
     * 设置预算
     * @param familyId 家庭ID
     * @param amount 预算金额
     * @return 是否成功
     */
    boolean setBudget(Long familyId, BigDecimal amount);
    
    /**
     * 获取预算
     * @param familyId 家庭ID
     * @return 预算对象，如果不存在返回null
     */
    Budget getBudget(Long familyId);
}




