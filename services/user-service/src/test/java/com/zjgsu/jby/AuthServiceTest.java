package com.zjgsu.jby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zjgsu.jby.dto.CreateUserRequest;
import com.zjgsu.jby.dto.LoginRequest;
import com.zjgsu.jby.dto.LoginResponse;
import com.zjgsu.jby.exception.UnauthorizedException;
import com.zjgsu.jby.service.AuthService;
import com.zjgsu.jby.service.UserService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * 认证链路测试：注册写入 BCrypt 哈希、正确密码签发 JWT、错误密码被拒绝。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

    /** database/seed.sql 中学生账号的密码哈希 */
    private static final String SEED_STUDENT_HASH = "$2y$10$rKtFxL9LdIL2QMh/3Wb3zOgEIjfMyGo6hCHOfAGrXdqzDm/1XXIqa";
    /** database/seed.sql 中管理员账号的密码哈希 */
    private static final String SEED_ADMIN_HASH = "$2y$10$KVHuV3UdsGGwFZOMB24TM.QdbClJM6Upoh93/bGU420AzhHu7Cm.O";

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Test
    void seedHashesMatchDocumentedPasswords() {
        var encoder = new BCryptPasswordEncoder();
        assertThat(encoder.matches("Student@123", SEED_STUDENT_HASH)).isTrue();
        assertThat(encoder.matches("Admin@123", SEED_ADMIN_HASH)).isTrue();
    }

    @Test
    void loginWithCorrectPasswordReturnsJwt() {
        String studentId = "T" + UUID.randomUUID().toString().substring(0, 8);
        userService.register(new CreateUserRequest(studentId, "测试同学", "计算机学院", "Password@123"));

        LoginResponse response = authService.login(new LoginRequest(studentId, "Password@123"));

        assertThat(response.accessToken().split("\\.")).hasSize(3);
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.role()).isEqualTo("STUDENT");
        assertThat(response.studentId()).isEqualTo(studentId);
    }

    @Test
    void loginWithWrongPasswordIsRejected() {
        String studentId = "T" + UUID.randomUUID().toString().substring(0, 8);
        userService.register(new CreateUserRequest(studentId, "测试同学", "计算机学院", "Password@123"));

        assertThatThrownBy(() -> authService.login(new LoginRequest(studentId, "wrong-password")))
                .isInstanceOf(UnauthorizedException.class);
    }
}
