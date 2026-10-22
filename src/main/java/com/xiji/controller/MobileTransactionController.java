package com.xiji.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiji.common.annotation.OperationLog;
import com.xiji.common.response.ResultVo;
import com.xiji.entity.domain.Category;
import com.xiji.entity.domain.Transactions;
import com.xiji.entity.dto.request.MobileTransactionRequest;
import com.xiji.entity.dto.request.MobileTransactionUpdateRequest;
import com.xiji.entity.dto.request.MobileTransactionListRequest;
import com.xiji.entity.dto.response.MobileTransactionListResponse;
import com.xiji.entity.dto.response.MobileTransactionResponse;
import com.xiji.service.CategoryService;
import com.xiji.service.TransactionsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 手机端交易记录控制器
 * @author liberty
 */
@RestController
@RequestMapping("/api/v1/mobile/transactions")
@Slf4j
@RequiredArgsConstructor
public class MobileTransactionController extends BaseController {

    private final TransactionsService transactionsService;
    private final CategoryService categoryService;

    /**
     * 添加交易记录
     */
    @OperationLog(description = "新增交易")
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public ResultVo addTransaction(@Valid @RequestBody MobileTransactionRequest request, HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        // 获取当前选择的家庭ID
        String familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        // 查询分类（系统默认分类 + 家庭自定义分类）
        List<Category> categories = categoryService.getEnabledCategoriesByFamily(familyId, request.getType());
        Category category = categories.stream()
                .filter(c -> request.getCategory().equals(c.getName()))
                .findFirst()
                .orElse(null);
        if (category == null) {
            return ResultVo.error("分类不存在");
        }

        // 构建交易记录
        Transactions transaction = new Transactions();
        transaction.setId(java.util.UUID.randomUUID().toString());
        transaction.setFamilyId(familyId);
        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setCategoryId(category.getId());
        transaction.setDate(LocalDate.parse(request.getDate()));
        transaction.setDescription(request.getNote());
        transaction.setPayMethod(request.getLocation());
        transaction.setCreatedBy(userId);
        // 创建时间和更新时间由MyBatis-Plus自动填充

        if (transactionsService.save(transaction)) {
            return ResultVo.success("添加成功");
        } else {
            return ResultVo.error("添加失败");
        }
    }

    /**
     * 获取交易列表
     */
    @GetMapping
    public ResultVo getTransactions(MobileTransactionListRequest request, HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        String familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        // 设置默认分页参数
        int currentPage = request.getPage() != null && request.getPage() > 0 ? request.getPage() : 1;
        int pageSize = request.getPageSize() != null && request.getPageSize() > 0 ? request.getPageSize() : 10;

        LambdaQueryWrapper<Transactions> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Transactions::getFamilyId, familyId);

        // 类型过滤
        if (request.getType() != null) {
            queryWrapper.eq(Transactions::getType, request.getType());
        }
        // 用户过滤
        if(request.getUserId() != null){
            queryWrapper.eq(Transactions::getCreatedBy, request.getUserId());
        }
        // 日期范围过滤
        if (request.getStartDate() != null) {
            queryWrapper.ge(Transactions::getDate, request.getStartDate());
        }
        if (request.getEndDate() != null) {
            queryWrapper.le(Transactions::getDate, request.getEndDate());
        }
        if(StrUtil.isNotBlank(request.getKeyword())){
            queryWrapper.and(wrapper ->
                    wrapper.like(Transactions::getDescription, request.getKeyword())
                            .or()
                            .like(Transactions::getCounterparty, request.getKeyword())
            );
        }

        // 排序
        queryWrapper.orderByDesc(Transactions::getDate)
                .orderByDesc(Transactions::getId);

        // 分页查询
        Page<Transactions> page = new Page<>(currentPage, pageSize);
        IPage<Transactions> pageResult = transactionsService.page(page, queryWrapper);

