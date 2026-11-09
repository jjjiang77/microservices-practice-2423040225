package com.zjgsu.jby.controller;

import com.zjgsu.jby.dto.CreateProductRequest;
import com.zjgsu.jby.dto.PageResponse;
import com.zjgsu.jby.dto.ProductResponse;
import com.zjgsu.jby.dto.PurchaseRequest;
import com.zjgsu.jby.entity.ProductCategory;
import com.zjgsu.jby.entity.ProductStatus;
import com.zjgsu.jby.service.ProductService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品接口。仅做 HTTP 输入输出，业务规则全部在 ProductService 中。
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /** 卖家发布商品，新商品状态为 PENDING */
    @PostMapping
    public ResponseEntity<ProductResponse> publish(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = productService.publish(request);
        return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
    }

    /** 分页浏览商品，支持 status / category / sellerId 过滤 */
    @GetMapping
    public PageResponse<ProductResponse> list(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return productService.search(status, category, sellerId, page, size);
    }

    /** 商品详情 */
    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    /** 审核上架：PENDING → ON_SALE */
    @PostMapping("/{id}/approve")
    public ProductResponse approve(@PathVariable Long id) {
        return productService.approve(id);
    }

    /** 买家下单：ON_SALE → SOLD */
    @PostMapping("/{id}/purchase")
    public ProductResponse purchase(@PathVariable Long id, @Valid @RequestBody PurchaseRequest request) {
        return productService.purchase(id, request.buyerId());
    }

    /** 卖家下架：ON_SALE → OFF_SHELF */
    @PostMapping("/{id}/off-shelf")
    public ProductResponse takeOffShelf(@PathVariable Long id) {
        return productService.takeOffShelf(id);
    }
}
