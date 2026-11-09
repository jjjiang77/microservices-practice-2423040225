package com.zjgsu.jby.service;

import com.zjgsu.jby.dto.CreateUserRequest;
import com.zjgsu.jby.dto.UserResponse;
import com.zjgsu.jby.entity.User;
import com.zjgsu.jby.exception.DuplicateResourceException;
import com.zjgsu.jby.exception.ResourceNotFoundException;
import com.zjgsu.jby.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户业务逻辑：注册（学号唯一）与查询。
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 学生注册。业务规则：同一学号只能注册一次（唯一约束 + 前置校验）。
     */
    @Transactional
    public UserResponse register(CreateUserRequest request) {
        String studentId = request.studentId().trim();
        // 先查重以返回可读的冲突信息，数据库唯一约束仍负责兜底。
        if (userRepository.existsByStudentId(studentId)) {
            throw new DuplicateResourceException("学号已被注册: " + studentId);
        }
        User user = new User(studentId, request.nickname().trim(), request.department());
        // 保存用户资料后转换为响应对象。学号登记是当前注册流程。
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(requireUser(id));
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: id=" + id));
    }
}
