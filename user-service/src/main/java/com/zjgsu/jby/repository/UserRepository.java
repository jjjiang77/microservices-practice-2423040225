package com.zjgsu.jby.repository;

import com.zjgsu.jby.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 用户数据访问层。所有 SQL 由 Spring Data JPA 依据方法名生成。
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByStudentId(String studentId);

    boolean existsByStudentId(String studentId);
}
