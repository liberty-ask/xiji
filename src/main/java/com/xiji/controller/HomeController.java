package com.xiji.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiji.common.response.ResultVo;
import com.xiji.entity.domain.Budget;
import com.xiji.entity.domain.Category;
import com.xiji.entity.domain.Transactions;
import com.xiji.entity.domain.User;
import com.xiji.entity.dto.response.HomeResponse;
import com.xiji.service.BudgetService;
import com.xiji.service.CategoryService;
import com.xiji.service.TransactionsService;
import com.xiji.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 首页统计
 * @author liberty
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

        // 按类型聚合本月收支（数据库内SUM，排除不计收支类型）
        QueryWrapper<Transactions> monthAggQuery = new QueryWrapper<>();
        monthAggQuery.select("type", "SUM(amount) AS total")
                .eq("family_id", familyId)
                .ge("date", firstDayOfMonth)
                .le("date", lastDayOfMonth)
                .in("type", 0, 1)
                .groupBy("type");
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        for (Map<String, Object> row : transactionsService.listMaps(monthAggQuery)) {
            Object typeObj = row.get("type");
            if (typeObj == null) {
                continue;
            }
            int type = ((Number) typeObj).intValue();
            if (type == 0) {
                totalIncome = toBigDecimal(row.get("total"));
            } else if (type == 1) {
                totalExpense = toBigDecimal(row.get("total"));
            }
        }

        // 计算余额（收入-支出）
        BigDecimal balance = totalIncome.subtract(totalExpense);

        // 今日/昨日支出（数据库内SUM）
        BigDecimal todayExpense = sumExpenseByDate(familyId, now);
        BigDecimal yesterdayExpense = sumExpenseByDate(familyId, now.minusDays(1));

        // 获取本月支出预算
        Budget budgetEntity = budgetService.getBudget(familyId);
        BigDecimal budgetTotal = budgetEntity != null && budgetEntity.getAmount() != null
                ? budgetEntity.getAmount()
                : BigDecimal.ZERO;

        // 构建响应
        HomeResponse response = new HomeResponse();
        response.setBalance(balance);
        response.setIncome(totalIncome);
        response.setExpense(totalExpense);
        response.setTodayExpense(todayExpense);
        response.setYesterdayExpense(yesterdayExpense);

        // 预算信息
        HomeResponse.BudgetInfo budgetInfo = new HomeResponse.BudgetInfo();
        budgetInfo.setUsed(totalExpense);
        budgetInfo.setTotal(budgetTotal);
        response.setBudget(budgetInfo);

        // 最近活动（最近10条交易记录，按日期和创建时间倒序）
        LambdaQueryWrapper<Transactions> recentQuery = new LambdaQueryWrapper<>();
        recentQuery.eq(Transactions::getFamilyId, familyId)
                .orderByDesc(Transactions::getDate)
                .orderByDesc(Transactions::getCreatedAt)
                .ne(Transactions::getType, 2)
                .last("LIMIT 10");
        List<Transactions> recentTransactions = transactionsService.list(recentQuery);

        // 批量查询相关分类和用户信息（避免循环点查）
        Set<Long> categoryIds = recentTransactions.stream()
                .map(Transactions::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Category> categoryMap = categoryIds.isEmpty() ? Map.of()
                : categoryService.listByIds(categoryIds).stream()
                    .collect(Collectors.toMap(Category::getId, c -> c));
        Set<Long> userIds = recentTransactions.stream()
                .map(Transactions::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : userService.listByIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));

        // 转换为活动项
        List<HomeResponse.ActivityItem> activities = recentTransactions.stream().map(transaction -> {
            HomeResponse.ActivityItem item = new HomeResponse.ActivityItem();
            // 标题：分类名 > 描述 > 默认（查询已排除type=2的不计收支交易）
            Category category = transaction.getCategoryId() != null
                    ? categoryMap.get(transaction.getCategoryId())
                    : null;
            if (category != null) {
                item.setTitle(category.getName());
                item.setIcon(category.getIcon() != null ? category.getIcon() : "payments");
            } else {
                item.setTitle(transaction.getDescription() != null ? transaction.getDescription() : "支出");
                item.setIcon("payments");
            }

            // 用户名
            User user = transaction.getCreatedBy() != null ? userMap.get(transaction.getCreatedBy()) : null;
            String userName;
            if (user != null && user.getName() != null) {
                userName = user.getName();
            } else if (user != null && user.getUsername() != null) {
                userName = user.getUsername();
            } else {
                userName = "未知用户";
            }
            item.setUser(userName);

            // 时间
            if (transaction.getCreatedAt() != null) {
                item.setTime(transaction.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            } else if (transaction.getDate() != null) {
                // 无创建时间时用交易日期当天的00:00
                item.setTime(transaction.getDate().atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            } else {
                item.setTime("00:00");
            }

            // 日期
            if (transaction.getDate() != null) {
                item.setDate(transaction.getDate().toString());
            }

            // 金额（数值，正负号由客户端根据isIncome拼接）
            item.setAmount(transaction.getAmount() != null ? transaction.getAmount() : BigDecimal.ZERO);

            // 是否收入
            item.setIsIncome(transaction.getType() != null && transaction.getType() == 0);

            // 交易对方
            if (transaction.getCounterparty() != null && !transaction.getCounterparty().isEmpty()) {
                item.setCounterparty(transaction.getCounterparty());
            }

            // 描述
            if (transaction.getDescription() != null && !transaction.getDescription().isEmpty()) {
                item.setDescription(transaction.getDescription());
            }

            return item;
        }).collect(Collectors.toList());

        response.setActivities(activities);

        return ResultVo.success(response);
    }

    /**
     * 按日期统计家庭支出总额（数据库内SUM）
     */
    private BigDecimal sumExpenseByDate(Long familyId, LocalDate date) {
        QueryWrapper<Transactions> query = new QueryWrapper<>();
        query.select("COALESCE(SUM(amount), 0) AS total")
                .eq("family_id", familyId)
                .eq("type", 1)
                .eq("date", date);
        List<Map<String, Object>> rows = transactionsService.listMaps(query);
        if (rows.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return toBigDecimal(rows.get(0).get("total"));
    }

    /**
     * 将聚合查询结果转为BigDecimal（兼容不同驱动的数值类型）
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }
}
