package com.jiejie.order.security;

import com.jiejie.common.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class AuthContext {

    private AuthContext() {
    }

    public static Long currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("currentUserId");
        if (attr instanceof Number number) {
            return number.longValue();
        }
        if (attr instanceof String str && StringUtils.hasText(str)) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        String token = resolveBearerToken(request);
        if (!StringUtils.hasText(token)) {
            return null;
        }
        Long userId = JwtUtils.getUserId(token);
        if (userId != null) {
            request.setAttribute("currentUserId", userId);
        }
        return userId;
    }

    public static String currentUsername(HttpServletRequest request) {
        Object attr = request.getAttribute("currentUsername");
        if (attr instanceof String str && StringUtils.hasText(str)) {
            return str;
        }
        String token = resolveBearerToken(request);
        if (!StringUtils.hasText(token)) {
            return null;
        }
        String username = JwtUtils.getUsername(token);
        if (StringUtils.hasText(username)) {
            request.setAttribute("currentUsername", username);
        }
        return username;
    }

    private static String resolveBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring("Bearer ".length()).trim();
            if (StringUtils.hasText(token)) {
                return token;
            }
        }
        return null;
    }
}
