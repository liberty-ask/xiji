package com.xiji.controller;

import com.xiji.entity.domain.User;
import com.xiji.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Controller基类
 * 提供公共方法，减少代码重复
 * @author liberty
 */
@Slf4j
public abstract class BaseController {

    @Autowired
    protected UserService userService;

    /**
     * 从请求中获取当前用户ID
     * 由LoginCheckFilter解析token后写入request attribute（Long），此处直接读取
     * @param request HttpServletRequest
     * @return 用户ID，如果未登录或token无效返回null
     */
    protected Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        return userId instanceof Long ? (Long) userId : null;
    }

    /**
     * 获取当前用户选择的家庭ID
     * @param userId 用户ID
     * @return 家庭ID，如果用户没有选择家庭返回null
     */
    protected Long getCurrentFamilyId(Long userId) {
        if (userId == null) {
            return null;
        }

        User user = userService.getById(userId);
        if (user == null) {
            return null;
        }

        return user.getCurrentFamilyId();
    }
}
