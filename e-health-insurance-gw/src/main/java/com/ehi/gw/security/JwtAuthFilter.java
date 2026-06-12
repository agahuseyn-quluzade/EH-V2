package com.ehi.gw.security;

import com.ehi.gw.config.GatewaySecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final GatewaySecurityProperties securityProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isPublic(path, request.getMethod())) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Auth rejected: {} {} (missing/invalid token)", request.getMethod(), path);
            return unauthorized(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!jwtProvider.isTokenValid(token)) {
            log.warn("Auth rejected: {} {} (missing/invalid token)", request.getMethod(), path);
            return unauthorized(exchange);
        }

        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", jwtProvider.getUserId(token))
                .header("X-User-Role", jwtProvider.getRole(token))
                .build();

        log.debug("Authenticated request: userId={}, path={}", jwtProvider.getUserId(token), path);
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private boolean isPublic(String path, HttpMethod method) {
        boolean publicPath = securityProperties.getPublicPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));

        boolean publicGetPath = HttpMethod.GET.equals(method)
                && securityProperties.getPublicGetPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));

        return publicPath || publicGetPath;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }
}
