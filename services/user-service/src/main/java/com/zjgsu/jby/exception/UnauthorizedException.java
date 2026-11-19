package com.zjgsu.jby.exception;

/**
 * 认证失败：账号不存在或密码不匹配，统一返回 401。
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
