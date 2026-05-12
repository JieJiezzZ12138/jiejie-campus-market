package com.jiejie.order.feign;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignAuthForwardConfig {
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String INTERNAL_TOKEN_ENV = "INTERNAL_API_TOKEN";
    private static final String DEFAULT_INTERNAL_TOKEN = "dev-only-internal-api-token";

    @Bean
    public RequestInterceptor forwardAuthorizationHeader() {
        return template -> {
            template.header(INTERNAL_TOKEN_HEADER, resolveInternalToken());

            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
                return;
            }

            HttpServletRequest request = servletAttributes.getRequest();
            String authorization = request.getHeader("Authorization");
            if (StringUtils.hasText(authorization)) {
                template.header("Authorization", authorization);
            }
        };
    }

    private static String resolveInternalToken() {
        String token = System.getenv(INTERNAL_TOKEN_ENV);
        if (!StringUtils.hasText(token)) {
            return DEFAULT_INTERNAL_TOKEN;
        }
        return token;
    }
}
