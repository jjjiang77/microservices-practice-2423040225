package com.zjgsu.jby.service;

import com.zjgsu.jby.dto.LoginRequest;
import com.zjgsu.jby.dto.LoginResponse;
import com.zjgsu.jby.entity.User;
import com.zjgsu.jby.exception.UnauthorizedException;
import com.zjgsu.jby.repository.UserRepository;
import com.zjgsu.jby.security.JwtTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录认证：校验学号与 BCrypt 密码哈希，通过后签发 JWT。
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByStudentId(request.studentId().trim())
                .orElseThrow(() -> new UnauthorizedException("学号或密码错误"));
        String passwordHash = user.getPasswordHash();
        // 历史数据没有密码哈希时同样按认证失败处理，不泄露账号是否存在。
        if (passwordHash == null || !passwordEncoder.matches(request.password(), passwordHash)) {
            throw new UnauthorizedException("学号或密码错误");
        }
        String token = jwtTokenService.issue(user.getId(), user.getStudentId(), user.getRole().name());
        return new LoginResponse(
                token,
                "Bearer",
                jwtTokenService.ttlSeconds(),
                user.getId(),
                user.getStudentId(),
                user.getRole().name());
    }
}
