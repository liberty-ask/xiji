package com.xiji.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiji.entity.domain.Budget;
import com.xiji.entity.domain.Category;
import com.xiji.entity.domain.Transactions;
import com.xiji.entity.domain.User;
import com.xiji.service.BudgetService;
import com.xiji.service.CategoryService;
import com.xiji.service.TransactionsService;
import com.xiji.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HomeController接口测试：聚合统计、数值契约、crash修复回归
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HomeControllerTest {

    @Mock
    private TransactionsService transactionsService;
    @Mock
    private BudgetService budgetService;
    @Mock
    private CategoryService categoryService;
    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        HomeController controller = new HomeController(
                transactionsService, budgetService, categoryService, userService);
        // BaseController.getCurrentFamilyId 使用父类protected的userService字段，standalone环境需手动注入
        java.lang.reflect.Field userServiceField = BaseController.class.getDeclaredField("userService");
        userServiceField.setAccessible(true);
        userServiceField.set(controller, userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // 用户与当前家庭
        User user = new User();
        user.setId(1L);
        user.setName("张三");
        user.setCurrentFamilyId(100L);
        when(userService.getById(1L)).thenReturn(user);
    }

    private Map<String, Object> aggRow(String key, Object value) {
        Map<String, Object> row = new HashMap<>();
        row.put(key, value);
        return row;
    }

    @Test
    void home_aggregatesStatistics_andReturnsNumericContract() throws Exception {
        // 月聚合：收入1000.00、支出400.50
        Map<String, Object> incomeRow = new HashMap<>();
        incomeRow.put("type", 0);
        incomeRow.put("total", new BigDecimal("1000.00"));
        Map<String, Object> expenseRow = new HashMap<>();
        expenseRow.put("type", 1);
        expenseRow.put("total", new BigDecimal("400.50"));
        // 今日支出50.25、昨日支出30.00（无GROUP BY聚合返回单行）
        List<Map<String, Object>> todayRow = List.of(aggRow("total", new BigDecimal("50.25")));
        List<Map<String, Object>> yesterdayRow = List.of(aggRow("total", new BigDecimal("30.00")));
        // listMaps调用顺序：月聚合 -> 今日 -> 昨日
        when(transactionsService.listMaps(any(QueryWrapper.class)))
                .thenReturn(List.of(incomeRow, expenseRow))
                .thenReturn(todayRow)
                .thenReturn(yesterdayRow);

        // 预算
        Budget budget = new Budget();
        budget.setAmount(new BigDecimal("2000"));
        when(budgetService.getBudget(100L)).thenReturn(budget);

        // 最近交易：一条正常（含createdAt），一条脏数据（createdAt=null但date非空，历史crash场景）
        Transactions t1 = new Transactions();
        t1.setCategoryId(10L);
        t1.setCreatedBy(1L);
        t1.setType(0);
        t1.setAmount(new BigDecimal("100"));
        t1.setDate(LocalDate.of(2026, 10, 19));
        t1.setCreatedAt(LocalDateTime.of(2026, 10, 19, 8, 30));
        t1.setDescription("工资");

        Transactions t2 = new Transactions();
        t2.setCategoryId(11L);
        t2.setCreatedBy(2L);
        t2.setType(1);
        t2.setAmount(new BigDecimal("25.5"));
        t2.setDate(LocalDate.of(2026, 10, 18));
        t2.setCreatedAt(null);
        t2.setDescription("午饭");

        when(transactionsService.list(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(t1, t2));

        Category c1 = new Category();
        c1.setId(10L);
        c1.setName("工资");
        c1.setIcon("work");
        Category c2 = new Category();
        c2.setId(11L);
        c2.setName("餐饮");
        c2.setIcon("restaurant");
        when(categoryService.listByIds(anyCollection())).thenReturn(Arrays.asList(c1, c2));

        User u1 = new User();
        u1.setId(1L);
        u1.setName("张三");
        User u2 = new User();
        u2.setId(2L);
        u2.setName("李四");
        when(userService.listByIds(anyCollection())).thenReturn(Arrays.asList(u1, u2));

        mockMvc.perform(get("/api/v1/home")
                        .requestAttr("userId", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                // 数值契约：金额字段为数字
                .andExpect(jsonPath("$.data.income").value(1000.00))
                .andExpect(jsonPath("$.data.expense").value(400.50))
                .andExpect(jsonPath("$.data.balance").value(599.50))
                .andExpect(jsonPath("$.data.todayExpense").value(50.25))
                .andExpect(jsonPath("$.data.yesterdayExpense").value(30.00))
                .andExpect(jsonPath("$.data.budget.used").value(400.50))
                .andExpect(jsonPath("$.data.budget.total").value(2000))
                // 最近活动：数量、标题（来自批量查询的分类）、数值金额
                .andExpect(jsonPath("$.data.activities.length()").value(2))
                .andExpect(jsonPath("$.data.activities[0].title").value("工资"))
                .andExpect(jsonPath("$.data.activities[0].amount").value(100))
                .andExpect(jsonPath("$.data.activities[0].user").value("张三"))
                .andExpect(jsonPath("$.data.activities[1].title").value("餐饮"))
                .andExpect(jsonPath("$.data.activities[1].user").value("李四"))
                // crash修复：createdAt为null但date非空时，time为当日00:00而不是抛异常
                .andExpect(jsonPath("$.data.activities[1].time").value("2026-10-18 00:00"));
    }

    @Test
    void home_noData_returnsZeros() throws Exception {
        // 无任何交易：月聚合返回空、今昨聚合返回0
        when(transactionsService.listMaps(any(QueryWrapper.class)))
                .thenReturn(List.of())
                .thenReturn(List.of(aggRow("total", BigDecimal.ZERO)))
                .thenReturn(List.of(aggRow("total", new BigDecimal("0"))));
        when(budgetService.getBudget(100L)).thenReturn(null);
        when(transactionsService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/home")
                        .requestAttr("userId", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.income").value(0))
                .andExpect(jsonPath("$.data.expense").value(0))
                .andExpect(jsonPath("$.data.balance").value(0))
                .andExpect(jsonPath("$.data.budget.total").value(0))
                .andExpect(jsonPath("$.data.activities.length()").value(0));
    }
}
