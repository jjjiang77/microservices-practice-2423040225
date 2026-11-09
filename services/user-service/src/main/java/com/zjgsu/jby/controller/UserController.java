package com.zjgsu.jby.controller;

import com.zjgsu.jby.dto.CreateUserRequest;
import com.zjgsu.jby.dto.UserResponse;
import com.zjgsu.jby.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口。仅做 HTTP 输入输出，业务逻辑全部委托给 UserService。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 注册学生用户，学号重复返回 409 */
    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody CreateUserRequest request) {
        UserResponse created = userService.register(request);
        return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
    }

    /** 按 ID 查询用户，不存在返回 404 */
    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }
}
