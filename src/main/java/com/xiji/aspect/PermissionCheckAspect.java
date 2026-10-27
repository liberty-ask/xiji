package com.xiji.aspect;

import com.xiji.config.exception.UnauthorizedException;
import com.xiji.common.annotation.CheckPermission;
import com.xiji.entity.domain.User;
import com.xiji.service.FamilyMemberService;
import com.xiji.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.JoinPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

/**
 * 权限检查切面类
 * 用于检查用户是否有权限执行某个操作
 */
@Slf4j
@Aspect
@Component
public class PermissionCheckAspect {

    private final UserService userService;
    private final FamilyMemberService familyMemberService;

    public PermissionCheckAspect(UserService userService, FamilyMemberService familyMemberService) {
        this.userService = userService;
        this.familyMemberService = familyMemberService;
    }

    @Before("@annotation(checkPermission)")
    public void checkPermission(JoinPoint joinPoint, CheckPermission checkPermission) throws UnauthorizedException {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
            // 获取注解中定义的权限名称
            String requiredPermission = checkPermission.value();

            // 从request attribute获取当前用户ID（由LoginCheckFilter解析token后写入）
            Object userIdObj = request.getAttribute("userId");
            if (userIdObj == null) {
                throw new UnauthorizedException("未登录或token已过期");
            }
            Long userId = (Long) userIdObj;

            // 获取用户信息
            User user = userService.getById(userId);
            if (user == null) {
                throw new UnauthorizedException("用户不存在");
            }

            // 获取当前选择的家庭ID
            Long familyId = user.getCurrentFamilyId();
            if (familyId == null) {
                throw new UnauthorizedException("请先选择家庭");
            }

            // 从家庭成员关联表获取角色
            Integer role = familyMemberService.getMemberRole(familyId, userId);
            if (role == null) {
                throw new UnauthorizedException("用户不是该家庭的成员");
            }

            // 检查权限（默认要求管理员权限，role=1）
            if ("1".equals(requiredPermission) && role != 1) {
                throw new UnauthorizedException(UnauthorizedException.FORBIDDEN, "权限不足，需要管理员权限");
            }

        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("权限检查异常", e);
            throw new UnauthorizedException("权限检查失败");
        }
    }
}

