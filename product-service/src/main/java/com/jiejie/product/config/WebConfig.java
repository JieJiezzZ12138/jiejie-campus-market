package com.jiejie.product.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 👉 核心修改 1：给配置类起个专属名字，防止和 order-service 冲突
@Configuration("productWebConfig")
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 👉 核心修改 2：Controller 里返回的是 /images/xxx.jpg，所以这里必须映射 /images/**
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:/Users/jiejie/Desktop/web/my-system-cloud/pictures/");
    }
}