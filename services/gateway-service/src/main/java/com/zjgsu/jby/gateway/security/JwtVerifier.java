package com.zjgsu.jby.gateway.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * HS256 JWT 验签器。网关只信任本服务用共享密钥验签通过的 Token，
 * 客户端自带的身份请求头一律丢弃。
 */
@Component
public class JwtVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final byte[] secret;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtVerifier(@Value("${app.security.jwt-secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 校验 Token 并返回身份声明，任何异常情况统一抛出 {@link JwtVerificationException}。
     */
    public JwtClaims verify(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtVerificationException("Token 格式不正确");
        }
        if (!"HS256".equals(readJson(parts[0]).path("alg").asString(""))) {
            throw new JwtVerificationException("Token 签名算法不受支持");
        }

        // 签名覆盖 "头部.载荷"，必须使用原始 Base64URL 文本重新计算。
        String signingInput = parts[0] + "." + parts[1];
        byte[] expected = hmac(signingInput);
        byte[] actual;
        try {
            actual = URL_DECODER.decode(parts[2]);
        } catch (IllegalArgumentException ex) {
            throw new JwtVerificationException("Token 签名无法解码");
        }
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new JwtVerificationException("Token 签名校验失败");
        }

        JsonNode payload = readJson(parts[1]);
        long expiresAt = payload.path("exp").asLong(0);
        if (expiresAt <= Instant.now().getEpochSecond()) {
            throw new JwtVerificationException("Token 已过期");
        }
        long userId = payload.path("uid").asLong(0);
        String role = payload.path("role").asString("");
        if (userId <= 0 || role.isEmpty()) {
            throw new JwtVerificationException("Token 缺少用户或角色声明");
        }
        return new JwtClaims(userId, payload.path("sub").asString(""), role, Instant.ofEpochSecond(expiresAt));
    }

    private JsonNode readJson(String base64Url) {
        try {
            return objectMapper.readTree(new String(URL_DECODER.decode(base64Url), StandardCharsets.UTF_8));
        } catch (RuntimeException ex) {
            throw new JwtVerificationException("Token 载荷无法解析");
        }
    }

    private byte[] hmac(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("JWT 签名计算失败", ex);
        }
    }
}
