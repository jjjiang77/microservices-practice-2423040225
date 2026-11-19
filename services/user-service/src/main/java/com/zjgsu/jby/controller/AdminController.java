package com.zjgsu.jby.controller;

import com.zjgsu.jby.dto.UserResponse;
import com.zjgsu.jby.service.UserService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端接口。网关要求 ADMIN 角色，服务内部还会校验请求确实来自网关。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public List<UserResponse> listUsers() {
        return userService.listAll();
    }
}
