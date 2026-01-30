package com.xiji.aspect;

import com.xiji.config.exception.UnauthorizedException;
import com.xiji.common.annotation.CheckPermission;
import com.xiji.entity.domain.User;
import com.xiji.service.FamilyMemberService;
import com.xiji.service.UserService;
import com.xiji.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.JoinPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.Objects;

/**
 * 权限检查切面类
 * 用于检查用户是否有权限执行某个操作
 */
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
            
            // 从请求头获取token
            String token = request.getHeader("token");
            if (token == null || token.isEmpty()) {
                // 尝试从Authorization头获取
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }
            
            if (token == null || token.isEmpty()) {
                throw new UnauthorizedException("未登录或token已过期");
            }
            
            // 解析token获取用户信息
            Map<String, Object> claims = JwtUtils.parseJwt(token);
            if (claims == null) {
                throw new UnauthorizedException("token无效");
            }
            
            // 获取用户ID
            Object idObj = claims.get("id");
            if (idObj == null) {
                throw new UnauthorizedException("用户ID信息缺失");
            }
            
            Long userId = null;
            if (idObj instanceof Long) {
                userId = (Long) idObj;
            } else if (idObj instanceof Number) {
                userId = ((Number) idObj).longValue();
            } else if (idObj instanceof String) {
                try {
                    userId = Long.parseLong((String) idObj);
                } catch (NumberFormatException e) {
                    throw new UnauthorizedException("用户ID格式错误");
                }
            }
            
            if (userId == null) {
                throw new UnauthorizedException("用户ID格式错误");
            }
            
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
                throw new UnauthorizedException("权限不足，需要管理员权限");
            }
            
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("权限检查失败: " + e.getMessage());
        }
    }
}

