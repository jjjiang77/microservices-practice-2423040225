package com.zjgsu.jby.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一错误响应体。
 */
public record ApiErrorResponse(String code, String message, List<String> details, LocalDateTime timestamp) {

    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(code, message, List.of(), LocalDateTime.now());
    }

    public static ApiErrorResponse of(String code, String message, List<String> details) {
        return new ApiErrorResponse(code, message, details, LocalDateTime.now());
    }
}
