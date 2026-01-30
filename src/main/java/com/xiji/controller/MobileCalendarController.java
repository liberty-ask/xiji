package com.xiji.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiji.common.response.ResultVo;
import com.xiji.entity.domain.Transactions;
import com.xiji.entity.domain.Category;
import com.xiji.entity.dto.response.CalendarOverviewResponse;
import com.xiji.service.TransactionsService;
import com.xiji.service.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 手机端日历控制器
 * @author liberty
 */
@RestController
@RequestMapping("/api/v1/calendar")
@Slf4j
@RequiredArgsConstructor
public class MobileCalendarController extends BaseController {

    private final TransactionsService transactionsService;
    private final CategoryService categoryService;

    /**
     * 格式化金额（带千分位，不带小数）
     */
    private String formatAmountNoDecimal(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        NumberFormat formatter = NumberFormat.getInstance();
        formatter.setGroupingUsed(true);
        formatter.setMaximumFractionDigits(0);
        formatter.setMinimumFractionDigits(0);
        return formatter.format(amount);
    }

    /**
     * 格式化金额（不带千分位，带两位小数）
     */
    private String formatAmountWithDecimal(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        DecimalFormat df = new DecimalFormat("0.00");
        return df.format(amount);
    }

    /**
     * 获取日历概览数据
     */
    @GetMapping("/overview")
    public ResultVo getCalendarOverview(@RequestParam int year,
                                        @RequestParam int month,
                                        @RequestParam(defaultValue = "1") int day,
                                        HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        Long familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        // 验证月份范围
        if (month < 1 || month > 12) {
            return ResultVo.error("月份参数不正确，应为1-12");
        }

        // 解析月份
        LocalDate targetDate = LocalDate.of(year, month, 1);
        LocalDate firstDayOfMonth = targetDate.withDayOfMonth(1);
        LocalDate lastDayOfMonth = targetDate.withDayOfMonth(targetDate.lengthOfMonth());

        // 查询当月所有交易
        LambdaQueryWrapper<Transactions> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Transactions::getFamilyId, familyId)
                .ge(Transactions::getDate, firstDayOfMonth)
                .le(Transactions::getDate, lastDayOfMonth)
                .orderByDesc(Transactions::getDate)
                .orderByDesc(Transactions::getCreatedAt);

        List<Transactions> transactions = transactionsService.list(queryWrapper);

        // 计算当月总收入和总支出
        BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.getType() != null && t.getType() == 0)
                .map(Transactions::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = transactions.stream()
                .filter(t -> t.getType() != null && t.getType() == 1)
                .map(Transactions::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算结余
        BigDecimal surplus = totalIncome.subtract(totalExpense);

        // 获取所有相关的分类信息
        Map<Long, Category> categoryMap = new HashMap<>();
        for (Transactions transaction : transactions) {
            if (transaction.getCategoryId() != null && !categoryMap.containsKey(transaction.getCategoryId())) {
                Category category = categoryService.getById(transaction.getCategoryId());
                if (category != null) {
                    categoryMap.put(transaction.getCategoryId(), category);
                }
            }
        }

        // 按日期分组交易
        Map<Integer, List<Transactions>> transactionsByDay = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getDate().getDayOfMonth()));

        // 构建每日收支汇总
        Map<String, CalendarOverviewResponse.DailySummary> dailySummary = new LinkedHashMap<>();
        for (int dayOfMonth = 1; dayOfMonth <= lastDayOfMonth.getDayOfMonth(); dayOfMonth++) {
            List<Transactions> dayTransactions = transactionsByDay.getOrDefault(dayOfMonth, List.of());
            
            BigDecimal dayIncome = dayTransactions.stream()
                    .filter(t -> t.getType() != null && t.getType() == 0)
                    .map(Transactions::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal dayExpense = dayTransactions.stream()
                    .filter(t -> t.getType() != null && t.getType() == 1)
                    .map(Transactions::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            CalendarOverviewResponse.DailySummary summary = new CalendarOverviewResponse.DailySummary();
            summary.setIncome(dayIncome);
            summary.setExpense(dayExpense);
            dailySummary.put(String.valueOf(dayOfMonth), summary);
        }

        // 构建每日详情（按日期分组）
        Map<String, List<CalendarOverviewResponse.DailyDetail>> dailyDetails = new LinkedHashMap<>();
        for (int dayOfMonth = 1; dayOfMonth <= lastDayOfMonth.getDayOfMonth(); dayOfMonth++) {
            List<Transactions> dayTransactions = transactionsByDay.getOrDefault(dayOfMonth, List.of());
            
            List<CalendarOverviewResponse.DailyDetail> dayDetails = dayTransactions.stream()
                    .map(transaction -> {
                        CalendarOverviewResponse.DailyDetail detail = new CalendarOverviewResponse.DailyDetail();

                        // 获取分类信息
                        Category category = transaction.getCategoryId() != null 
                                ? categoryMap.get(transaction.getCategoryId()) 
                                : null;

                        // name: 优先使用描述，否则使用分类名称，否则使用默认值
                        if (transaction.getDescription() != null && !transaction.getDescription().isEmpty()) {
                            detail.setName(transaction.getDescription());
                        } else if (category != null && category.getName() != null) {
                            detail.setName(category.getName());
                        } else {
                            detail.setName(transaction.getType() != null && transaction.getType() == 0 ? "收入" : "支出");
                        }

                        // cat: 分类名称
                        detail.setCat(category != null && category.getName() != null ? category.getName() : "");

                        // icon: 图标名称
                        detail.setIcon(category != null && category.getIcon() != null ? category.getIcon() : "");

                        // time: 时间（HH:mm格式）
                        LocalDateTime createdAt = transaction.getCreatedAt();
                        if (createdAt != null) {
                            detail.setTime(createdAt.format(DateTimeFormatter.ofPattern("HH:mm")));
                        } else {
                            detail.setTime("00:00");
                        }
                        detail.setType(transaction.getType());

                        // amount: 金额（带+/-号和空格）
                        boolean isIncome = transaction.getType() != null && transaction.getType() == 0;
                        if (transaction.getAmount() != null) {
                            detail.setAmount(isIncome ? "+ " + transaction.getAmount() : "- " + transaction.getAmount());
                        } else {
                            detail.setAmount(isIncome ? "+ 0.00" : "- 0.00");
                        }
                        detail.setDescription(transaction.getDescription());
                        detail.setCounterparty(transaction.getCounterparty());

                        return detail;
                    })
                    .collect(Collectors.toList());
            
            dailyDetails.put(String.valueOf(dayOfMonth), dayDetails);
        }

        // 构建响应
        CalendarOverviewResponse response = new CalendarOverviewResponse();
        response.setMonthlyIncome(totalIncome);
        response.setMonthlyExpense(totalExpense);
        response.setSurplus(surplus);
        response.setDailySummary(dailySummary);
        response.setDailyDetails(dailyDetails);

        return ResultVo.success(response);
    }
}
