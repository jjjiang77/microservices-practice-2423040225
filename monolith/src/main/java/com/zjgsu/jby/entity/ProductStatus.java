package com.zjgsu.jby.entity;

/**
 * 商品状态流转：PENDING（待审核）→ ON_SALE（在售）→ SOLD（已售）
 *                                        ↘ OFF_SHELF（已下架）
 */
public enum ProductStatus {
    /** 待审核，卖家刚发布，未对买家展示 */
    PENDING,
    /** 在售，审核通过后对买家展示 */
    ON_SALE,
    /** 已售，买家已下单购买 */
    SOLD,
    /** 已下架，卖家主动撤回 */
    OFF_SHELF
}
