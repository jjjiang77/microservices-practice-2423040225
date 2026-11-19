package com.zjgsu.jby.dto;

/**
 * 登录响应体。accessToken 为 HS256 JWT，后续请求放入 Authorization: Bearer &lt;token&gt;。
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String studentId,
        String role) {
}
