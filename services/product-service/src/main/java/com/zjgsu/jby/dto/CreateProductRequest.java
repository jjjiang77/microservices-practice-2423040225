package com.zjgsu.jby.dto;

import com.zjgsu.jby.entity.ProductCategory;
import com.zjgsu.jby.entity.ProductCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 发布商品的请求体。价格区间等业务规则在 Service 中校验。
 */
public record CreateProductRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 60, message = "标题长度不能超过 60")
        String title,

        @Size(max = 500, message = "描述长度不能超过 500")
        String description,

        @NotNull(message = "价格不能为空")
        BigDecimal price,

        @NotNull(message = "分类不能为空")
        ProductCategory category,

        @NotNull(message = "新旧程度不能为空")
        ProductCondition conditionLevel,

        @NotNull(message = "卖家 ID 不能为空")
        Long sellerId) {
}