        // 批量查询分类信息（避免循环点查）
        Set<String> categoryIds = pageResult.getRecords().stream()
                .map(Transactions::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, Category> categoryMap = categoryIds.isEmpty() ? Map.of()
                : categoryService.listByIds(categoryIds).stream()
                    .collect(Collectors.toMap(Category::getId, c -> c));

        // 转换为响应DTO
        List<MobileTransactionResponse> transactionResponses = pageResult.getRecords().stream()
                .map(t -> {
                    MobileTransactionResponse response = new MobileTransactionResponse();
                    response.setId(t.getId());
                    response.setType(t.getType());
                    response.setAmount(t.getAmount());
                    Category cat = t.getCategoryId() != null ? categoryMap.get(t.getCategoryId()) : null;
                    response.setCategory(cat != null ? cat.getName() : "");
                    response.setIcon(cat != null ? cat.getIcon() : null);
                    response.setDate(t.getDate());
                    response.setDescription(t.getDescription());
                    response.setUserId(t.getCreatedBy());
                    response.setCounterparty(t.getCounterparty());
                    return response;
                })
                .collect(Collectors.toList());

        MobileTransactionListResponse response = new MobileTransactionListResponse();
        response.setList(transactionResponses);
        response.setTotal((int) pageResult.getTotal());
        response.setPage(currentPage);
        response.setPageSize(pageSize);

        return ResultVo.success(response);
    }

    /**
     * 修改交易记录（只能修改自己创建的记录）
     */
    @OperationLog(description = "修改交易")
    @PutMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public ResultVo updateTransaction(@PathVariable String id,
                                     @Valid @RequestBody MobileTransactionUpdateRequest request, 
                                     HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        // 获取当前选择的家庭ID
        String familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        // 查询原始交易记录
        Transactions oldTransaction = transactionsService.getById(id);
        if (oldTransaction == null) {
            return ResultVo.error("交易记录不存在");
        }

        // 权限校验：只能修改自己创建的记录
        if (!userId.equals(oldTransaction.getCreatedBy())) {
            return ResultVo.error("只能修改自己创建的记录");
        }

        // 验证交易记录是否属于当前家庭
        if (!familyId.equals(oldTransaction.getFamilyId())) {
            return ResultVo.error("交易记录不属于当前家庭");
        }

        // 查询分类（系统默认分类 + 家庭自定义分类）
        List<Category> categories = categoryService.getEnabledCategoriesByFamily(familyId, request.getType());
        Category category = categories.stream()
                .filter(c -> request.getCategory().equals(c.getName()))
                .findFirst()
                .orElse(null);
        if (category == null) {
            return ResultVo.error("分类不存在");
        }

        // 构建交易记录
        Transactions transaction = new Transactions();
        transaction.setId(id);
        transaction.setFamilyId(familyId); // 保持原有的家庭ID
        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setCategoryId(category.getId());
        transaction.setDate(LocalDate.parse(request.getDate()));
        transaction.setDescription(request.getNote());
        transaction.setPayMethod(request.getLocation());
        transaction.setUpdatedBy(userId);
        if (transactionsService.updateById(transaction)) {
            return ResultVo.success("修改成功");
        } else {
            return ResultVo.error("修改失败");
        }
    }

    /**
     * 删除交易记录（只能删除自己创建的记录）
     */
    @OperationLog(description = "删除交易")
    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public ResultVo deleteTransaction(@PathVariable String id, HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        // 查询交易记录
        Transactions transaction = transactionsService.getById(id);
        if (transaction == null) {
            return ResultVo.error("交易记录不存在");
        }

        // 权限校验：只能删除自己创建的记录
        if (!userId.equals(transaction.getCreatedBy())) {
            return ResultVo.error("只能删除自己创建的记录");
        }

        // 逻辑删除（MyBatis-Plus会自动处理逻辑删除）
        if (transactionsService.removeById(id)) {
            return ResultVo.success("删除成功");
        } else {
            return ResultVo.error("删除失败");
        }
    }

}
