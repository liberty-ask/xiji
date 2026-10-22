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
 * 鉴权通过后将userId/username写入request attribute，后续统一从attribute读取（全局仅此一处解析token）
 * @author liberty
 */
@Slf4j
@WebFilter(filterName = "LoginCheckFilter", urlPatterns = "/*")
public class LoginCheckFilter implements Filter {

    // 白名单路径，不需要登录验证
    private static final String[] WHITELIST_PATHS = {
        "/api/v1/auth/send-code",
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/api/v1/auth/register/send-code",
        "/api/v1/auth/register",
        "/api/v1/auth/forgot-password/send-code",
        "/api/v1/auth/forgot-password/reset",
        "/swagger-ui",
        "/swagger-ui.html",
        "/swagger-ui/",
        "/v3/api-docs",
        "/v3/api-docs/",
        "/webjars",
        "/error"  // 错误页面
    };

    private static final Gson GSON = new Gson();

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
                String userId = parseUserId(claims.get("id"));
                if (userId != null) {
                    // 单次解析结果通过request attribute传递
                    request.setAttribute("userId", userId);
                    request.setAttribute("username", String.valueOf(claims.get("username")));
                    log.debug("JWT令牌验证成功，userId={}", userId);
                    filterChain.doFilter(servletRequest, servletResponse);
                    return;
                } else {
                    log.warn("JWT令牌中缺少有效的用户ID，URI={}", uri);
                }
            } else {
                log.warn("JWT令牌验证失败，URI={}", uri);
            }
        } else {
            log.warn("请求缺少JWT令牌，URI={}", uri);
        }

        //JWT令牌验证失败或缺失，返回HTTP 401 + body code=401
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(GSON.toJson(ResultVo.error(401, "请先登录")));
    }

    /**
     * 将claims中的id转换为String（UUID）
     */
    private String parseUserId(Object idObj) {
        if (idObj == null) {
            return null;
        }
        String userId = idObj.toString();
        return userId.isEmpty() ? null : userId;
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
