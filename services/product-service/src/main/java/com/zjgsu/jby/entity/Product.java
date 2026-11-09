package com.zjgsu.jby.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品（二手物品）。微服务拆分后，Product 不再直接关联 User 实体，
 * 只保存 sellerId（外键 id）和 sellerNickname（冗余），
 * 跨服务查询通过 user-service 的 HTTP 接口完成。
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

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 24)
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_level", nullable = false, length = 16)
    private ProductCondition conditionLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ProductStatus status = ProductStatus.PENDING;

    /** 微服务拆分：只存 sellerId + sellerNickname，不直接关联 User 实体 */
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "seller_nickname", length = 64)
    private String sellerNickname;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Product() {}

    public Product(
            String title,
            String description,
            BigDecimal price,
            ProductCategory category,
            ProductCondition conditionLevel,
            Long sellerId,
            String sellerNickname) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.conditionLevel = conditionLevel;
        this.sellerId = sellerId;
        this.sellerNickname = sellerNickname;
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

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public ProductCategory getCategory() { return category; }
    public ProductCondition getConditionLevel() { return conditionLevel; }
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }
    public Long getSellerId() { return sellerId; }
    public String getSellerNickname() { return sellerNickname; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
