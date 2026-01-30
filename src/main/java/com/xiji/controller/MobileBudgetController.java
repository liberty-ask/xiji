package com.xiji.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiji.common.annotation.OperationLog;
import com.xiji.common.response.ResultVo;
import com.xiji.entity.domain.Budget;
import com.xiji.entity.domain.Transactions;
import com.xiji.entity.dto.request.SetBudgetRequest;
import com.xiji.entity.dto.response.MobileBudgetResponse;
import com.xiji.service.BudgetService;
import com.xiji.service.TransactionsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 手机端预算控制器
 * @author liberty
 */
@RestController
@RequestMapping("/api/v1/budgets")
@Slf4j
@RequiredArgsConstructor
public class MobileBudgetController extends BaseController {

    private final TransactionsService transactionsService;
    private final BudgetService budgetService;

    /**
     * 获取预算信息
     * GET /budget
     */
    @GetMapping
    public ResultVo getBudget(@RequestParam(required = false) Integer year,
                              @RequestParam(required = false) Integer month,
                              HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        Long familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        // 使用当前年月（如果未指定）
        LocalDate now = LocalDate.now();
        int currentYear = year != null ? year : now.getYear();
        int currentMonth = month != null ? month : now.getMonthValue();

        if (currentMonth < 1 || currentMonth > 12) {
            return ResultVo.error("月份参数不正确，应为1-12");
        }

        // 计算月份的开始和结束日期
        YearMonth yearMonth = YearMonth.of(currentYear, currentMonth);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 查询该月的所有支出（当前家庭）
        List<Transactions> transactions = transactionsService.list(
            new LambdaQueryWrapper<Transactions>()
                .eq(Transactions::getFamilyId, familyId)
                .eq(Transactions::getType, 1) // 只查询支出
                .ge(Transactions::getDate, startDate)
                .le(Transactions::getDate, endDate));

        // 计算已使用金额
        BigDecimal used = transactions.stream()
            .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 从数据库获取预算（支出类型）
        Budget budget = budgetService.getBudget(familyId, currentYear, currentMonth, 1);
        BigDecimal total = budget != null && budget.getAmount() != null 
            ? budget.getAmount() 
            : BigDecimal.ZERO;

        // 计算剩余金额
        BigDecimal remaining = total.subtract(used);

        // 计算使用百分比
        Double percentage = BigDecimal.ZERO.compareTo(total) == 0 
            ? 0.0 
            : used.divide(total, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        MobileBudgetResponse response = new MobileBudgetResponse();
        response.setBudget(total);
        response.setUsed(used);
        response.setRemaining(remaining);
        response.setPercentage(percentage);

        return ResultVo.success(response);
    }

    /**
     * 设置预算
     * POST /budget
     */
    @OperationLog(description = "设置预算")
    @PostMapping
    public ResultVo setBudget(@Valid @RequestBody SetBudgetRequest request, HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        Long familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        // 使用当前年月作为默认值
        LocalDate now = LocalDate.now();
        int year = request.getYear() != null ? request.getYear() : now.getYear();
        int month = request.getMonth() != null ? request.getMonth() : now.getMonthValue();

        if (month < 1 || month > 12) {
            return ResultVo.error("月份参数不正确，应为1-12");
        }

        // 设置预算（支出类型）
        boolean success = budgetService.setBudget(familyId, year, month, request.getTotal(), 1);
        if (!success) {
            return ResultVo.error("设置预算失败");
        }

        // 计算已使用金额（与getBudget逻辑相同）
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Transactions> transactions = transactionsService.list(
            new LambdaQueryWrapper<Transactions>()
                .eq(Transactions::getFamilyId, familyId)
                .eq(Transactions::getType, 1)
                .ge(Transactions::getDate, startDate)
                .le(Transactions::getDate, endDate));

        BigDecimal used = transactions.stream()
            .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = request.getTotal();

        // 计算剩余金额
        BigDecimal remaining = total.subtract(used);

        // 计算使用百分比
        Double percentage = BigDecimal.ZERO.compareTo(total) == 0 
            ? 0.0 
            : used.divide(total, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        MobileBudgetResponse response = new MobileBudgetResponse();
        response.setBudget(total);
        response.setUsed(used);
        response.setRemaining(remaining);
        response.setPercentage(percentage);

        return ResultVo.success("设置成功", response);
    }
}
