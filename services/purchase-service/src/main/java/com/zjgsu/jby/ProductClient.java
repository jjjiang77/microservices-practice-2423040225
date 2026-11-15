package com.zjgsu.jby;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-service")
public interface ProductClient {
    @PostMapping("/api/products/{productId}/purchase")
    ResponseEntity<Map<String, Object>> purchase(
            @PathVariable("productId") Long productId, @RequestBody Buyer buyer);

    record Buyer(Long buyerId) {}
}
