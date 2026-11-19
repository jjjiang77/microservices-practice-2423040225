package com.zjgsu.jby.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 下游来源校验：除公开接口外，请求必须携带网关内部密钥。
 *
 * <p>这样即使有人绕过网关直接访问 user-service 端口，也会被 401 拒绝；
 * 客户端伪造 X-User-Role 但没有密钥，同样无法调用管理端接口。</p>
 */
@Component
public class GatewayGuardFilter extends OncePerRequestFilter {

    public static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    private final byte[] gatewaySecret;

    public GatewayGuardFilter(@Value("${app.security.gateway-secret}") String gatewaySecret) {
        this.gatewaySecret = gatewaySecret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isPublic(request.getMethod(), path)) {
            chain.doFilter(request, response);
            return;
        }

        String secret = request.getHeader(GATEWAY_SECRET_HEADER);
        if (secret == null || !MessageDigest.isEqual(gatewaySecret, secret.getBytes(StandardCharsets.UTF_8))) {
            reject(response, HttpServletResponse.SC_UNAUTHORIZED, "UNTRUSTED_REQUEST", "请求必须经过 API 网关");
            return;
        }
        if (path.startsWith("/api/admin") && !"ADMIN".equals(request.getHeader(USER_ROLE_HEADER))) {
            reject(response, HttpServletResponse.SC_FORBIDDEN, "ACCESS_DENIED", "需要管理员角色");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isPublic(String method, String path) {
        if (path.startsWith("/actuator")) {
            return true;
        }
        return "POST".equals(method) && ("/api/auth/login".equals(path) || "/api/users".equals(path));
    }

    private void reject(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
