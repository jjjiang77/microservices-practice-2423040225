package com.zjgsu.jby.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品（二手物品）。每个商品归属一个卖家用户（N:1）。
 */
@Entity
@Table(
        name = "t_product",
        indexes = {
                @Index(name = "idx_product_status", columnList = "status"),
                @Index(name = "idx_product_seller_id", columnList = "seller_id")
        })
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 60)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    /** 商品价格，单位元，最多 8 位整数 + 2 位小数 */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 24)
    private ProductCategory category;

    /** 列名用 condition_level，避免与 MySQL 保留字 CONDITION 冲突 */
    @Enumerated(EnumType.STRING)
    @Column(name = "condition_level", nullable = false, length = 16)
    private ProductCondition conditionLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ProductStatus status = ProductStatus.PENDING;

    @ManyToOne(optional = false)
    @JoinColumn(name = "seller_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_seller"))
    private User seller;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Product() {
        // JPA 需要无参构造
    }

    public Product(
            String title,
            String description,
            BigDecimal price,
            ProductCategory category,
            ProductCondition conditionLevel,
            User seller) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.conditionLevel = conditionLevel;
        this.seller = seller;
        this.status = ProductStatus.PENDING;
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

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public ProductCondition getConditionLevel() {
        return conditionLevel;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public User getSeller() {
        return seller;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
