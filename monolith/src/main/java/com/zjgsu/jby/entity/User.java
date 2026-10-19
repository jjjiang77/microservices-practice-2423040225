package com.zjgsu.jby.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户（学生，买卖双方共用）。一个用户可以发布多个商品（1:N）。
 */
@Entity
@Table(
        name = "t_user",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_student_id", columnNames = "student_id"))
public class User {

    /** 新注册学生的初始信用分 */
    public static final int DEFAULT_CREDIT_SCORE = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 学号，校内身份标识，唯一 */
    @Column(name = "student_id", nullable = false, length = 20)
    private String studentId;

    @Column(name = "nickname", nullable = false, length = 32)
    private String nickname;

    @Column(name = "department", length = 40)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private UserRole role = UserRole.STUDENT;

    /** 信用分，低于发布/下单门槛时被业务规则拦截 */
    @Column(name = "credit_score", nullable = false)
    private Integer creditScore = DEFAULT_CREDIT_SCORE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 该用户发布的商品（反向关系，商品表持有外键） */
    @OneToMany(mappedBy = "seller", fetch = FetchType.LAZY)
    private List<Product> products = new ArrayList<>();

    protected User() {
        // JPA 需要无参构造
    }

    public User(String studentId, String nickname, String department) {
        this(studentId, nickname, department, DEFAULT_CREDIT_SCORE);
    }

    public User(String studentId, String nickname, String department, Integer creditScore) {
        this.studentId = studentId;
        this.nickname = nickname;
        this.department = department;
        this.creditScore = creditScore;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getDepartment() {
        return department;
    }

    public UserRole getRole() {
        return role;
    }

    public Integer getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(Integer creditScore) {
        this.creditScore = creditScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<Product> getProducts() {
        return products;
    }
}
