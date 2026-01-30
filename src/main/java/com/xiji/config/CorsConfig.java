package com.xiji.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置
 * @author liberty
 */
@Configuration
public class CorsConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        
        // 从环境变量或配置读取允许的源，默认允许本地开发环境
        String allowedOrigin = System.getenv("CORS_ALLOWED_ORIGIN");
        if (allowedOrigin == null || allowedOrigin.isEmpty()) {
            allowedOrigin = "http://localhost:3000";
        }
        
        // 使用addAllowedOriginPattern()以支持credentials（Spring Boot 2.4+要求）
        corsConfiguration.addAllowedOriginPattern("*");
        corsConfiguration.setAllowCredentials(true);
        
        // 允许所有Header
        corsConfiguration.addAllowedHeader("*");
        
        // 允许所有Method
        corsConfiguration.addAllowedMethod("*");
        
        // 允许暴露的响应头
        corsConfiguration.addExposedHeader("*");
        
        source.registerCorsConfiguration("/**", corsConfiguration);
        
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        // 设置最高优先级，确保CorsFilter在其他过滤器之前执行（处理OPTIONS预检请求）
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}