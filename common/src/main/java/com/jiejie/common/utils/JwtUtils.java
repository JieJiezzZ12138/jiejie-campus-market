package com.jiejie.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtils {

    // 完美规范：秘钥长度必须大于 32 字节
    private static final String SECRET_STRING = "JiejieMallSuperSecretKey666ThisIsVeryLong";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes());

    // 过期时间：7天
    private static final long EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000;

    /**
     * 1. 生成 Token
     */
    public static String generateToken(Long userId, String username, String role) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("username", username)
                .claim("role", role)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static String generateToken(Long userId, String username) {
        return generateToken(userId, username, "USER");
    }

    /**
     * 2. 解析 Token (基础方法)
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            // 解析失败（过期、篡改等）直接返回 null
            return null;
        }
    }

    /**
     * 3. 👉 新增：直接从 Token 中获取用户 ID
     * 对应 ProductController 里的调用：JwtUtils.getUserId(token)
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;

        Object userId = claims.get("userId");
        if (userId == null) return null;

        // 这里的 userId 拿出来可能是 Integer，需要转成 Long
        return Long.valueOf(userId.toString());
    }

    /**
     * 4. 👉 新增：直接从 Token 中获取用户名
     */
    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;
        return claims.get("username", String.class);
    }

    public static String getRole(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;
        return claims.get("role", String.class);
    }
}
