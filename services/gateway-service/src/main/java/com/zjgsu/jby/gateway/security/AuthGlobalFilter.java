package com.zjgsu.jby.gateway.security;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 统一鉴权过滤器。
 *
 * <ol>
 *   <li>先删除客户端自带的身份头，防止伪造；</li>
 *   <li>公开接口直接放行；</li>
 *   <li>其余接口必须携带验签通过的 Bearer Token，否则 401；</li>
 *   <li>/api/admin/** 额外要求 ADMIN 角色，否则 403；</li>
 *   <li>鉴权通过后注入可信身份头和网关内部密钥。</li>
 * </ol>
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** 下游服务用该头判断请求是否确实经过网关 */
    public static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    private final JwtVerifier jwtVerifier;
    private final String gatewaySecret;

    public AuthGlobalFilter(
            JwtVerifier jwtVerifier,
            @Value("${app.security.gateway-secret}") String gatewaySecret) {
        this.jwtVerifier = jwtVerifier;
        this.gatewaySecret = gatewaySecret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 无论是否公开接口，都先剥离客户端可能伪造的身份头。
        ServerWebExchange sanitized = exchange.mutate().request(withoutIdentityHeaders(request)).build();
        if (isPublic(request.getMethod().name(), path)) {
            return chain.filter(sanitized);
        }

        String token = bearerToken(request);
        if (token == null) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "未登录或缺少 Bearer Token");
        }

        JwtClaims claims;
        try {
            claims = jwtVerifier.verify(token);
        } catch (JwtVerificationException ex) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", ex.getMessage());
        }

        if (path.startsWith("/api/admin") && !"ADMIN".equals(claims.role())) {
            return reject(exchange, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "需要管理员角色");
        }

        // 只有验签通过后才写入可信身份，下游不得再信任客户端原始请求头。
        ServerHttpRequest authenticated = sanitized.getRequest().mutate()
                .header(USER_ID_HEADER, String.valueOf(claims.userId()))
                .header(USER_ROLE_HEADER, claims.role())
                .header(GATEWAY_SECRET_HEADER, gatewaySecret)
                .build();
        return chain.filter(sanitized.mutate().request(authenticated).build());
    }

    private ServerHttpRequest withoutIdentityHeaders(ServerHttpRequest request) {
        return request.mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_ROLE_HEADER);
                    headers.remove(GATEWAY_SECRET_HEADER);
                })
                .build();
    }

    private String bearerToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return token.isEmpty() ? null : token;
    }

    private boolean isPublic(String method, String path) {
        if (path.startsWith("/actuator/health")) {
            return true;
        }
        return "POST".equals(method) && ("/api/auth/login".equals(path) || "/api/users".equals(path));
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
