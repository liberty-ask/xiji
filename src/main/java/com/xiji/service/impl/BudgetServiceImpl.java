package com.xiji.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiji.entity.domain.Budget;
import com.xiji.mapper.BudgetMapper;
import com.xiji.service.BudgetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 预算服务实现类
 * @author liberty
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetServiceImpl extends ServiceImpl<BudgetMapper, Budget> implements BudgetService {
    
    @Override
    public Budget getOrCreateBudget(Long familyId) {
        Budget budget = getBudget(familyId);
        if (budget == null) {
            budget = new Budget();
            budget.setFamilyId(familyId);
            budget.setAmount(BigDecimal.ZERO);
            save(budget);
        }
        return budget;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setBudget(Long familyId, BigDecimal amount) {
        Budget budget = getBudget(familyId);
        if (budget == null) {
            budget = new Budget();
            budget.setFamilyId(familyId);
        }
        budget.setAmount(amount);
        return saveOrUpdate(budget);
    }
    
    @Override
    public Budget getBudget(Long familyId) {
        LambdaQueryWrapper<Budget> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Budget::getFamilyId, familyId);
        return getOne(queryWrapper);
    }
}




