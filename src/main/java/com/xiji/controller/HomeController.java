package com.xiji.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiji.entity.domain.Transactions;
import com.xiji.entity.domain.Category;
import com.xiji.entity.domain.User;
import com.xiji.entity.domain.Budget;
import com.xiji.entity.dto.response.HomeResponse;
import com.xiji.service.TransactionsService;
import com.xiji.service.BudgetService;
import com.xiji.service.CategoryService;
import com.xiji.service.UserService;
import com.xiji.common.response.ResultVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页统计
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
@RequiredArgsConstructor
public class HomeController extends BaseController {

    private final TransactionsService transactionsService;
    private final BudgetService budgetService;
    private final CategoryService categoryService;
    private final UserService userService;

    /**
     * 获取首页数据
     */
    @GetMapping("/home")
    public ResultVo home(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        Long familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        LocalDate now = LocalDate.now(ZoneId.systemDefault());
        YearMonth currentYearMonth = YearMonth.from(now);
        LocalDate firstDayOfMonth = currentYearMonth.atDay(1);
        LocalDate lastDayOfMonth = currentYearMonth.atEndOfMonth();

        // 查询本月所有交易
        LambdaQueryWrapper<Transactions> monthQuery = new LambdaQueryWrapper<>();
        monthQuery.eq(Transactions::getFamilyId, familyId)
                .ge(Transactions::getDate, firstDayOfMonth)
                .le(Transactions::getDate, lastDayOfMonth);
        List<Transactions> monthTransactions = transactionsService.list(monthQuery);

        // 统计本月收入和支出
        BigDecimal totalIncome = monthTransactions.stream()
                .filter(t -> t.getType() != null && t.getType() == 0 && t.getAmount() != null)
                .map(Transactions::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = monthTransactions.stream()
                .filter(t -> t.getType() != null && t.getType() == 1 && t.getAmount() != null)
                .map(Transactions::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算余额（收入-支出）
        BigDecimal balance = totalIncome.subtract(totalExpense);

        // 查询今日支出
        LocalDate today = now;
        LambdaQueryWrapper<Transactions> todayQuery = new LambdaQueryWrapper<>();
        todayQuery.eq(Transactions::getFamilyId, familyId)
                .eq(Transactions::getDate, today)
                .eq(Transactions::getType, 1);
        List<Transactions> todayTransactions = transactionsService.list(todayQuery);
        BigDecimal todayExpense = todayTransactions.stream()
                .filter(t -> t.getAmount() != null)
                .map(Transactions::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 查询昨日支出
        LocalDate yesterday = now.minusDays(1);
        LambdaQueryWrapper<Transactions> yesterdayQuery = new LambdaQueryWrapper<>();
        yesterdayQuery.eq(Transactions::getFamilyId, familyId)
                .eq(Transactions::getDate, yesterday)
                .eq(Transactions::getType, 1);
        List<Transactions> yesterdayTransactions = transactionsService.list(yesterdayQuery);
        BigDecimal yesterdayExpense = yesterdayTransactions.stream()
                .filter(t -> t.getAmount() != null)
                .map(Transactions::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 获取本月支出预算
        Budget budgetEntity = budgetService.getBudget(familyId);
        BigDecimal budgetTotal = budgetEntity != null && budgetEntity.getAmount() != null 
                ? budgetEntity.getAmount() 
                : BigDecimal.ZERO;

        // 构建响应
        HomeResponse response = new HomeResponse();
        response.setBalance(formatAmount(balance));
        response.setIncome(formatAmount(totalIncome));
        response.setExpense(formatAmount(totalExpense));
        response.setTodayExpense(formatAmount(todayExpense));
        response.setYesterdayExpense(formatAmount(yesterdayExpense));

        // 预算信息
        HomeResponse.BudgetInfo budgetInfo = new HomeResponse.BudgetInfo();
        budgetInfo.setUsed(totalExpense);
        budgetInfo.setTotal(budgetTotal);
        response.setBudget(budgetInfo);

        // 最近活动（最近10条交易记录，按创建时间倒序）
        LambdaQueryWrapper<Transactions> recentQuery = new LambdaQueryWrapper<>();
        recentQuery.eq(Transactions::getFamilyId, familyId)
                .orderByDesc(Transactions::getDate)
                .orderByDesc(Transactions::getCreatedAt)
                .ne(Transactions::getType, 2)
                .last("LIMIT 10");
        List<Transactions> recentTransactions = transactionsService.list(recentQuery);

        // 获取所有相关的分类和用户信息
        Map<Long, Category> categoryMap = new HashMap<>();
        Map<Long, User> userMap = new HashMap<>();
        
        for (Transactions transaction : recentTransactions) {
            if (transaction.getCategoryId() != null && !categoryMap.containsKey(transaction.getCategoryId())) {
                Category category = categoryService.getById(transaction.getCategoryId());
                if (category != null) {
                    categoryMap.put(transaction.getCategoryId(), category);
                }
            }
            if (transaction.getCreatedBy() != null && !userMap.containsKey(transaction.getCreatedBy())) {
                User user = userService.getById(transaction.getCreatedBy());
                if (user != null) {
                    userMap.put(transaction.getCreatedBy(), user);
                }
            }
        }

        // 转换为活动项
        List<HomeResponse.ActivityItem> activities = recentTransactions.stream().map(transaction -> {
            HomeResponse.ActivityItem item = new HomeResponse.ActivityItem();
            
            // 标题：优先使用分类名称，否则使用描述
            Category category = transaction.getCategoryId() != null 
                    ? categoryMap.get(transaction.getCategoryId()) 
                    : null;
            if (category != null && category.getName() != null) {
                item.setTitle(category.getName());
                item.setIcon(category.getIcon());
            } else {
                item.setTitle(transaction.getDescription() != null && !transaction.getDescription().isEmpty() 
                        ? transaction.getDescription() 
                        : (transaction.getType() == 0 ? "收入" : "支出"));
                item.setIcon(null);
            }
            
            // 用户名称
            User user = transaction.getCreatedBy() != null 
                    ? userMap.get(transaction.getCreatedBy()) 
                    : null;
            item.setUser(user != null && user.getName() != null ? user.getName() 
                    : (user != null && user.getUsername() != null ? user.getUsername() : "未知用户"));
            
            // 时间（HH:mm格式）
            LocalDateTime createdAt = transaction.getCreatedAt();
            if (createdAt != null) {
                item.setTime(createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            } else if (transaction.getDate() != null) {
                item.setTime(LocalTime.of(0, 0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            } else {
                item.setTime("00:00");
            }
            LocalDate transactionDate = transaction.getDate();
            if ( transactionDate != null) {
                item.setDate(transactionDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }

            // 金额（带+/-号）
            boolean isIncome = transaction.getType() != null && transaction.getType() == 0;
            item.setIsIncome(isIncome);
            if (transaction.getAmount() != null) {
                String amountStr = formatAmount(transaction.getAmount());
                item.setAmount(isIncome ? "+" + amountStr : "-" + amountStr);
            } else {
                item.setAmount(isIncome ? "+0" : "-0");
            }
            item.setDescription(transaction.getDescription());
            // 交易方
            item.setCounterparty(transaction.getCounterparty());
            return item;
        }).collect(Collectors.toList());

        response.setActivities(activities);

        return ResultVo.success(response);
    }

    /**
     * 格式化金额为字符串（千分位格式化）
     */
    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        NumberFormat formatter = NumberFormat.getInstance();
        formatter.setGroupingUsed(true);
        formatter.setMaximumFractionDigits(2);
        formatter.setMinimumFractionDigits(2);
        return formatter.format(amount);
    }
}
