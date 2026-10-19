package com.zjgsu.jby.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 购买商品的请求体：买家 ID。
 */
public record PurchaseRequest(@NotNull(message = "买家 ID 不能为空") Long buyerId) {
}
