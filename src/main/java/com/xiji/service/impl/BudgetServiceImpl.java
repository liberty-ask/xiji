package com.xiji.service.impl;

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
    public Budget getOrCreateBudget(Long familyId, Integer year, Integer month, Integer type) {
        Budget budget = getBudget(familyId, year, month, type);
        if (budget == null) {
            budget = new Budget();
            budget.setFamilyId(familyId);
            budget.setYear(year);
            budget.setMonth(month);
            budget.setType(type);
            budget.setAmount(BigDecimal.ZERO);
            save(budget);
        }
        return budget;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setBudget(Long familyId, Integer year, Integer month, BigDecimal amount, Integer type) {
        Budget budget = getBudget(familyId, year, month, type);
        if (budget == null) {
            budget = new Budget();
            budget.setFamilyId(familyId);
            budget.setYear(year);
            budget.setMonth(month);
            budget.setType(type);
        }
        budget.setAmount(amount);
        return saveOrUpdate(budget);
    }
    
    @Override
    public Budget getBudget(Long familyId, Integer year, Integer month, Integer type) {
        LambdaQueryWrapper<Budget> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Budget::getFamilyId, familyId)
                    .eq(Budget::getYear, year)
                    .eq(Budget::getType, type);
        if (month != null) {
            queryWrapper.eq(Budget::getMonth, month);
        } else {
            queryWrapper.isNull(Budget::getMonth);
        }
        return getOne(queryWrapper);
    }
}




