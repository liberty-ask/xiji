package com.xiji.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiji.entity.domain.Category;

import java.util.List;

/**
 * 收支类别服务接口
 */
public interface CategoryService extends IService<Category> {
    
    /**
     * 获取所有启用的类别（按排序序号排序）
     * @return 类别列表
     */
    List<Category> getEnabledCategories();
    
    /**
     * 更新类别排序
     * @param categoryIds 类别ID列表（按排序顺序）
     * @return 是否成功
     */
    boolean updateSortOrder(List<Long> categoryIds);
    
    /**
     * 获取指定家庭的分类列表（系统默认分类 + 家庭自定义分类）
     * @param familyId 家庭ID（如果为null，只返回系统默认分类）
     * @param type 类别类型（0-收入，1-支出，null表示查询所有类型）
     * @return 分类列表
     */
    List<Category> getCategoriesByFamily(Long familyId, Integer type);
    
    /**
     * 获取指定家庭启用的分类列表（系统默认分类 + 家庭自定义分类）
     * @param familyId 家庭ID（如果为null，只返回系统默认分类）
     * @param type 类别类型（0-收入，1-支出，null表示查询所有类型）
     * @return 分类列表
     */
    List<Category> getEnabledCategoriesByFamily(Long familyId, Integer type);
}

