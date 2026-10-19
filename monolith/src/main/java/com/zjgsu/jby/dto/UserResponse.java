package com.zjgsu.jby.dto;

import com.zjgsu.jby.entity.User;
import java.time.LocalDateTime;

/**
 * 用户响应体。
 */
public record UserResponse(
        Long id,
        String studentId,
        String nickname,
        String department,
        String role,
        Integer creditScore,
        LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getStudentId(),
                user.getNickname(),
                user.getDepartment(),
                user.getRole().name(),
                user.getCreditScore(),
                user.getCreatedAt());
    }
}
