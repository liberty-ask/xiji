package com.xiji.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 配置类
 * 用于配置 Swagger/OpenAPI 文档信息
 * 
 * @author liberty
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // 定义安全方案
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .name("token")
                .in(SecurityScheme.In.HEADER)
                .description("JWT认证令牌，从登录接口获取。也可以在Authorization头中使用Bearer Token格式");

        // 添加Bearer Token安全方案（用于Authorization头）
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT认证令牌，使用Bearer Token格式：Authorization: Bearer <token>");

        return new OpenAPI()
                .info(new Info()
                        .title("家庭财务管理系统 API 文档")
                        .description("家庭财务管理系统接口文档，提供用户管理、交易记录、分类管理等功能")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("liberty")
                                .email(""))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .components(new Components()
                        .addSecuritySchemes("token", securityScheme)
                        .addSecuritySchemes("Bearer", bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList("token"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"));
    }
}


