package com.zjgsu.jby.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * HS256 JWT 签发器。载荷包含学号、用户主键和角色，网关用同一密钥验签。
 */
@Service
public class JwtTokenService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final byte[] secret;
    private final long ttlSeconds;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtTokenService(
            @Value("${app.security.jwt-secret}") String secret,
            @Value("${app.security.jwt-ttl-seconds:3600}") long ttlSeconds) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlSeconds;
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }

    /**
     * 为已通过密码校验的用户签发 Token。
     */
    public String issue(Long userId, String studentId, String role) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        // sub 放学号，uid 放数据库主键，网关据此注入可信身份头。
        payload.put("sub", studentId);
        payload.put("uid", userId);
        payload.put("role", role);
        payload.put("iat", now);
        payload.put("exp", now + ttlSeconds);

        String signingInput = encode(header) + "." + encode(payload);
        return signingInput + "." + URL_ENCODER.encodeToString(hmac(signingInput));
    }

    private String encode(Map<String, Object> value) {
        return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private byte[] hmac(String signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("JWT 签名计算失败", ex);
        }
    }
}
