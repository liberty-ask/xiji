package com.xiji.config;

import com.google.gson.Gson;
import com.xiji.common.response.ResultVo;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 跨域配置
 * @author liberty
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class CorsConfig {

    private final CustomConfig customConfig;

    @Bean
    public FilterRegistrationBean<CustomCorsFilter> corsFilterRegistration() {
        CustomCorsFilter filter = new CustomCorsFilter(customConfig);
        
        FilterRegistrationBean<CustomCorsFilter> registration = new FilterRegistrationBean<>(filter);
        // 设置最高优先级，确保CorsFilter在其他过滤器之前执行（处理OPTIONS预检请求）
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        log.info("CORS配置完成");
        return registration;
    }

    /**
     * 自定义CORS过滤器
     * 统一处理CORS验证失败的响应
     */
    @Slf4j
    @RequiredArgsConstructor
    public static class CustomCorsFilter implements Filter {
        
        private final CustomConfig customConfig;
        private final Gson gson = new Gson();
        
        private CorsConfiguration corsConfiguration;
        
        @Override
        public void init(FilterConfig filterConfig) {
            corsConfiguration = buildCorsConfiguration(customConfig.getCors());
        }
        
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
                throws IOException, ServletException {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            // 检查是否是CORS请求
            if (CorsUtils.isCorsRequest(httpRequest)) {
                String origin = httpRequest.getHeader("Origin");
                
                // 验证Origin
                if (!isOriginAllowed(origin)) {
                    log.warn("CORS验证失败，Origin不被允许: {}", origin);
                    writeCorsErrorResponse(httpResponse, "跨域请求被拒绝，不允许的源");
                    return;
                }
                
                // 处理预检请求
                if (CorsUtils.isPreFlightRequest(httpRequest)) {
                    // 设置CORS响应头
                    setCorsResponseHeaders(httpResponse, origin);
                    // 预检请求直接返回200
                    httpResponse.setStatus(HttpServletResponse.SC_OK);
                    return;
                }
                
                // 非预检请求，设置CORS响应头后继续处理
                setCorsResponseHeaders(httpResponse, origin);
            }
            
            chain.doFilter(request, response);
        }
        
        /**
         * 检查Origin是否被允许
         */
        private boolean isOriginAllowed(String origin) {
            if (origin == null || origin.isEmpty()) {
                return true;
            }
            
            CustomConfig.CorsConfig corsConfig = customConfig.getCors();
            String allowedOrigins = corsConfig.getAllowedOrigins();
            if (allowedOrigins == null || allowedOrigins.isEmpty()) {
                return false;
            }
            
            List<String> origins = Arrays.asList(allowedOrigins.split(","));
            return origins.stream()
                    .map(String::trim)
                    .anyMatch(allowed -> {
                        if ("*".equals(allowed)) {
                            return true;
                        }
                        return allowed.equals(origin);
                    });
        }
        
        /**
         * 设置CORS响应头
         */
        private void setCorsResponseHeaders(HttpServletResponse response, String origin) {
            CustomConfig.CorsConfig corsConfig = customConfig.getCors();
            
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", 
                    String.valueOf(corsConfig.getAllowCredentials()));
            response.setHeader("Access-Control-Allow-Methods", corsConfig.getAllowedMethods());
            response.setHeader("Access-Control-Allow-Headers", corsConfig.getAllowedHeaders());
            response.setHeader("Access-Control-Expose-Headers", corsConfig.getExposedHeaders());
            // 预检请求的缓存时间（秒）
            response.setHeader("Access-Control-Max-Age", "3600");
        }
        
        /**
         * 写入CORS错误响应（统一格式）
         */
        private void writeCorsErrorResponse(HttpServletResponse response, String message) 
                throws IOException {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            
            ResultVo resultVo = ResultVo.error(message);
            String json = gson.toJson(resultVo);
            response.getWriter().write(json);
        }
        
        /**
         * 构建CORS配置
         */
        private CorsConfiguration buildCorsConfiguration(CustomConfig.CorsConfig corsConfig) {
            CorsConfiguration configuration = new CorsConfiguration();
            
            String allowedOrigins = corsConfig.getAllowedOrigins();
            if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
                String[] origins = allowedOrigins.split(",");
                for (String origin : origins) {
                    String trimmedOrigin = origin.trim();
                    if (!trimmedOrigin.isEmpty()) {
                        configuration.addAllowedOriginPattern(trimmedOrigin);
                        log.info("配置CORS允许源: {}", trimmedOrigin);
                    }
                }
            }
            
            configuration.setAllowCredentials(corsConfig.getAllowCredentials());
            
            String allowedMethods = corsConfig.getAllowedMethods();
            if (allowedMethods != null && !allowedMethods.isEmpty()) {
                if ("*".equals(allowedMethods)) {
                    configuration.addAllowedMethod("*");
                } else {
                    Arrays.stream(allowedMethods.split(","))
                            .map(String::trim)
                            .filter(method -> !method.isEmpty())
                            .forEach(configuration::addAllowedMethod);
                }
            }
            
            String allowedHeaders = corsConfig.getAllowedHeaders();
            if (allowedHeaders != null && !allowedHeaders.isEmpty()) {
                if ("*".equals(allowedHeaders)) {
                    configuration.addAllowedHeader("*");
                } else {
                    Arrays.stream(allowedHeaders.split(","))
                            .map(String::trim)
                            .filter(header -> !header.isEmpty())
                            .forEach(configuration::addAllowedHeader);
                }
            }
            
            String exposedHeaders = corsConfig.getExposedHeaders();
            if (exposedHeaders != null && !exposedHeaders.isEmpty()) {
                if ("*".equals(exposedHeaders)) {
                    configuration.addExposedHeader("*");
                } else {
                    Arrays.stream(exposedHeaders.split(","))
                            .map(String::trim)
                            .filter(header -> !header.isEmpty())
                            .forEach(configuration::addExposedHeader);
                }
            }
            
            return configuration;
        }
    }
}