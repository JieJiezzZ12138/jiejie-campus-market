package com.jiejie.product.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 👉 核心修改 1：给配置类起个专属名字，防止和 order-service 冲突
@Configuration("productWebConfig")
public class WebConfig implements WebMvcConfigurer {
    @Value("${file.upload-path}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadPath.startsWith("file:") ? uploadPath : "file:" + uploadPath;
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/images/**")
                .addResourceLocations(location);
    }
}
