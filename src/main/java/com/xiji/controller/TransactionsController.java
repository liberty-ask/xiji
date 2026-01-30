package com.xiji.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiji.common.annotation.OperationLog;
import com.xiji.entity.domain.Transactions;
import com.xiji.entity.dto.request.PageParamType;
import com.xiji.entity.dto.response.PageData;
import com.xiji.service.TransactionsService;
import com.xiji.common.response.ResultVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 记账
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Slf4j
@RequiredArgsConstructor
public class TransactionsController extends BaseController {

    private final TransactionsService transactionsService;

    /**
     * 新增收支记录
     */
    @OperationLog(description = "新增收支记录")
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public ResultVo add(@RequestBody Transactions transaction, HttpServletRequest request) {
        // 获取当前用户ID
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        // 获取当前用户的当前家庭ID
        Long familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        // 参数验证
        if (transaction.getAmount() == null || transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResultVo.error("金额必须大于0");
        }
        // 限制最大金额（防止异常数据）
        if (transaction.getAmount().compareTo(new BigDecimal("1000000")) > 0) {
            return ResultVo.error("单笔交易金额不能超过1000000");
        }
        if (transaction.getType() == null) {
            return ResultVo.error("交易类型不能为空");
        }
        if (transaction.getType() != 0 && transaction.getType() != 1) {
            return ResultVo.error("交易类型不正确，0为收入，1为支出");
        }
        
        // 设置家庭ID和创建人（不从前端获取）
        transaction.setFamilyId(familyId);
        transaction.setCreatedBy(userId);
        // 创建时间和更新时间由MyBatis-Plus自动填充
        
        // 保存交易记录
        if (!transactionsService.save(transaction)) {
            return ResultVo.error("保存交易记录失败");
        }
        
        return ResultVo.success("添加成功");
    }

    /**
     * 删除收支记录
     */
    @OperationLog(description = "删除收支记录")
    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public ResultVo delete(@PathVariable Long id) {
        // 删除交易记录
        if (!transactionsService.removeById(id)) {
            return ResultVo.error("删除交易记录失败");
        }
        
        return ResultVo.success("删除成功");
    }

    /**
     * 根据ID查找账单
     */
    @GetMapping("/{id}")
    public ResultVo selectById(@PathVariable Long id) {
        Transactions result = transactionsService.getById(id);
        return ResultVo.success(result);
    }

    /**
     * 修改收支记录
     */
    @OperationLog(description = "修改收支记录")
    @PutMapping
    @Transactional(rollbackFor = Exception.class)
    public ResultVo update(@RequestBody Transactions transaction, HttpServletRequest request) {
        if (transaction.getId() == null) {
            return ResultVo.error("交易记录ID不能为空");
        }
        
        // 获取当前用户ID
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }
        
        // 查询原始交易记录
        Transactions oldTransactions = transactionsService.getById(transaction.getId());
        if (oldTransactions == null) {
            return ResultVo.error("交易记录不存在");
        }
        
        // 设置更新人
        transaction.setUpdatedBy(userId);
        log.info("update transaction: id={}", transaction.getId());
        
        if (!transactionsService.updateById(transaction)) {
            return ResultVo.error("更新交易记录失败");
        }
        
        return ResultVo.success("更新成功");
    }

    /**
     * 分页获取记账列表
     */
    @GetMapping
    public ResultVo getList(PageParamType param, HttpServletRequest request) {
        // 获取当前用户ID
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        // 获取当前用户的当前家庭ID
        Long familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        // 构建查询条件
        LambdaQueryWrapper<Transactions> queryWrapper = new LambdaQueryWrapper<>();
        // 根据当前用户的当前家庭过滤
        queryWrapper.eq(Transactions::getFamilyId, familyId);

        // 搜索条件：描述字段模糊查询
        if (StringUtils.isNotEmpty(param.getValue())) {
            queryWrapper.and(wrapper ->
                    wrapper.like(Transactions::getDescription, param.getValue())
                            .or()
                            .like(Transactions::getCounterparty, param.getValue())
            );
        }

        // 类型过滤：0-收入，1-支出
        if (param.getType() != null) {
            queryWrapper.eq(Transactions::getType, param.getType());
        }

        // 按日期降序、ID降序排序
        queryWrapper.orderByDesc(Transactions::getDate)
                    .orderByDesc(Transactions::getId);

        // 创建分页对象
        int currentPage = Optional.ofNullable(param.getCurrentPage()).orElse(1);
        int pageSize = Optional.ofNullable(param.getPageSize()).orElse(10);
        Page<Transactions> page = new Page<>(currentPage, pageSize);

        // 执行分页查询
        IPage<Transactions> pageResult = transactionsService.page(page, queryWrapper);

        // 封装分页数据
        PageData pageData = new PageData();
        pageData.setRecords(pageResult.getRecords());
        pageData.setTotal((int) pageResult.getTotal());
        pageData.setCurrent((int) pageResult.getCurrent());
        pageData.setSize((int) pageResult.getSize());
        pageData.setPages((int) pageResult.getPages());

        return ResultVo.success(pageData);
    }

}
