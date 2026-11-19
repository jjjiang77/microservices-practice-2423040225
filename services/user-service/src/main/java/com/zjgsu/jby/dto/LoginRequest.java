package com.zjgsu.jby.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求体。
 */
public record LoginRequest(
        @NotBlank(message = "学号不能为空")
        String studentId,

        @NotBlank(message = "密码不能为空")
        String password) {
}
