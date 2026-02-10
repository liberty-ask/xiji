package com.xiji.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiji.common.response.ResultVo;
import com.xiji.entity.domain.Category;
import com.xiji.entity.domain.FamilyMember;
import com.xiji.entity.domain.Transactions;
import com.xiji.entity.domain.User;
import com.xiji.entity.dto.response.*;
import com.xiji.service.CategoryService;
import com.xiji.service.FamilyMemberService;
import com.xiji.service.TransactionsService;
import com.xiji.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 手机端统计控制器
 * @author liberty
 */
@RestController
@RequestMapping("/api/v1/statistics")
@Slf4j
@RequiredArgsConstructor
public class MobileStatisticsController extends BaseController {

    private final TransactionsService transactionsService;
    private final CategoryService categoryService;
    private final UserService userService;
    private final FamilyMemberService familyMemberService;

    /**
     * 格式化总金额（带千分位，无小数，无¥符号）
     */
    private String formatTotalAmount(BigDecimal amount) {
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
     * 格式化金额（带¥符号，带千分位，无小数）
     */
    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "¥0";
        }
        NumberFormat formatter = NumberFormat.getInstance();
        formatter.setGroupingUsed(true);
        formatter.setMaximumFractionDigits(0);
        formatter.setMinimumFractionDigits(0);
        return "¥" + formatter.format(amount);
    }

    /**
     * 获取指定年月的日期范围
     */
    private DateRange getMonthRange(Integer year, Integer month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());
        return new DateRange(startDate, endDate);
    }

    /**
     * 获取上个月的日期范围
     */
    private DateRange getPreviousMonthRange(Integer year, Integer month) {
        LocalDate date = LocalDate.of(year, month, 1);
        LocalDate previousMonth = date.minusMonths(1);
        LocalDate startDate = previousMonth.withDayOfMonth(1);
        LocalDate endDate = previousMonth.with(TemporalAdjusters.lastDayOfMonth());
        return new DateRange(startDate, endDate);
    }

    /**
     * 根据 period 参数获取日期范围
     * @param period 时间维度：'year'、'month'、'week'
     * @param year 年份
     * @param month 月份（当 period='month' 时必传）
     * @param weekStartDate 周开始日期（当 period='week' 时必传）
     * @param weekEndDate 周结束日期（当 period='week' 时必传）
     * @return 日期范围
     */
    private DateRange getDateRangeByPeriod(String period, Integer year, Integer month, 
                                           String weekStartDate, String weekEndDate) {
        if ("year".equals(period)) {
            // 年维度：整年
            LocalDate startDate = LocalDate.of(year, 1, 1);
            LocalDate endDate = LocalDate.of(year, 12, 31);
            return new DateRange(startDate, endDate);
        } else if ("month".equals(period)) {
            // 月维度：指定月份
            if (month == null) {
                throw new IllegalArgumentException("period='month' 时必须提供 month 参数");
            }
            return getMonthRange(year, month);
        } else if ("week".equals(period)) {
            // 周维度：指定周
            if (weekStartDate == null || weekEndDate == null) {
                throw new IllegalArgumentException("period='week' 时必须提供 weekStartDate 和 weekEndDate 参数");
            }
            LocalDate start = LocalDate.parse(weekStartDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDate end = LocalDate.parse(weekEndDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return new DateRange(start, end);
        } else {
            throw new IllegalArgumentException("period 参数不正确，应为 'year'、'month' 或 'week'");
        }
    }

    /**
     * 获取上一个周期的日期范围（用于对比）
     * @param period 时间维度：'year'、'month'、'week'
     * @param year 年份
     * @param month 月份（当 period='month' 时必传）
     * @param weekStartDate 周开始日期（当 period='week' 时必传）
     * @param weekEndDate 周结束日期（当 period='week' 时必传）
     * @return 上一个周期的日期范围
     */
    private DateRange getPreviousPeriodRange(String period, Integer year, Integer month,
                                            String weekStartDate, String weekEndDate) {
        if ("year".equals(period)) {
            // 上一年
            LocalDate startDate = LocalDate.of(year - 1, 1, 1);
            LocalDate endDate = LocalDate.of(year - 1, 12, 31);
            return new DateRange(startDate, endDate);
        } else if ("month".equals(period)) {
            // 上个月
            return getPreviousMonthRange(year, month);
        } else if ("week".equals(period)) {
            // 上一周
            if (weekStartDate == null || weekEndDate == null) {
                throw new IllegalArgumentException("period='week' 时必须提供 weekStartDate 和 weekEndDate 参数");
            }
            LocalDate start = LocalDate.parse(weekStartDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDate end = LocalDate.parse(weekEndDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
            LocalDate previousStart = start.minusDays(days);
            LocalDate previousEnd = start.minusDays(1);
            return new DateRange(previousStart, previousEnd);
        } else {
            throw new IllegalArgumentException("period 参数不正确，应为 'year'、'month' 或 'week'");
        }
    }

    /**
     * 日期范围内部类
     */
    private static class DateRange {
        LocalDate start;
        LocalDate end;

        DateRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }
    }

    /**
     * 计算指定日期范围内的总金额
     */
    private BigDecimal calculateTotalAmount(Long familyId, Integer transactionType, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Transactions> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Transactions::getFamilyId, familyId)
                .eq(Transactions::getType, transactionType)
                .ge(Transactions::getDate, startDate)
                .le(Transactions::getDate, endDate);

        List<Transactions> transactions = transactionsService.list(queryWrapper);
        return transactions.stream()
                .map(Transactions::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 计算变化百分比
     */
    private String calculateChangePercentage(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            if (current == null || current.compareTo(BigDecimal.ZERO) == 0) {
                return "0%";
            } else {
                return "+100%";
            }
        }

        BigDecimal change = current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        DecimalFormat df = new DecimalFormat("#");
        String sign = change.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + df.format(change) + "%";
    }

    /**
     * 接口2：获取分类统计数据
     * GET /api/v1/statistics
     * @param type 交易类型（0-收入，1-支出）
     * @param year 年份
     * @param month 月份（当period='month'时必传）
     * @param period 时间维度：'year'、'month'、'week'
     * @param weekStartDate 周开始日期（当period='week'时必传）
     * @param weekEndDate 周结束日期（当period='week'时必传）
     * @param memberId 成员ID（可选）
     * @param limit 返回Top N分类（可选）
     */
    @GetMapping
    public ResultVo getStatistics(@RequestParam Integer type,
                                  @RequestParam Integer year,
                                  @RequestParam(required = false) Integer month,
                                  @RequestParam String period,
                                  @RequestParam(required = false) String weekStartDate,
                                  @RequestParam(required = false) String weekEndDate,
                                  @RequestParam(required = false) String memberId,
                                  @RequestParam(required = false) Integer limit,
                                  HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        Long familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        // 验证type参数
        if (type == null || (type != 0 && type != 1)) {
            return ResultVo.error("type参数不正确，应为0（收入）或1（支出）");
        }

        // 验证year参数
        if (year == null || year < 2000 || year > 3000) {
            return ResultVo.error("year参数不正确，应为2000-3000之间的年份");
        }

        // 验证period参数
        if (period == null || !Arrays.asList("year", "month", "week").contains(period)) {
            return ResultVo.error("period参数不正确，应为'year'、'month'或'week'");
        }

        // 验证参数组合
        if ("year".equals(period)) {
            if (month != null) {
                return ResultVo.error("period='year'时不应传month参数");
            }
            if (weekStartDate != null || weekEndDate != null) {
                return ResultVo.error("period='year'时不应传weekStartDate和weekEndDate参数");
            }
        } else if ("month".equals(period)) {
            if (month == null || month < 1 || month > 12) {
                return ResultVo.error("period='month'时必须提供month参数（1-12）");
            }
            if (weekStartDate != null || weekEndDate != null) {
                return ResultVo.error("period='month'时不应传weekStartDate和weekEndDate参数");
            }
        } else if ("week".equals(period)) {
            if (month != null) {
                return ResultVo.error("period='week'时不应传month参数");
            }
            if (weekStartDate == null || weekEndDate == null) {
                return ResultVo.error("period='week'时必须提供weekStartDate和weekEndDate参数");
            }
            // 验证日期格式
            try {
                LocalDate.parse(weekStartDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                LocalDate.parse(weekEndDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                return ResultVo.error("weekStartDate和weekEndDate格式不正确，应为yyyy-MM-dd");
            }
        }

        // 转换 memberId
        Long memberIdLong = null;
        if (memberId != null && !memberId.isEmpty()) {
            try {
                memberIdLong = Long.parseLong(memberId);
            } catch (NumberFormatException e) {
                return ResultVo.error("memberId格式不正确");
            }
        }

        try {
            // 获取当前周期的日期范围
            DateRange currentRange = getDateRangeByPeriod(period, year, month, weekStartDate, weekEndDate);
            DateRange previousRange = getPreviousPeriodRange(period, year, month, weekStartDate, weekEndDate);

            // 查询当前期的交易
            List<Transactions> transactions = getTransactions(familyId, type, currentRange.start, currentRange.end, memberIdLong);

            // 按分类统计金额
            Map<Long, BigDecimal> categoryAmountMap = new HashMap<>();
            for (Transactions transaction : transactions) {
                Long categoryId = transaction.getCategoryId();
                BigDecimal amount = transaction.getAmount();
                if (categoryId != null && amount != null) {
                    categoryAmountMap.merge(categoryId, amount, BigDecimal::add);
                }
            }

            // 计算当前期总金额
            BigDecimal currentTotal = categoryAmountMap.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 计算上期总金额（用于计算变化）
            BigDecimal previousTotal = calculateTotalAmount(familyId, type,
                    previousRange.start, previousRange.end);

            // 计算变化百分比
            String change = calculateChangePercentage(currentTotal, previousTotal);

            // 构建分类统计列表
            List<MobileStatisticsResponse.CategoryStatisticsItem> items = new ArrayList<>();
            String[] colors = {"#13ec5b", "#fbbf24", "#3b82f6", "#ef4444", "#8b5cf6", "#ec4899", "#06b6d4", "#84cc16"};
            int colorIndex = 0;

            for (Map.Entry<Long, BigDecimal> entry : categoryAmountMap.entrySet()) {
                Category category = categoryService.getById(entry.getKey());
                if (category != null) {
                    MobileStatisticsResponse.CategoryStatisticsItem item = 
                        new MobileStatisticsResponse.CategoryStatisticsItem();
                    item.setName(category.getName());
                    item.setAmount(formatAmount(entry.getValue()));
                    
                    // 计算占比
                    BigDecimal percentage = currentTotal.compareTo(BigDecimal.ZERO) == 0 
                        ? BigDecimal.ZERO 
                        : entry.getValue()
                            .divide(currentTotal, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));
                    DecimalFormat df = new DecimalFormat("#");
                    item.setPct(df.format(percentage) + "%");
                    
                    item.setColor(colors[colorIndex % colors.length]);
                    item.setIcon(category.getIcon() != null ? category.getIcon() : "payments");
                    
                    items.add(item);
                    colorIndex++;
                }
            }

            // 按金额排序（降序）
            items.sort((a, b) -> {
                String amountA = a.getAmount().replaceAll("[¥,]", "");
                String amountB = b.getAmount().replaceAll("[¥,]", "");
                return new BigDecimal(amountB).compareTo(new BigDecimal(amountA));
            });

            // 限制返回数量
            if (limit != null && limit > 0 && items.size() > limit) {
                items = items.subList(0, limit);
            }

            // 构建响应
            MobileStatisticsResponse response = new MobileStatisticsResponse();
            response.setTotal(formatTotalAmount(currentTotal));
            response.setChange(change);
            response.setItems(items);

            return ResultVo.success(response);
        } catch (IllegalArgumentException e) {
            return ResultVo.error(e.getMessage());
        }
    }

    /**
     * 计算变化率（BigDecimal格式）
     */
    private BigDecimal calculateChangeRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            if (current == null || current.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            } else {
                return new BigDecimal("100");
            }
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * 获取指定日期范围内的交易列表
     */
    private List<Transactions> getTransactions(Long familyId, Integer type, LocalDate startDate, LocalDate endDate, Long memberId) {
        LambdaQueryWrapper<Transactions> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Transactions::getFamilyId, familyId)
                .ge(Transactions::getDate, startDate)
                .le(Transactions::getDate, endDate);
        
        if (type != null) {
            queryWrapper.eq(Transactions::getType, type);
        } else {
            // 当type为null时，只查询收入(0)和支出(1)类型的交易，排除不计收支类型(2)的记录
            queryWrapper.in(Transactions::getType, 0, 1);
        }
        
        // 通过createdBy字段过滤成员（createdBy表示创建该交易记录的用户，通常是记录交易的成员）
        if (memberId != null) {
            queryWrapper.eq(Transactions::getCreatedBy, memberId);
        }
        
        return transactionsService.list(queryWrapper);
    }

    /**
     * 接口1：获取统计概览数据
     * GET /api/v1/statistics/overview
     */
    @GetMapping("/overview")
    public ResultVo getOverview(@RequestParam Integer year,
                                 @RequestParam(required = false) Integer month,
                                 @RequestParam String period,
                                 @RequestParam(required = false) String weekStartDate,
                                 @RequestParam(required = false) String weekEndDate,
                                 @RequestParam(required = false) String memberId,
                                 HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        Long familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        // 验证参数
        if (year == null || year < 2000 || year > 3000) {
            return ResultVo.error("year参数不正确，应为2000-3000之间的年份");
        }
        if (period == null || !Arrays.asList("year", "month", "week").contains(period)) {
            return ResultVo.error("period参数不正确，应为'year'、'month'或'week'");
        }

        // 验证参数组合
        if ("year".equals(period)) {
            if (month != null) {
                return ResultVo.error("period='year'时不应传month参数");
            }
            if (weekStartDate != null || weekEndDate != null) {
                return ResultVo.error("period='year'时不应传weekStartDate和weekEndDate参数");
            }
        } else if ("month".equals(period)) {
            if (month == null || month < 1 || month > 12) {
                return ResultVo.error("period='month'时必须提供month参数（1-12）");
            }
            if (weekStartDate != null || weekEndDate != null) {
                return ResultVo.error("period='month'时不应传weekStartDate和weekEndDate参数");
            }
        } else if ("week".equals(period)) {
            if (month != null) {
                return ResultVo.error("period='week'时不应传month参数");
            }
            if (weekStartDate == null || weekEndDate == null) {
                return ResultVo.error("period='week'时必须提供weekStartDate和weekEndDate参数");
            }
            // 验证日期格式
            try {
                LocalDate.parse(weekStartDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                LocalDate.parse(weekEndDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                return ResultVo.error("weekStartDate和weekEndDate格式不正确，应为yyyy-MM-dd");
            }
        }

        // 转换 memberId
        Long memberIdLong = null;
        if (memberId != null && !memberId.isEmpty()) {
            try {
                memberIdLong = Long.parseLong(memberId);
            } catch (NumberFormatException e) {
                return ResultVo.error("memberId格式不正确");
            }
        }

        try {
            // 获取当前周期和上一周期的日期范围
            DateRange currentRange = getDateRangeByPeriod(period, year, month, weekStartDate, weekEndDate);
            DateRange previousRange = getPreviousPeriodRange(period, year, month, weekStartDate, weekEndDate);

            // 计算当前期收入、支出
            BigDecimal currentIncome = calculateTotalAmount(familyId, 0, currentRange.start, currentRange.end);
            BigDecimal currentExpense = calculateTotalAmount(familyId, 1, currentRange.start, currentRange.end);
            BigDecimal currentNet = currentIncome.subtract(currentExpense);

            // 计算上期收入、支出
            BigDecimal previousIncome = calculateTotalAmount(familyId, 0, previousRange.start, previousRange.end);
            BigDecimal previousExpense = calculateTotalAmount(familyId, 1, previousRange.start, previousRange.end);

            // 计算变化率
            BigDecimal incomeChange = calculateChangeRate(currentIncome, previousIncome);
            BigDecimal expenseChange = calculateChangeRate(currentExpense, previousExpense);

            // 计算交易笔数
            List<Transactions> transactions = getTransactions(familyId, null, currentRange.start, currentRange.end, memberIdLong);
            int transactionCount = transactions.size();

            StatisticsOverviewResponse response = new StatisticsOverviewResponse();
            response.setTotalIncome(currentIncome);
            response.setTotalExpense(currentExpense);
            response.setNetIncome(currentNet);
            response.setIncomeChange(incomeChange);
            response.setExpenseChange(expenseChange);
            // 移除日均字段（文档中未要求）
            response.setTransactionCount(transactionCount);

            return ResultVo.success(response);
        } catch (IllegalArgumentException e) {
            return ResultVo.error(e.getMessage());
        }
    }

    /**
     * 接口2：获取分类统计
     * GET /api/v1/statistics/by-category
     */
    @GetMapping("/by-category")
    public ResultVo getByCategory(@RequestParam Integer type,
                                   @RequestParam Integer year,
                                   @RequestParam Integer month,
                                   @RequestParam(required = false) Long memberId,
                                   @RequestParam(required = false) Integer limit,
                                   HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        Long familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        // 验证参数
        if (type == null || (type != 0 && type != 1)) {
            return ResultVo.error("type参数不正确，应为0（收入）或1（支出）");
        }
        if (year == null || year < 2000 || year > 3000) {
            return ResultVo.error("year参数不正确，应为2000-3000之间的年份");
        }
        if (month == null || month < 1 || month > 12) {
            return ResultVo.error("month参数不正确，应为1-12之间的月份");
        }

        DateRange currentRange = getMonthRange(year, month);
        List<Transactions> transactions = getTransactions(familyId, type, currentRange.start, currentRange.end, memberId);

        // 按分类统计金额和笔数
        Map<Long, BigDecimal> categoryAmountMap = new HashMap<>();
        Map<Long, Integer> categoryCountMap = new HashMap<>();
        for (Transactions transaction : transactions) {
            Long categoryId = transaction.getCategoryId();
            BigDecimal amount = transaction.getAmount();
            if (categoryId != null && amount != null) {
                categoryAmountMap.merge(categoryId, amount, BigDecimal::add);
                categoryCountMap.merge(categoryId, 1, Integer::sum);
            }
        }

        // 计算总金额
        BigDecimal total = categoryAmountMap.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 构建分类统计列表
        List<StatisticsByCategoryResponse.CategoryStatisticsItem> items = new ArrayList<>();
        String[] colors = {"#13ec5b", "#fbbf24", "#3b82f6", "#ef4444", "#8b5cf6", "#ec4899", "#06b6d4", "#84cc16"};
        int colorIndex = 0;

        for (Map.Entry<Long, BigDecimal> entry : categoryAmountMap.entrySet()) {
            Category category = categoryService.getById(entry.getKey());
            if (category != null) {
                StatisticsByCategoryResponse.CategoryStatisticsItem item = 
                    new StatisticsByCategoryResponse.CategoryStatisticsItem();
                item.setCategoryId(String.valueOf(category.getId()));
                item.setCategoryName(category.getName());
                item.setIcon(category.getIcon() != null ? category.getIcon() : "payments");
                item.setColor(colors[colorIndex % colors.length]);
                item.setAmount(entry.getValue());
                item.setAmountStr(formatAmount(entry.getValue()));
                
                // 计算占比
                BigDecimal percentage = total.compareTo(BigDecimal.ZERO) == 0 
                    ? BigDecimal.ZERO 
                    : entry.getValue()
                        .divide(total, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                item.setPercentage(percentage);
                DecimalFormat df = new DecimalFormat("#.##");
                item.setPercentageStr(df.format(percentage) + "%");
                
                item.setCount(categoryCountMap.getOrDefault(entry.getKey(), 0));
                
                items.add(item);
                colorIndex++;
            }
        }

        // 按金额排序（降序）
        items.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));

        // 限制返回数量
        if (limit != null && limit > 0 && items.size() > limit) {
            items = items.subList(0, limit);
        }

        StatisticsByCategoryResponse response = new StatisticsByCategoryResponse();
        response.setTotal(formatTotalAmount(total));
        response.setItems(items);

        return ResultVo.success(response);
    }

    /**
     * 接口3：获取时间趋势统计
     * GET /api/v1/statistics/trend
     */
    @GetMapping("/trend")
    public ResultVo getTrend(@RequestParam(required = false) Integer type,
                              @RequestParam Integer year,
                              @RequestParam(required = false) Integer month,
                              @RequestParam String period,
                              @RequestParam(required = false) String weekStartDate,
                              @RequestParam(required = false) String weekEndDate,
                              @RequestParam(required = false) String memberId,
                              HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        Long familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        // 验证参数
        if (type != null && type != 0 && type != 1) {
            return ResultVo.error("type参数不正确，应为0（收入）或1（支出）或null（全部）");
        }
        if (year == null || year < 2000 || year > 3000) {
            return ResultVo.error("year参数不正确，应为2000-3000之间的年份");
        }
        if (!Arrays.asList("day", "week", "month", "year").contains(period)) {
            return ResultVo.error("period参数不正确，应为day、week、month或year");
        }

        // 转换 memberId
        Long memberIdLong = null;
        if (memberId != null && !memberId.isEmpty()) {
            try {
                memberIdLong = Long.parseLong(memberId);
            } catch (NumberFormatException e) {
                return ResultVo.error("memberId格式不正确");
            }
        }

        List<StatisticsTrendResponse.TrendItem> items = new ArrayList<>();

        try {
            if ("year".equals(period)) {
                // 当用户选择"年"维度，返回按月份聚合的数据
                if (month != null) {
                    return ResultVo.error("period='year'时不应传month参数");
                }
                if (weekStartDate != null || weekEndDate != null) {
                    return ResultVo.error("period='year'时不应传weekStartDate和weekEndDate参数");
                }
                // 生成12个月的数据
                for (int m = 1; m <= 12; m++) {
                    LocalDate monthStart = LocalDate.of(year, m, 1);
                    LocalDate monthEnd = monthStart.with(TemporalAdjusters.lastDayOfMonth());
                    StatisticsTrendResponse.TrendItem item = calculateTrendItem(
                        familyId, type, monthStart, monthEnd, memberIdLong, 
                        year + "-" + String.format("%02d", m), 
                        year + "年" + m + "月", items.isEmpty() ? null : items.get(items.size() - 1));
                    items.add(item);
                }
            } else if ("month".equals(period)) {
                // 当用户选择"月"维度，返回按天聚合的数据
                if (month == null || month < 1 || month > 12) {
                    return ResultVo.error("period='month'时必须提供month参数（1-12）");
                }
                if (weekStartDate != null || weekEndDate != null) {
                    return ResultVo.error("period='month'时不应传weekStartDate和weekEndDate参数");
                }
                LocalDate startDate = LocalDate.of(year, month, 1);
                LocalDate endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());
                int days = endDate.getDayOfMonth();
                for (int d = 1; d <= days; d++) {
                    LocalDate dayDate = LocalDate.of(year, month, d);
                    StatisticsTrendResponse.TrendItem item = calculateTrendItem(
                        familyId, type, dayDate, dayDate, memberIdLong,
                        year + "-" + String.format("%02d", month) + "-" + String.format("%02d", d),
                        month + "月" + d + "日", items.isEmpty() ? null : items.get(items.size() - 1));
                    items.add(item);
                }
            } else if ("day".equals(period)) {
                // 当用户选择"周"维度，period='day'，返回按天聚合的数据
                if (month != null) {
                    return ResultVo.error("period='day'（周维度）时不应传month参数");
                }
                if (weekStartDate == null || weekEndDate == null) {
                    return ResultVo.error("period='day'（周维度）时必须提供weekStartDate和weekEndDate参数");
                }
                // 验证日期格式
                LocalDate start;
                LocalDate end;
                try {
                    start = LocalDate.parse(weekStartDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    end = LocalDate.parse(weekEndDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } catch (Exception e) {
                    return ResultVo.error("weekStartDate和weekEndDate格式不正确，应为yyyy-MM-dd");
                }
                // 生成周内每天的数据
                LocalDate currentDate = start;
                while (!currentDate.isAfter(end)) {
                    StatisticsTrendResponse.TrendItem item = calculateTrendItem(
                        familyId, type, currentDate, currentDate, memberIdLong,
                        currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        currentDate.getMonthValue() + "月" + currentDate.getDayOfMonth() + "日",
                        items.isEmpty() ? null : items.get(items.size() - 1));
                    items.add(item);
                    currentDate = currentDate.plusDays(1);
                }
            } else {
                return ResultVo.error("period参数不正确，应为year、month或day");
            }

            // 计算汇总信息
            StatisticsTrendResponse.Summary summary = new StatisticsTrendResponse.Summary();
            BigDecimal totalIncome = items.stream()
                    .map(StatisticsTrendResponse.TrendItem::getIncome)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalExpense = items.stream()
                    .map(StatisticsTrendResponse.TrendItem::getExpense)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal maxIncome = items.stream()
                    .map(StatisticsTrendResponse.TrendItem::getIncome)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            BigDecimal maxExpense = items.stream()
                    .map(StatisticsTrendResponse.TrendItem::getExpense)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            summary.setTotalIncome(totalIncome);
            summary.setTotalExpense(totalExpense);
            summary.setMaxIncome(maxIncome);
            summary.setMaxExpense(maxExpense);
            summary.setAvgIncome(items.isEmpty() ? BigDecimal.ZERO : totalIncome.divide(new BigDecimal(items.size()), 2, RoundingMode.HALF_UP));
            summary.setAvgExpense(items.isEmpty() ? BigDecimal.ZERO : totalExpense.divide(new BigDecimal(items.size()), 2, RoundingMode.HALF_UP));

            StatisticsTrendResponse response = new StatisticsTrendResponse();
            response.setPeriod(period);
            response.setItems(items);
            response.setSummary(summary);

            return ResultVo.success(response);
        } catch (IllegalArgumentException e) {
            return ResultVo.error(e.getMessage());
        }
    }

    /**
     * 计算趋势项
     */
    private StatisticsTrendResponse.TrendItem calculateTrendItem(Long familyId, Integer type, 
                                                                  LocalDate startDate, LocalDate endDate,
                                                                  Long memberId, String date, String dateLabel,
                                                                  StatisticsTrendResponse.TrendItem previousItem) {
        BigDecimal income = type == null || type == 0 ? calculateTotalAmount(familyId, 0, startDate, endDate) : BigDecimal.ZERO;
        BigDecimal expense = type == null || type == 1 ? calculateTotalAmount(familyId, 1, startDate, endDate) : BigDecimal.ZERO;
        BigDecimal net = income.subtract(expense);

        StatisticsTrendResponse.TrendItem item = new StatisticsTrendResponse.TrendItem();
        item.setDate(date);
        item.setDateLabel(dateLabel);
        item.setIncome(income);
        item.setExpense(expense);
        item.setNet(net);

        if (previousItem != null) {
            item.setIncomeChange(calculateChangeRate(income, previousItem.getIncome()));
            item.setExpenseChange(calculateChangeRate(expense, previousItem.getExpense()));
        }

        return item;
    }

    /**
     * 接口4：获取成员统计
     * GET /api/v1/statistics/by-member
     */
    @GetMapping("/by-member")
    public ResultVo getByMember(@RequestParam Integer type,
                                 @RequestParam Integer year,
                                 @RequestParam(required = false) Integer month,
                                 @RequestParam String period,
                                 @RequestParam(required = false) String weekStartDate,
                                 @RequestParam(required = false) String weekEndDate,
                                 @RequestParam(required = false) String categoryId,
                                 HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        Long familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        // 验证参数
        if (type == null || (type != 0 && type != 1)) {
            return ResultVo.error("type参数不正确，应为0（收入）或1（支出）");
        }
        if (year == null || year < 2000 || year > 3000) {
            return ResultVo.error("year参数不正确，应为2000-3000之间的年份");
        }
        if (period == null || !Arrays.asList("year", "month", "week").contains(period)) {
            return ResultVo.error("period参数不正确，应为'year'、'month'或'week'");
        }

        // 验证参数组合
        if ("year".equals(period)) {
            if (month != null) {
                return ResultVo.error("period='year'时不应传month参数");
            }
            if (weekStartDate != null || weekEndDate != null) {
                return ResultVo.error("period='year'时不应传weekStartDate和weekEndDate参数");
            }
        } else if ("month".equals(period)) {
            if (month == null || month < 1 || month > 12) {
                return ResultVo.error("period='month'时必须提供month参数（1-12）");
            }
            if (weekStartDate != null || weekEndDate != null) {
                return ResultVo.error("period='month'时不应传weekStartDate和weekEndDate参数");
            }
        } else if ("week".equals(period)) {
            if (month != null) {
                return ResultVo.error("period='week'时不应传month参数");
            }
            if (weekStartDate == null || weekEndDate == null) {
                return ResultVo.error("period='week'时必须提供weekStartDate和weekEndDate参数");
            }
            // 验证日期格式
            try {
                LocalDate.parse(weekStartDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                LocalDate.parse(weekEndDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                return ResultVo.error("weekStartDate和weekEndDate格式不正确，应为yyyy-MM-dd");
            }
        }

        // 转换 categoryId
        Long categoryIdLong = null;
        if (categoryId != null && !categoryId.isEmpty()) {
            try {
                categoryIdLong = Long.parseLong(categoryId);
            } catch (NumberFormatException e) {
                return ResultVo.error("categoryId格式不正确");
            }
        }

        try {
            DateRange currentRange = getDateRangeByPeriod(period, year, month, weekStartDate, weekEndDate);
            List<Transactions> transactions = getTransactions(familyId, type, currentRange.start, currentRange.end, null);

            // 如果指定了分类，过滤交易
            final Long finalCategoryId = categoryIdLong;
            if (finalCategoryId != null) {
                transactions = transactions.stream()
                        .filter(t -> finalCategoryId.equals(t.getCategoryId()))
                        .collect(Collectors.toList());
            }

            // 获取家庭成员
            List<FamilyMember> members = familyMemberService.list(
                new LambdaQueryWrapper<FamilyMember>()
                    .eq(FamilyMember::getFamilyId, familyId));
            List<Long> memberUserIds = members.stream()
                    .map(FamilyMember::getUserId)
                    .collect(Collectors.toList());
            Map<Long, User> userMap = userService.listByIds(memberUserIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));

            // 按成员统计金额和笔数
            // 注意：Transactions表中没有memberId字段，这里假设通过其他方式关联
            // 如果Transactions表有createdBy字段，可以使用它来关联成员
            Map<Long, BigDecimal> memberAmountMap = new HashMap<>();
            Map<Long, Integer> memberCountMap = new HashMap<>();
            
            // 由于Transactions表可能没有直接关联成员，这里需要根据实际情况调整
            // 假设通过createdBy字段关联（如果存在）
            for (Transactions transaction : transactions) {
                // 这里需要根据实际表结构调整，暂时使用createdBy
                Long memberUserId = transaction.getCreatedBy();
                if (memberUserId != null && memberUserIds.contains(memberUserId)) {
                    BigDecimal amount = transaction.getAmount();
                    if (amount != null) {
                        memberAmountMap.merge(memberUserId, amount, BigDecimal::add);
                        memberCountMap.merge(memberUserId, 1, Integer::sum);
                    }
                }
            }

            // 计算总金额
            BigDecimal total = memberAmountMap.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 构建成员统计列表
            List<StatisticsByMemberResponse.MemberStatisticsItem> items = new ArrayList<>();
            for (Map.Entry<Long, BigDecimal> entry : memberAmountMap.entrySet()) {
                User user = userMap.get(entry.getKey());
                if (user != null) {
                    StatisticsByMemberResponse.MemberStatisticsItem item = 
                        new StatisticsByMemberResponse.MemberStatisticsItem();
                    item.setMemberId(String.valueOf(user.getId()));
                    item.setMemberName(user.getName() != null ? user.getName() : user.getUsername());
                    item.setAvatar(user.getAvatar());
                    item.setAmount(entry.getValue());
                    item.setAmountStr(formatAmount(entry.getValue()));
                    
                    // 计算占比
                    BigDecimal percentage = total.compareTo(BigDecimal.ZERO) == 0 
                        ? BigDecimal.ZERO 
                        : entry.getValue()
                            .divide(total, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));
                    item.setPercentage(percentage);
                    DecimalFormat df = new DecimalFormat("#.##");
                    item.setPercentageStr(df.format(percentage) + "%");
                    
                    int count = memberCountMap.getOrDefault(entry.getKey(), 0);
                    item.setCount(count);
                    item.setAvgAmount(count > 0 ? entry.getValue().divide(new BigDecimal(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
                    
                    items.add(item);
                }
            }

            // 按金额排序（降序）
            items.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));

            StatisticsByMemberResponse response = new StatisticsByMemberResponse();
            response.setTotal(formatTotalAmount(total));
            response.setItems(items);

            return ResultVo.success(response);
        } catch (IllegalArgumentException e) {
            return ResultVo.error(e.getMessage());
        }
    }

    /**
     * 接口5：获取日期统计（日历视图）
     * GET /api/v1/statistics/by-date
     */
    @GetMapping("/by-date")
    public ResultVo getByDate(@RequestParam(required = false) Integer type,
                               @RequestParam Integer year,
                               @RequestParam(required = false) Integer month,
                               @RequestParam String period,
                               @RequestParam(required = false) String weekStartDate,
                               @RequestParam(required = false) String weekEndDate,
                               @RequestParam(required = false) String memberId,
                               HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        Long familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        // 验证参数
        if (type != null && type != 0 && type != 1) {
            return ResultVo.error("type参数不正确，应为0（收入）或1（支出）或null（全部）");
        }
        if (year == null || year < 2000 || year > 3000) {
            return ResultVo.error("year参数不正确，应为2000-3000之间的年份");
        }
        if (period == null || !Arrays.asList("year", "month", "week").contains(period)) {
            return ResultVo.error("period参数不正确，应为'year'、'month'或'week'");
        }

        // 验证参数组合
        if ("year".equals(period)) {
            if (month != null) {
                return ResultVo.error("period='year'时不应传month参数");
            }
            if (weekStartDate != null || weekEndDate != null) {
                return ResultVo.error("period='year'时不应传weekStartDate和weekEndDate参数");
            }
        } else if ("month".equals(period)) {
            if (month == null || month < 1 || month > 12) {
                return ResultVo.error("period='month'时必须提供month参数（1-12）");
            }
            if (weekStartDate != null || weekEndDate != null) {
                return ResultVo.error("period='month'时不应传weekStartDate和weekEndDate参数");
            }
        } else if ("week".equals(period)) {
            if (month != null) {
                return ResultVo.error("period='week'时不应传month参数");
            }
            if (weekStartDate == null || weekEndDate == null) {
                return ResultVo.error("period='week'时必须提供weekStartDate和weekEndDate参数");
            }
            // 验证日期格式
            try {
                LocalDate.parse(weekStartDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                LocalDate.parse(weekEndDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                return ResultVo.error("weekStartDate和weekEndDate格式不正确，应为yyyy-MM-dd");
            }
        }

        // 转换 memberId
        Long memberIdLong = null;
        if (memberId != null && !memberId.isEmpty()) {
            try {
                memberIdLong = Long.parseLong(memberId);
            } catch (NumberFormatException e) {
                return ResultVo.error("memberId格式不正确");
            }
        }

        try {
            DateRange dateRange = getDateRangeByPeriod(period, year, month, weekStartDate, weekEndDate);
            LocalDate startDate = dateRange.start;
            LocalDate endDate = dateRange.end;
            
            // 计算日期范围
            List<LocalDate> dateList = new ArrayList<>();
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                dateList.add(currentDate);
                currentDate = currentDate.plusDays(1);
            }

            // 获取所有交易
            List<Transactions> allTransactions = getTransactions(familyId, type, startDate, endDate, memberIdLong);

            // 按日期分组
            Map<LocalDate, List<Transactions>> transactionsByDate = allTransactions.stream()
                    .collect(Collectors.groupingBy(Transactions::getDate));

            // 计算最大金额（用于热力值计算）
            BigDecimal maxAmount = allTransactions.stream()
                    .map(Transactions::getAmount)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            List<StatisticsByDateResponse.DateStatisticsItem> days = new ArrayList<>();
            LocalDate maxDate = null;
            BigDecimal maxDayAmount = BigDecimal.ZERO;

            // 遍历日期范围内的每一天
            for (LocalDate date : dateList) {
                List<Transactions> dayTransactions = transactionsByDate.getOrDefault(date, new ArrayList<>());
                
                BigDecimal income = dayTransactions.stream()
                        .filter(t -> t.getType() == 0)
                        .map(Transactions::getAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal expense = dayTransactions.stream()
                        .filter(t -> t.getType() == 1)
                        .map(Transactions::getAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal net = income.subtract(expense);
                BigDecimal dayTotal = income.add(expense);
                
                // 计算热力值（1-5）
                int intensity = 1;
                if (maxAmount.compareTo(BigDecimal.ZERO) > 0) {
                    double ratio = dayTotal.divide(maxAmount, 4, RoundingMode.HALF_UP).doubleValue();
                    intensity = (int) Math.min(5, Math.max(1, Math.ceil(ratio * 5)));
                }

                StatisticsByDateResponse.DateStatisticsItem item = 
                    new StatisticsByDateResponse.DateStatisticsItem();
                item.setDate(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                item.setDay(date.getDayOfMonth());
                item.setIncome(income);
                item.setExpense(expense);
                item.setNet(net);
                item.setCount(dayTransactions.size());
                item.setIntensity(intensity);

                days.add(item);

                // 记录最大金额日期
                if (dayTotal.compareTo(maxDayAmount) > 0) {
                    maxDayAmount = dayTotal;
                    maxDate = date;
                }
            }

            StatisticsByDateResponse.DateSummary summary = new StatisticsByDateResponse.DateSummary();
            summary.setMaxAmount(maxDayAmount);
            summary.setMaxDate(maxDate != null ? maxDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null);

            StatisticsByDateResponse response = new StatisticsByDateResponse();
            response.setYear(year);
            if (month != null) {
                response.setMonth(month);
            }
            response.setDays(days);
            response.setSummary(summary);

            return ResultVo.success(response);
        } catch (IllegalArgumentException e) {
            return ResultVo.error(e.getMessage());
        }
    }

    /**
     * 接口6：获取对比数据
     * GET /api/v1/statistics/compare
     * 
     * 注意：由于Spring的@RequestParam不能直接接收对象，这里分别接收period1和period2的各个字段
     */
    @GetMapping("/compare")
    public ResultVo getCompare(@RequestParam String type,
                                @RequestParam String comparisonType,
                                // period1 参数
                                @RequestParam Integer period1Year,
                                @RequestParam(required = false) Integer period1Month,
                                @RequestParam String period1Period,
                                @RequestParam(required = false) String period1WeekStartDate,
                                @RequestParam(required = false) String period1WeekEndDate,
                                @RequestParam(required = false) String period1MemberId,
                                // period2 参数
                                @RequestParam Integer period2Year,
                                @RequestParam(required = false) Integer period2Month,
                                @RequestParam String period2Period,
                                @RequestParam(required = false) String period2WeekStartDate,
                                @RequestParam(required = false) String period2WeekEndDate,
                                @RequestParam(required = false) String period2MemberId,
                                @RequestParam(required = false) List<String> memberIds,
                                @RequestParam(required = false) Integer transactionType,
                                HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return ResultVo.error("用户未登录");
        }

        Long familyId = getCurrentFamilyId(userId);
        if (familyId == null) {
            return ResultVo.error("请先选择家庭");
        }

        // 验证参数
        if (!Arrays.asList("period", "member").contains(type)) {
            return ResultVo.error("type参数不正确，应为period或member");
        }
        if (!Arrays.asList("month", "year").contains(comparisonType)) {
            return ResultVo.error("comparisonType参数不正确，应为month或year");
        }

        try {
            // 验证 period1 参数
            if (!Arrays.asList("year", "month", "week").contains(period1Period)) {
                return ResultVo.error("period1Period参数不正确，应为'year'、'month'或'week'");
            }
            if ("month".equals(period1Period) && period1Month == null) {
                return ResultVo.error("period1Period='month'时必须提供period1Month参数");
            }
            if ("week".equals(period1Period) && (period1WeekStartDate == null || period1WeekEndDate == null)) {
                return ResultVo.error("period1Period='week'时必须提供period1WeekStartDate和period1WeekEndDate参数");
            }

            // 验证 period2 参数
            if (!Arrays.asList("year", "month", "week").contains(period2Period)) {
                return ResultVo.error("period2Period参数不正确，应为'year'、'month'或'week'");
            }
            if ("month".equals(period2Period) && period2Month == null) {
                return ResultVo.error("period2Period='month'时必须提供period2Month参数");
            }
            if ("week".equals(period2Period) && (period2WeekStartDate == null || period2WeekEndDate == null)) {
                return ResultVo.error("period2Period='week'时必须提供period2WeekStartDate和period2WeekEndDate参数");
            }

            // 转换 memberId
            Long period1MemberIdLong = null;
            if (period1MemberId != null && !period1MemberId.isEmpty()) {
                try {
                    period1MemberIdLong = Long.parseLong(period1MemberId);
                } catch (NumberFormatException e) {
                    return ResultVo.error("period1MemberId格式不正确");
                }
            }
            Long period2MemberIdLong = null;
            if (period2MemberId != null && !period2MemberId.isEmpty()) {
                try {
                    period2MemberIdLong = Long.parseLong(period2MemberId);
                } catch (NumberFormatException e) {
                    return ResultVo.error("period2MemberId格式不正确");
                }
            }

            // 获取第一个时期的数据
            DateRange range1 = getDateRangeByPeriod(period1Period, period1Year, period1Month, 
                    period1WeekStartDate, period1WeekEndDate);
            BigDecimal income1 = calculateTotalAmount(familyId, 0, range1.start, range1.end);
            BigDecimal expense1 = calculateTotalAmount(familyId, 1, range1.start, range1.end);
            BigDecimal net1 = income1.subtract(expense1);
            
            // 计算交易笔数
            List<Transactions> transactions1 = getTransactions(familyId, transactionType, 
                    range1.start, range1.end, period1MemberIdLong);
            int transactionCount1 = transactions1.size();

            // 获取第二个时期的数据
            DateRange range2 = getDateRangeByPeriod(period2Period, period2Year, period2Month, 
                    period2WeekStartDate, period2WeekEndDate);
            BigDecimal income2 = calculateTotalAmount(familyId, 0, range2.start, range2.end);
            BigDecimal expense2 = calculateTotalAmount(familyId, 1, range2.start, range2.end);
            BigDecimal net2 = income2.subtract(expense2);
            
            // 计算交易笔数
            List<Transactions> transactions2 = getTransactions(familyId, transactionType, 
                    range2.start, range2.end, period2MemberIdLong);
            int transactionCount2 = transactions2.size();

            // 构建响应
            StatisticsCompareResponse.PeriodData period1Data = new StatisticsCompareResponse.PeriodData();
            period1Data.setYear(period1Year);
            if (period1Month != null) {
                period1Data.setMonth(period1Month);
            }
            // 构建标签
            String label1 = period1Year + "年";
            if (period1Month != null) {
                label1 += period1Month + "月";
            } else if ("week".equals(period1Period)) {
                label1 += "第" + period1WeekStartDate + "周";
            }
            period1Data.setLabel(label1);
            period1Data.setTotalIncome(income1);
            period1Data.setTotalExpense(expense1);
            period1Data.setNetIncome(net1);
            period1Data.setTransactionCount(transactionCount1);

            StatisticsCompareResponse.PeriodData period2Data = new StatisticsCompareResponse.PeriodData();
            period2Data.setYear(period2Year);
            if (period2Month != null) {
                period2Data.setMonth(period2Month);
            }
            // 构建标签
            String label2 = period2Year + "年";
            if (period2Month != null) {
                label2 += period2Month + "月";
            } else if ("week".equals(period2Period)) {
                label2 += "第" + period2WeekStartDate + "周";
            }
            period2Data.setLabel(label2);
            period2Data.setTotalIncome(income2);
            period2Data.setTotalExpense(expense2);
            period2Data.setNetIncome(net2);
            period2Data.setTransactionCount(transactionCount2);

            StatisticsCompareResponse.Changes changes = new StatisticsCompareResponse.Changes();
            changes.setIncomeChange(calculateChangeRate(income2, income1));
            changes.setExpenseChange(calculateChangeRate(expense2, expense1));
            changes.setNetChange(calculateChangeRate(net2, net1));
            changes.setCountChange(calculateChangeRate(
                    new BigDecimal(transactionCount2), 
                    new BigDecimal(transactionCount1)));

            StatisticsCompareResponse response = new StatisticsCompareResponse();
            response.setType(type);
            response.setPeriod1(period1Data);
            response.setPeriod2(period2Data);
            response.setChanges(changes);

            return ResultVo.success(response);
        } catch (IllegalArgumentException e) {
            return ResultVo.error(e.getMessage());
        }
    }
}
