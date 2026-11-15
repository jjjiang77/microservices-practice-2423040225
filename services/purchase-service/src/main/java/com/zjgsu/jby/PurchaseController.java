package com.zjgsu.jby;

import feign.FeignException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {
    private final ProductClient products;

    public PurchaseController(ProductClient products) {
        this.products = products;
    }

    public record Request(Long productId, Long buyerId) {}

    @PostMapping
    public ResponseEntity<Map<String, Object>> purchase(@RequestBody Request request) {
        try {
            var response = products.purchase(request.productId(), new ProductClient.Buyer(request.buyerId()));
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
