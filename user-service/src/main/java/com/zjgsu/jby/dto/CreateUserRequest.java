package com.zjgsu.jby.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册用户（学生）的请求体。
 */
public record CreateUserRequest(
        @NotBlank(message = "学号不能为空")
        @Size(max = 20, message = "学号长度不能超过 20")
        String studentId,

        @NotBlank(message = "昵称不能为空")
        @Size(max = 32, message = "昵称长度不能超过 32")
        String nickname,

        @Size(max = 40, message = "院系长度不能超过 40")
        String department) {
}
