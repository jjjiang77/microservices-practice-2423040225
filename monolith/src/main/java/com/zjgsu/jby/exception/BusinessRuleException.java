package com.zjgsu.jby.exception;

/**
 * 选题业务规则不满足，映射为 HTTP 422。
 * code 用于区分具体规则，便于测试断言与前端提示。
 */
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
