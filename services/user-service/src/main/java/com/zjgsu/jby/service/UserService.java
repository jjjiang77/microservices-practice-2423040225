package com.zjgsu.jby.service;

import com.zjgsu.jby.dto.CreateUserRequest;
import com.zjgsu.jby.dto.UserResponse;
import com.zjgsu.jby.entity.User;
import com.zjgsu.jby.exception.DuplicateResourceException;
import com.zjgsu.jby.exception.ResourceNotFoundException;
import com.zjgsu.jby.repository.UserRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户业务逻辑：注册（学号唯一）与查询。
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 学生注册。业务规则：同一学号只能注册一次（唯一约束 + 前置校验）。
     * 密码以 BCrypt 哈希写入，数据库与日志中都不会出现明文。
     */
    @Transactional
    public UserResponse register(CreateUserRequest request) {
        String studentId = request.studentId().trim();
        // 先查重以返回可读的冲突信息，数据库唯一约束仍负责兜底。
        if (userRepository.existsByStudentId(studentId)) {
            throw new DuplicateResourceException("学号已被注册: " + studentId);
        }
        User user = new User(studentId, request.nickname().trim(), request.department());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        // 保存用户资料后转换为响应对象。学号登记是当前注册流程。
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(requireUser(id));
    }

    /** 管理员查看全部用户，按主键升序返回。 */
    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return userRepository.findAll().stream()
                .sorted((left, right) -> Long.compare(left.getId(), right.getId()))
                .map(UserResponse::from)
                .toList();
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: id=" + id));
    }
}
