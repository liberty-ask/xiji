package com.xiji.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiji.entity.domain.FamilyMember;
import com.xiji.entity.domain.User;
import com.xiji.service.FamilyMemberService;
import com.xiji.service.UserService;
import com.xiji.utils.JwtUtils;
import io.jsonwebtoken.Claims;
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
     * @param request HttpServletRequest
     * @return 用户ID，如果未登录或token无效返回null
     */
    protected Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (token == null || token.isEmpty()) {
            return null;
        }

        Claims claims = JwtUtils.parseJwt(token);
        if (claims == null) {
            return null;
        }

        Object idObj = claims.get("id");
        if (idObj == null) {
            return null;
        }

        if (idObj instanceof Long) {
            return (Long) idObj;
        } else if (idObj instanceof Number) {
            return ((Number) idObj).longValue();
        } else if (idObj instanceof String) {
            try {
                return Long.parseLong((String) idObj);
            } catch (NumberFormatException e) {
                log.warn("无法解析用户ID: {}", idObj);
                return null;
            }
        }

        return null;
    }

    /**
     * 从请求中获取当前用户ID（字符串形式）
     * @param request HttpServletRequest
     * @return 用户ID字符串，如果未登录或token无效返回null
     */
    protected String getCurrentUserIdAsString(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return userId != null ? String.valueOf(userId) : null;
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

    /**
     * 检查用户是否是家庭的成员
     * @param userId 用户ID
     * @param familyId 家庭ID
     * @return 是否是家庭成员
     */
    protected boolean isFamilyMember(Long userId, Long familyId) {
        if (userId == null || familyId == null) {
            return false;
        }

        LambdaQueryWrapper<FamilyMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FamilyMember::getUserId, userId)
                   .eq(FamilyMember::getFamilyId, familyId);
        return familyMemberService.count(queryWrapper) > 0;
    }
}




