package com.xiji.filter;

import com.xiji.utils.JwtUtils;
import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import com.xiji.common.response.ResultVo;

import java.io.IOException;

/**
 * 登录检查过滤器
 * @author liberty
 */
@Slf4j
@WebFilter(filterName = "LoginCheckFilter", urlPatterns = "/*")
public class LoginCheckFilter implements Filter {
    
    // 白名单路径，不需要登录验证
    private static final String[] WHITELIST_PATHS = {
        "/api/user/login",
        "/api/user/register",
        "/api/user/captcha",
        "/api/v1/auth/send-code",                    // 手机端发送登录验证码
        "/api/v1/auth/login",                        // 手机端登录
        "/api/v1/auth/register/send-code",           // 手机端发送注册验证码
        "/api/v1/auth/register",                     // 手机端注册
        "/api/v1/auth/forgot-password/send-code",    // 手机端发送忘记密码验证码
        "/api/v1/auth/forgot-password/reset",        // 手机端重置密码
        "/swagger-ui",
        "/swagger-ui.html",
        "/swagger-ui/",
        "/v3/api-docs",
        "/v3/api-docs/",
        "/webjars",
        "/api/v1/upload",
        "/error"  // 错误页面
    };
    
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        
        //获取请求的URI
        String uri = request.getRequestURI();
        
        // 检查是否在白名单中
        if (isWhitelistPath(uri)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        
        //获取JWT令牌（支持token和Authorization两种方式）
        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }
        
        if (token != null && !token.isEmpty()) {
            //验证JWT令牌
            Claims claims = JwtUtils.parseJwt(token);
            if (claims != null) {
                log.debug("JWT令牌验证成功，用户ID={}", claims.get("id"));
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            } else {
                log.warn("JWT令牌验证失败，URI={}", uri);
            }
        } else {
            log.warn("请求缺少JWT令牌，URI={}", uri);
        }
        
        //JWT令牌验证失败或缺失，返回json错误
        Gson gson = new Gson();
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(gson.toJson(ResultVo.error("请先登录")));
    }
    
    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhitelistPath(String uri) {
        for (String whitelistPath : WHITELIST_PATHS) {
            if (uri.startsWith(whitelistPath)) {
                return true;
            }
        }
        return false;
    }
}
