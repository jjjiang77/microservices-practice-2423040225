package com.zjgsu.jby;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {
    private final PurchaseService purchases;

    public PurchaseController(PurchaseService purchases) {
        this.purchases = purchases;
    }

    public record Request(Long productId, Long buyerId) {}

    @PostMapping
    public ResponseEntity<Map<String, Object>> purchase(@RequestBody Request request) {
        return purchases.purchase(request.productId(), request.buyerId());
    }
}
