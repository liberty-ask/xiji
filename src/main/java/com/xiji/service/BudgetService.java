package com.xiji.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiji.entity.domain.Budget;

import java.math.BigDecimal;
import java.util.List;

/**
 * 预算服务接口
 * @author liberty
 */
public interface BudgetService extends IService<Budget> {
    
    /**
     * 获取或创建预算
     * @param familyId 家庭ID
     * @param year 年份
     * @param month 月份（1-12，如果为null则表示年度预算）
     * @param type 预算类型（0-收入，1-支出）
     * @return 预算对象
     */
    Budget getOrCreateBudget(Long familyId, Integer year, Integer month, Integer type);
    
    /**
     * 设置预算
     * @param familyId 家庭ID
     * @param year 年份
     * @param month 月份（1-12，如果为null则表示年度预算）
     * @param amount 预算金额
     * @param type 预算类型（0-收入，1-支出）
     * @return 是否成功
     */
    boolean setBudget(Long familyId, Integer year, Integer month, BigDecimal amount, Integer type);
    
    /**
     * 获取预算
     * @param familyId 家庭ID
     * @param year 年份
     * @param month 月份（1-12，如果为null则表示年度预算）
     * @param type 预算类型（0-收入，1-支出）
     * @return 预算对象，如果不存在返回null
     */
    Budget getBudget(Long familyId, Integer year, Integer month, Integer type);
}




