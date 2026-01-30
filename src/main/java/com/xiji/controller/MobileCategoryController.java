package com.xiji.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiji.common.annotation.CheckPermission;
import com.xiji.common.annotation.OperationLog;
import com.xiji.common.response.ResultVo;
import com.xiji.entity.domain.Category;
import com.xiji.entity.dto.request.AddCategoryRequest;
import com.xiji.entity.dto.request.UpdateCategoryRequest;
import com.xiji.entity.dto.response.MobileCategoryResponse;
import com.xiji.service.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 手机端分类相关接口
 * @author liberty
 */
@RestController
@RequestMapping("/api/v1/mobile/categories")
@Slf4j
@RequiredArgsConstructor
public class MobileCategoryController extends BaseController {

    private final CategoryService categoryService;

    /**
     * 获取分类列表
     * GET /categories
     * @param type 类别类型（可选，0-收入，1-支出）
     */
    @GetMapping
    public ResultVo getCategories(@RequestParam(required = false) Integer type,
                                  HttpServletRequest request) {
        // 获取当前用户ID
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        // 获取当前选择的家庭ID
        Long familyId = getCurrentFamilyId(userId);
        
        // 获取分类列表（系统默认分类 + 家庭自定义分类）
        List<Category> categories = categoryService.getEnabledCategoriesByFamily(familyId, type);
        
        // 转换为响应DTO
        List<MobileCategoryResponse> responseList = categories.stream().map(category -> {
            MobileCategoryResponse response = new MobileCategoryResponse();
            response.setId(category.getId());
            response.setName(category.getName());
            response.setIcon(category.getIcon() != null ? category.getIcon() : "payments");
            response.setType(category.getType());
            return response;
        }).collect(Collectors.toList());

        return ResultVo.success(responseList);
    }
    
    /**
     * 添加分类（家庭自定义分类）
     * POST /categories
     */
    @PostMapping
    @CheckPermission
    @OperationLog(description = "添加家庭自定义分类")
    public ResultVo addCategory(@Valid @RequestBody AddCategoryRequest request, HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        Long familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        // 检查名称是否重复（同一家庭内或系统默认分类中不能重复）
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getName, request.getName().trim())
                    .eq(Category::getType, request.getType())
                    .and(wrapper -> wrapper
                        .eq(Category::getFamilyId, familyId)
                        .or()
                        .isNull(Category::getFamilyId)
                    );
        if (categoryService.count(queryWrapper) > 0) {
            return ResultVo.error("该类型下分类名称已存在");
        }

        // 创建分类
        Category category = new Category();
        category.setFamilyId(familyId);
        category.setName(request.getName().trim());
        category.setIcon(request.getIcon());
        category.setType(request.getType());
        category.setStatus(1); // 默认启用
        
        // 设置排序序号
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        } else {
            // 获取当前最大排序序号
            LambdaQueryWrapper<Category> maxQuery = new LambdaQueryWrapper<>();
            maxQuery.eq(Category::getType, request.getType())
                    .and(wrapper -> wrapper
                        .eq(Category::getFamilyId, familyId)
                        .or()
                        .isNull(Category::getFamilyId)
                    )
                    .orderByDesc(Category::getSortOrder)
                    .last("LIMIT 1");
            Category maxCategory = categoryService.getOne(maxQuery);
            int maxSort = maxCategory != null && maxCategory.getSortOrder() != null 
                ? maxCategory.getSortOrder() + 1 
                : 1;
            category.setSortOrder(maxSort);
        }

        // 创建时间和更新时间由MyBatis-Plus自动填充
        // createdBy和updatedBy需要在业务代码中设置
        category.setCreatedBy(userId);
        category.setUpdatedBy(userId);

        boolean success = categoryService.save(category);
        if (success) {
            // 转换为响应DTO
            MobileCategoryResponse response = new MobileCategoryResponse();
            response.setId(category.getId());
            response.setName(category.getName());
            response.setIcon(category.getIcon() != null ? category.getIcon() : "payments");
            response.setType(category.getType());
            return ResultVo.success("添加成功", response);
        } else {
            return ResultVo.error("添加失败");
        }
    }
    
    /**
     * 更新分类
     * PUT /categories/{id}
     */
    @PutMapping("/{id}")
    @CheckPermission
    @OperationLog(description = "更新分类")
    public ResultVo updateCategory(@PathVariable Long id, 
                                   @Valid @RequestBody UpdateCategoryRequest request,
                                   HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        Long familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        // 查询分类
        Category category = categoryService.getById(id);
        if (category == null) {
            return ResultVo.error("分类不存在");
        }

        // 只能修改本家庭的自定义分类
        if (category.getFamilyId() == null || !category.getFamilyId().equals(familyId)) {
            return ResultVo.error("只能修改本家庭的自定义分类");
        }

        // 检查名称是否与其他分类重复
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Category::getName, request.getName().trim())
                        .eq(Category::getType, category.getType())
                        .ne(Category::getId, id)
                        .and(wrapper -> wrapper
                            .eq(Category::getFamilyId, familyId)
                            .or()
                            .isNull(Category::getFamilyId)
                        );
            if (categoryService.count(queryWrapper) > 0) {
                return ResultVo.error("该类型下分类名称已存在");
            }
            category.setName(request.getName().trim());
        }

        // 更新其他字段
        if (request.getIcon() != null) {
            category.setIcon(request.getIcon());
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }

        category.setUpdatedBy(userId);
        // 更新时间由MyBatis-Plus自动填充

        boolean success = categoryService.updateById(category);
        if (success) {
            // 转换为响应DTO
            MobileCategoryResponse response = new MobileCategoryResponse();
            response.setId(category.getId());
            response.setName(category.getName());
            response.setIcon(category.getIcon() != null ? category.getIcon() : "payments");
            response.setType(category.getType());
            return ResultVo.success("更新成功", response);
        } else {
            return ResultVo.error("更新失败");
        }
    }
    
    /**
     * 删除分类（逻辑删除）
     * DELETE /categories/{id}
     */
    @DeleteMapping("/{id}")
    @CheckPermission
    @OperationLog(description = "删除分类")
    public ResultVo deleteCategory(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        Long familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        // 查询分类
        Category category = categoryService.getById(id);
        if (category == null) {
            return ResultVo.error("分类不存在");
        }

        // 只能删除本家庭的自定义分类
        if (category.getFamilyId() == null || !category.getFamilyId().equals(familyId)) {
            return ResultVo.error("只能删除本家庭的自定义分类");
        }

        // 逻辑删除
        boolean success = categoryService.removeById(id);
        if (success) {
            return ResultVo.success("删除成功");
        } else {
            return ResultVo.error("删除失败");
        }
    }
}
