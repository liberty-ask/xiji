package com.xiji.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiji.entity.domain.Category;
import com.xiji.mapper.CategoryMapper;
import com.xiji.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 收支类别服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    
    @Override
    public List<Category> getEnabledCategories() {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getStatus, 1)
                    .orderByAsc(Category::getSortOrder)
                    .orderByAsc(Category::getId);
        return list(queryWrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSortOrder(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return false;
        }
        
        try {
            for (int i = 0; i < categoryIds.size(); i++) {
                Category category = new Category();
                category.setId(categoryIds.get(i));
                category.setSortOrder(i + 1);
                category.setUpdatedAt(LocalDateTime.now());
                updateById(category);
            }
            return true;
        } catch (Exception e) {
            log.error("更新类别排序失败", e);
            throw e;
        }
    }
    
    @Override
    public List<Category> getCategoriesByFamily(Long familyId, Integer type) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        
        // 查询条件：系统默认分类（familyId为null）或指定家庭的自定义分类
        if (familyId != null) {
            queryWrapper.and(wrapper -> wrapper
                .isNull(Category::getFamilyId)
                .or()
                .eq(Category::getFamilyId, familyId)
            );
        } else {
            // 如果familyId为null，只查询系统默认分类
            queryWrapper.isNull(Category::getFamilyId);
        }
        
        // 类型过滤
        if (type != null) {
            queryWrapper.eq(Category::getType, type);
        }
        
        // 排序：先按排序序号，再按ID
        queryWrapper.orderByAsc(Category::getSortOrder)
                    .orderByAsc(Category::getId);
        
        return list(queryWrapper);
    }
    
    @Override
    public List<Category> getEnabledCategoriesByFamily(Long familyId, Integer type) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        
        // 查询条件：系统默认分类（familyId为null）或指定家庭的自定义分类
        if (familyId != null) {
            queryWrapper.and(wrapper -> wrapper
                .isNull(Category::getFamilyId)
                .or()
                .eq(Category::getFamilyId, familyId)
            );
        } else {
            // 如果familyId为null，只查询系统默认分类
            queryWrapper.isNull(Category::getFamilyId);
        }
        
        // 类型过滤
        if (type != null) {
            queryWrapper.eq(Category::getType, type);
        }
        
        // 只查询启用的分类
        queryWrapper.eq(Category::getStatus, 1);
        
        // 排序：先按排序序号，再按ID
        queryWrapper.orderByAsc(Category::getSortOrder)
                    .orderByAsc(Category::getId);
        
        return list(queryWrapper);
    }
}

