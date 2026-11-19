package com.zjgsu.jby.gateway.security;

import java.time.Instant;

/**
 * 网关从 JWT 中解析出的可信身份。
 *
 * @param userId    用户主键，注入到下游的 X-User-Id
 * @param studentId 学号（JWT 的 sub）
 * @param role      角色，STUDENT 或 ADMIN
 * @param expiresAt 过期时间
 */
public record JwtClaims(Long userId, String studentId, String role, Instant expiresAt) {
}
