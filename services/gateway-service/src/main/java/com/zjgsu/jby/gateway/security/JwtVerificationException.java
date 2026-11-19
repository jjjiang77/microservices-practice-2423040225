package com.zjgsu.jby.gateway.security;

/**
 * JWT 校验失败：格式错误、算法不符、签名不匹配或已过期。
 */
public class JwtVerificationException extends RuntimeException {

    public JwtVerificationException(String message) {
        super(message);
    }
}
