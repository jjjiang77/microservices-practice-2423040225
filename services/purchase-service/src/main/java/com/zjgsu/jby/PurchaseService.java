package com.zjgsu.jby;

import feign.FeignException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 购买业务：通过 OpenFeign 调用商品服务完成跨服务购买，并把远程错误映射为清晰的 HTTP 状态。
 */
@Service
public class PurchaseService {

    private final ProductClient products;

    public PurchaseService(ProductClient products) {
        this.products = products;
    }

    public ResponseEntity<Map<String, Object>> purchase(Long productId, Long buyerId) {
        try {
            var response = products.purchase(productId, new ProductClient.Buyer(buyerId));
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (FeignException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "商品或买家不存在", ex);
        } catch (FeignException ex) {
            if (ex.status() >= 400 && ex.status() < 500) {
                throw new ResponseStatusException(HttpStatusCode.valueOf(ex.status()), "购买请求被拒绝", ex);
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "商品服务调用失败，请先查询购买结果", ex);
        }
    }
}
