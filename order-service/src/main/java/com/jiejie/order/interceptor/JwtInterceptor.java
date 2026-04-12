package com.jiejie.order.interceptor;

import com.jiejie.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 允许跨域的 OPTIONS 请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("token");

        if (token == null || token.isEmpty()) {
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录，请先登录！\"}");
            return false;
        }

        Claims claims = JwtUtils.parseToken(token);
        if (claims == null) {
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"Token无效或已过期，请重新登录！\"}");
            return false;
        }

        // 解析成功：userId 在 JWT 里可能是 Integer/Long，统一为 Long，避免与数据库 Long 比较 equals 失败
        Object rawUserId = claims.get("userId");
        Long userId = rawUserId == null ? null : ((Number) rawUserId).longValue();
        request.setAttribute("currentUserId", userId);
        request.setAttribute("currentUsername", claims.get("username", String.class));

        return true;
    }
}