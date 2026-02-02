package com.xiji.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiji.entity.domain.Transactions;
import com.xiji.entity.domain.Category;
import com.xiji.entity.domain.User;
import com.xiji.entity.domain.Budget;
import com.xiji.entity.dto.response.StatisticsTotalResponse;
import com.xiji.entity.dto.response.CategoryRankResponse;
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
     * 统计本月家庭收入和支出
     */
    @GetMapping("/home/count/total")
    public ResultVo incomeExpense(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        Long familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        LambdaQueryWrapper<Transactions> queryWrapper = new LambdaQueryWrapper<>();
        // 根据当前用户的当前家庭过滤
        queryWrapper.eq(Transactions::getFamilyId, familyId);
        // 获取当前日期
        LocalDate now = LocalDate.now(ZoneId.systemDefault());
        // 获取当前月份
        YearMonth currentYearMonth = YearMonth.from(now);
        // 获取本月的第一天
        LocalDate firstDayOfMonth = currentYearMonth.atDay(1);
        // 获取本月的最后一天
        LocalDate lastDayOfMonth = currentYearMonth.atEndOfMonth();
        log.info("当前{}",firstDayOfMonth);
        // 构建查询
        queryWrapper.ge(Transactions::getDate,firstDayOfMonth)
                .le(Transactions::getDate,lastDayOfMonth);
        // 查询
        List<Transactions> transactions = transactionsService.list(queryWrapper);
        log.info("列表{}",transactions);
        // 统计收入条数
        int countType0 = (int) transactions.stream()
                .filter(t -> t.getType() == 0)
                .count();
        BigDecimal totalAmountType0 = transactions.stream()
                .filter(t -> t.getType() == 0)
                .map(Transactions::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.info("type为0的条数: {},总金额: {}", countType0, totalAmountType0);
        // 统计支出条数
        int countType1 = (int) transactions.stream()
                .filter(t -> t.getType() == 1)
                .count();
        BigDecimal totalAmountType1 = transactions.stream()
                .filter(t -> t.getType() == 1)
                .map(Transactions::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.info("type为1的条数：{},总金额：{}",countType1,totalAmountType1);
        // 构建返回结果
        StatisticsTotalResponse response = new StatisticsTotalResponse();
        response.setTotalIncome(totalAmountType0);
        response.setTotalExpense(totalAmountType1);
        response.setIncomeCount(countType0);
        response.setExpenseCount(countType1);
        return ResultVo.success(response);
    }


    /**
     * 统计收入和支出排行榜
     */
    @GetMapping("/home/count/rank")
    public ResultVo rank(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        Long familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        // 查询本月的记录
        LambdaQueryWrapper<Transactions> queryWrapper = new LambdaQueryWrapper<>();
        // 根据当前用户的当前家庭过滤
        queryWrapper.eq(Transactions::getFamilyId, familyId);
        // 获取当前日期
        LocalDate now = LocalDate.now(ZoneId.systemDefault());
        // 获取当前月份
        YearMonth currentYearMonth = YearMonth.from(now);
        // 获取本月的第一天
        LocalDate firstDayOfMonth = currentYearMonth.atDay(1);
        // 获取本月的最后一天
        LocalDate lastDayOfMonth = currentYearMonth.atEndOfMonth();
        log.info("当前{}",firstDayOfMonth);
        // 构建查询
        queryWrapper.ge(Transactions::getDate,firstDayOfMonth)
                .le(Transactions::getDate,lastDayOfMonth);
        // 查询
        List<Transactions> transactions = transactionsService.list(queryWrapper);
        log.info("列表{}",transactions);
        // 统计每个分类的收入和支出
        // 收入
        Map<Long, BigDecimal> incomeMap = new HashMap<>();
        transactions.stream()
                .filter(t -> t.getType() == 0 && t.getCategoryId() != null && t.getAmount() != null)
                .forEach(t -> incomeMap.merge(t.getCategoryId(), t.getAmount(), BigDecimal::add));
        log.info("收入{}",incomeMap);
        // 支出
        Map<Long, BigDecimal> expenseMap = new HashMap<>();
        transactions.stream()
                .filter(t -> t.getType() == 1 && t.getCategoryId() != null && t.getAmount() != null)
                .forEach(t -> expenseMap.merge(t.getCategoryId(), t.getAmount(), BigDecimal::add));
        log.info("支出{}",expenseMap);
        // 构建返回结果
        CategoryRankResponse response = new CategoryRankResponse();
        response.setCategoryIncome(incomeMap);
        response.setCategoryExpense(expenseMap);
        return ResultVo.success(response);
    }

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
                item.setTime(createdAt.format(DateTimeFormatter.ofPattern("HH:mm")));
            } else if (transaction.getDate() != null) {
                item.setTime(LocalTime.of(0, 0).format(DateTimeFormatter.ofPattern("HH:mm")));
            } else {
                item.setTime("00:00");
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
