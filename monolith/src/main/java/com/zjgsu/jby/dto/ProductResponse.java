package com.zjgsu.jby.dto;

import com.zjgsu.jby.entity.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品响应体。价格等字段直接来自实体，卖家信息平铺为 sellerId / sellerNickname。
 */
public record ProductResponse(
        Long id,
        String title,
        String description,
        BigDecimal price,
        String category,
        String conditionLevel,
        String status,
        Long sellerId,
        String sellerNickname,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getTitle(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory().name(),
                product.getConditionLevel().name(),
                product.getStatus().name(),
                product.getSeller().getId(),
                product.getSeller().getNickname(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
