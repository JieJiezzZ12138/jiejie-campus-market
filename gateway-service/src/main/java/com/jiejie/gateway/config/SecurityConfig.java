package com.jiejie.gateway.config;

import com.jiejie.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.util.StringUtils;
import org.springframework.security.config.Customizer;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        AuthenticationWebFilter authWebFilter = new AuthenticationWebFilter(reactiveAuthenticationManager());
        authWebFilter.setServerAuthenticationConverter(bearerTokenConverter());
        authWebFilter.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance());
        authWebFilter.setAuthenticationSuccessHandler(authenticationSuccessHandler());
        authWebFilter.setAuthenticationFailureHandler(authenticationFailureHandler());

        http
                .cors(Customizer.withDefaults())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(ex -> ex
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/auth/login", "/auth/register", "/auth/ping").permitAll()
                        .pathMatchers("/product/list", "/images/**").permitAll()
                        .pathMatchers("/auth/admin/**", "/user/admin/**", "/order/admin/**", "/product/admin/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .anyExchange().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .addFilterAt(authWebFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        return http.build();
    }

    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager() {
        return authentication -> {
            String token = (String) authentication.getCredentials();
            Claims claims = JwtUtils.parseToken(token);
            if (claims == null) {
                return Mono.error(new BadCredentialsException("Invalid token"));
            }
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if (role != null && !role.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            Authentication auth = new UsernamePasswordAuthenticationToken(username, token, authorities);
            return Mono.just(auth);
        };
    }

    @Bean
    public ServerAuthenticationConverter bearerTokenConverter() {
        return exchange -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring("Bearer ".length()).trim();
                if (StringUtils.hasText(token)) {
                    return Mono.just(new UsernamePasswordAuthenticationToken(token, token));
                }
            }
            return Mono.empty();
        };
    }

    private ServerAuthenticationSuccessHandler authenticationSuccessHandler() {
        return (webFilterExchange, authentication) ->
                webFilterExchange.getChain().filter(webFilterExchange.getExchange());
    }

    private ServerAuthenticationFailureHandler authenticationFailureHandler() {
        return (webFilterExchange, exception) ->
                writeJson(webFilterExchange.getExchange().getResponse(),
                        HttpStatus.UNAUTHORIZED,
                        "{\"code\":401,\"msg\":\"Token无效或已过期，请重新登录！\"}");
    }

    private ServerAuthenticationEntryPoint authenticationEntryPoint() {
        return (exchange, exception) ->
                writeJson(exchange.getResponse(),
                        HttpStatus.UNAUTHORIZED,
                        "{\"code\":401,\"msg\":\"未登录，请先登录！\"}");
    }

    private ServerAccessDeniedHandler accessDeniedHandler() {
        return (exchange, denied) ->
                writeJson(exchange.getResponse(),
                        HttpStatus.FORBIDDEN,
                        "{\"code\":403,\"msg\":\"无权限访问\"}");
    }

    private Mono<Void> writeJson(org.springframework.http.server.reactive.ServerHttpResponse response,
                                 HttpStatus status,
                                 String body) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}
