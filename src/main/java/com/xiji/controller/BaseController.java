package com.xiji.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiji.entity.domain.FamilyMember;
import com.xiji.entity.domain.User;
import com.xiji.service.FamilyMemberService;
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

    @Autowired
    protected FamilyMemberService familyMemberService;

    /**
     * 从请求中获取当前用户ID
     * 由LoginCheckFilter解析token后写入request attribute，此处直接读取
     * @param request HttpServletRequest
     * @return 用户ID，如果未登录或token无效返回null
     */
    protected String getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        return userId instanceof String ? (String) userId : null;
    }

    /**
     * 从请求中获取当前用户ID（字符串形式）
     * @param request HttpServletRequest
     * @return 用户ID字符串，如果未登录或token无效返回null
     */
    protected String getCurrentUserIdAsString(HttpServletRequest request) {
        return getCurrentUserId(request);
    }

    /**
     * 获取当前用户选择的家庭ID
     * @param userId 用户ID
     * @return 家庭ID，如果用户没有选择家庭返回null
     */
    protected String getCurrentFamilyId(String userId) {
        if (userId == null) {
            return null;
        }

        User user = userService.getById(userId);
        if (user == null) {
            return null;
        }

        return user.getCurrentFamilyId();
    }

    /**
     * 检查用户是否是家庭的成员
     * @param userId 用户ID
     * @param familyId 家庭ID
     * @return 是否是家庭成员
     */
    protected boolean isFamilyMember(String userId, String familyId) {
        if (userId == null || familyId == null) {
            return false;
        }

        LambdaQueryWrapper<FamilyMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FamilyMember::getUserId, userId)
                   .eq(FamilyMember::getFamilyId, familyId);
        return familyMemberService.count(queryWrapper) > 0;
    }
}




