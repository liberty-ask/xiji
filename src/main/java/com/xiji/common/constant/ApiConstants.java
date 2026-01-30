package com.xiji.common.constant;

/**
 * API路径常量类
 * 统一管理所有API路径，便于维护和版本控制
 * 
 * @author liberty
 */
public class ApiConstants {
    
    /**
     * API版本前缀
     */
    public static final String API_V1 = "/api/v1";
    
    /**
     * 认证相关接口
     */
    public static final String AUTH_BASE = API_V1 + "/auth";
    public static final String AUTH_LOGIN = AUTH_BASE + "/login";
    public static final String AUTH_REGISTER = AUTH_BASE + "/register";
    public static final String AUTH_SEND_CODE = AUTH_BASE + "/send-code";
    public static final String AUTH_REGISTER_SEND_CODE = AUTH_BASE + "/register/send-code";
    public static final String AUTH_FORGOT_PASSWORD = AUTH_BASE + "/forgot-password";
    public static final String AUTH_FORGOT_PASSWORD_SEND_CODE = AUTH_BASE + "/forgot-password/send-code";
    public static final String AUTH_FORGOT_PASSWORD_RESET = AUTH_BASE + "/forgot-password/reset";
    
    /**
     * 用户相关接口
     */
    public static final String USER_BASE = API_V1 + "/user";
    public static final String USER_PROFILE = USER_BASE + "/profile";
    public static final String USER_CHANGE_PASSWORD = USER_BASE + "/change-password";
    public static final String USER_CAPTCHA = USER_BASE + "/captcha";
    
    /**
     * 家庭相关接口
     */
    public static final String FAMILY_BASE = API_V1 + "/families";
    public static final String FAMILY_LIST = FAMILY_BASE + "/list";
    public static final String FAMILY_DETAIL = FAMILY_BASE + "/detail";
    public static final String FAMILY_CREATE = FAMILY_BASE + "/create";
    public static final String FAMILY_MEMBERS = FAMILY_BASE + "/members";
    public static final String FAMILY_EXIT = FAMILY_BASE + "/exit";
    public static final String FAMILY_APPLY = FAMILY_BASE + "/apply";
    public static final String FAMILY_SWITCH = FAMILY_BASE + "/switch";
    public static final String FAMILY_PENDING_APPLICATIONS = FAMILY_BASE + "/pending-applications";
    public static final String FAMILY_PROCESS_APPLICATION = FAMILY_BASE + "/process-application";
    
    /**
     * 交易记录相关接口
     */
    public static final String TRANSACTIONS_BASE = API_V1 + "/transactions";
    
    /**
     * 分类相关接口
     */
    public static final String CATEGORIES_BASE = API_V1 + "/categories";
    public static final String CATEGORIES_LIST = CATEGORIES_BASE + "/list";
    public static final String CATEGORIES_ENABLED = CATEGORIES_BASE + "/enabled";
    
    /**
     * 预算相关接口
     */
    public static final String BUDGETS_BASE = API_V1 + "/budgets";
    
    /**
     * 统计相关接口
     */
    public static final String STATISTICS_BASE = API_V1 + "/statistics";
    
    /**
     * 日历相关接口
     */
    public static final String CALENDAR_BASE = API_V1 + "/calendar";
    public static final String CALENDAR_OVERVIEW = CALENDAR_BASE + "/overview";
    
    /**
     * 首页相关接口
     */
    public static final String HOME_BASE = API_V1 + "/home";
    public static final String HOME_COUNT_TOTAL = HOME_BASE + "/count/total";
    public static final String HOME_COUNT_RANK = HOME_BASE + "/count/rank";
    
    /**
     * 上传相关接口
     */
    public static final String UPLOAD_BASE = API_V1 + "/upload";
    public static final String UPLOAD_FILE = UPLOAD_BASE + "/file";
    
    /**
     * 工具相关接口
     */
    public static final String UTILS_BASE = API_V1 + "/utils";
    public static final String UTILS_WEATHER = UTILS_BASE + "/weather";
    
    private ApiConstants() {
        // 工具类，禁止实例化
    }
}








