package com.xiji.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiji.common.annotation.CheckPermission;
import com.xiji.common.annotation.OperationLog;
import com.xiji.common.response.ResultVo;
import com.xiji.entity.domain.Category;
import com.xiji.entity.dto.request.CreateCategoryRequest;
import com.xiji.entity.dto.request.UpdateCategoryRequest;
import com.xiji.entity.dto.response.CategoryResponse;
import com.xiji.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 收支类别管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
    
    private final CategoryService categoryService;
    
    /**
     * 获取所有类别（按排序序号排序）
     * @param familyId 家庭ID（可选，如果提供则返回系统默认+家庭自定义，否则只返回系统默认）
     * @param type 类别类型（可选，0-收入，1-支出）
     */
    @GetMapping("/list")
    public ResultVo list(@RequestParam(required = false) Long familyId,
                         @RequestParam(required = false) Integer type) {
        List<Category> categories = categoryService.getCategoriesByFamily(familyId, type);
        List<CategoryResponse> responseList = categories.stream().map(category -> {
            CategoryResponse response = new CategoryResponse();
            BeanUtils.copyProperties(category, response);
            return response;
        }).collect(Collectors.toList());
        return ResultVo.success(responseList);
    }
    
    /**
     * 获取所有启用的类别（用于下拉选择）
     * @param familyId 家庭ID（可选，如果提供则返回系统默认+家庭自定义，否则只返回系统默认）
     * @param type 类别类型（可选，0-收入，1-支出）
     */
    @GetMapping("/enabled")
    public ResultVo getEnabledCategories(@RequestParam(required = false) Long familyId,
                                         @RequestParam(required = false) Integer type) {
        List<Category> categories = categoryService.getEnabledCategoriesByFamily(familyId, type);
        List<CategoryResponse> responseList = categories.stream().map(category -> {
            CategoryResponse response = new CategoryResponse();
            BeanUtils.copyProperties(category, response);
            return response;
        }).collect(Collectors.toList());
        return ResultVo.success(responseList);
    }
    
    /**
     * 根据ID获取类别详情
     */
    @GetMapping("/{id}")
    public ResultVo getById(@PathVariable Long id) {
        Category category = categoryService.getById(id);
        if (category == null) {
            return ResultVo.error("类别不存在");
        }
        CategoryResponse response = new CategoryResponse();
        BeanUtils.copyProperties(category, response);
        return ResultVo.success(response);
    }
    
    /**
     * 新增类别
     * 如果familyId为null，创建系统默认分类；如果familyId有值，创建家庭自定义分类
     */
    @PostMapping
    @CheckPermission
    @OperationLog(description = "新增收支类别")
    public ResultVo add(@Valid @RequestBody CreateCategoryRequest request) {
        // 检查名称是否重复（同一家庭内或系统默认分类中不能重复）
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getName, request.getName().trim())
                    .eq(Category::getType, request.getType());
        if (request.getFamilyId() != null) {
            // 家庭自定义分类：检查该家庭内是否重复，以及系统默认分类中是否重复
            queryWrapper.and(wrapper -> wrapper
                .eq(Category::getFamilyId, request.getFamilyId())
                .or()
                .isNull(Category::getFamilyId)
            );
        } else {
            // 系统默认分类：只检查系统默认分类中是否重复
            queryWrapper.isNull(Category::getFamilyId);
        }
        if (categoryService.count(queryWrapper) > 0) {
            return ResultVo.error("该类型下类别名称已存在");
        }
        
        // 创建分类对象
        Category category = new Category();
        category.setName(request.getName().trim());
        category.setIcon(request.getIcon());
        category.setType(request.getType());
        category.setFamilyId(request.getFamilyId());
        
        // 设置默认值
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        } else {
            // 获取当前最大排序序号（同一类型、同一家庭范围内）
            LambdaQueryWrapper<Category> maxQuery = new LambdaQueryWrapper<>();
            maxQuery.eq(Category::getType, request.getType());
            if (request.getFamilyId() != null) {
                maxQuery.and(wrapper -> wrapper
                    .eq(Category::getFamilyId, request.getFamilyId())
                    .or()
                    .isNull(Category::getFamilyId)
                );
            } else {
                maxQuery.isNull(Category::getFamilyId);
            }
            maxQuery.orderByDesc(Category::getSortOrder)
                    .last("LIMIT 1");
            Category maxCategory = categoryService.getOne(maxQuery);
            int maxSort = maxCategory != null && maxCategory.getSortOrder() != null 
                ? maxCategory.getSortOrder() + 1 
                : 1;
            category.setSortOrder(maxSort);
        }
        
        category.setStatus(request.getStatus() != null ? request.getStatus() : 1); // 默认启用
        
        // 创建时间和更新时间由MyBatis-Plus自动填充
        
        boolean success = categoryService.save(category);
        if (success) {
            CategoryResponse response = new CategoryResponse();
            BeanUtils.copyProperties(category, response);
            return ResultVo.success("新增成功", response);
        } else {
            return ResultVo.error("新增失败");
        }
    }
    
    /**
     * 更新类别
     */
    @PutMapping("/{id}")
    @CheckPermission
    @OperationLog(description = "更新收支类别")
    public ResultVo update(@PathVariable Long id, @Valid @RequestBody UpdateCategoryRequest request) {
        Category existingCategory = categoryService.getById(id);
        if (existingCategory == null) {
            return ResultVo.error("类别不存在");
        }
        
        // 检查名称是否与其他类别重复（同一类型、同一家庭范围内）
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getName, request.getName().trim())
                    .eq(Category::getType, existingCategory.getType())
                    .ne(Category::getId, id);
        if (existingCategory.getFamilyId() != null) {
            queryWrapper.and(wrapper -> wrapper
                .eq(Category::getFamilyId, existingCategory.getFamilyId())
                .or()
                .isNull(Category::getFamilyId)
            );
        } else {
            queryWrapper.isNull(Category::getFamilyId);
        }
        if (categoryService.count(queryWrapper) > 0) {
            return ResultVo.error("该类型下类别名称已存在");
        }
        existingCategory.setName(request.getName().trim());
        
        if (request.getIcon() != null) {
            existingCategory.setIcon(request.getIcon());
        }
        
        if (request.getSortOrder() != null) {
            existingCategory.setSortOrder(request.getSortOrder());
        }
        
        if (request.getStatus() != null) {
            existingCategory.setStatus(request.getStatus());
        }
        
        // 更新时间由MyBatis-Plus自动填充
        
        boolean success = categoryService.updateById(existingCategory);
        if (success) {
            CategoryResponse response = new CategoryResponse();
            BeanUtils.copyProperties(existingCategory, response);
            return ResultVo.success("更新成功", response);
        } else {
            return ResultVo.error("更新失败");
        }
    }
    
    /**
     * 删除类别
     */
    @DeleteMapping("/{id}")
    @CheckPermission
    @OperationLog(description = "删除收支类别")
    public ResultVo delete(@PathVariable Long id) {
        Category category = categoryService.getById(id);
        if (category == null) {
            return ResultVo.error("类别不存在");
        }
        
        // 检查是否有关联的交易记录（可选，根据业务需求决定是否允许删除）
        // 这里暂时允许删除，实际项目中可能需要检查关联数据
        
        boolean success = categoryService.removeById(id);
        if (success) {
            return ResultVo.success("删除成功");
        } else {
            return ResultVo.error("删除失败");
        }
    }
    
    /**
     * 更新类别排序
     */
    @PutMapping("/sort")
    @CheckPermission
    @OperationLog(description = "更新类别排序")
    public ResultVo updateSort(@RequestBody List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return ResultVo.error("类别ID列表不能为空");
        }
        
        boolean success = categoryService.updateSortOrder(categoryIds);
        if (success) {
            return ResultVo.success("排序更新成功");
        } else {
            return ResultVo.error("排序更新失败");
        }
    }
}

