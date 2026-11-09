package com.zjgsu.jby.service;

import com.zjgsu.jby.dto.CreateProductRequest;
import com.zjgsu.jby.dto.PageResponse;
import com.zjgsu.jby.dto.ProductResponse;
import com.zjgsu.jby.entity.Product;
import com.zjgsu.jby.entity.ProductCategory;
import com.zjgsu.jby.entity.ProductStatus;
import com.zjgsu.jby.exception.BusinessRuleException;
import com.zjgsu.jby.exception.DuplicateResourceException;
import com.zjgsu.jby.exception.ResourceNotFoundException;
import com.zjgsu.jby.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品业务逻辑：发布 → 审核上架 → 浏览 → 下单/下架。
 * 微服务拆分后，通过 UserClient 以 HTTP 方式跨服务获取卖家/买家信息，
 * 不再直接依赖 UserRepository（服务边界解耦）。
 */
@Service
public class ProductService {

    public static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    public static final BigDecimal MAX_PRICE = new BigDecimal("2000.00");
    public static final int MIN_CREDIT_SCORE = 60;
    private static final List<ProductStatus> UNFINISHED_STATUSES =
            List.of(ProductStatus.PENDING, ProductStatus.ON_SALE);
    private static final int MAX_PAGE_SIZE = 50;

    private final ProductRepository productRepository;
    /** 微服务拆分：不再直接依赖 UserRepository，改用 HTTP 客户端 */
    private final UserClient userClient;

    public ProductService(ProductRepository productRepository, UserClient userClient) {
        this.productRepository = productRepository;
        this.userClient = userClient;
    }

    // 事务管理本次商品库写入，远程用户查询拥有独立的服务边界。
    @Transactional
    public ProductResponse publish(CreateProductRequest request) {
        // 跨服务调 user-service 查卖家
        Map<String, Object> seller = requireUser(request.sellerId(), "卖家");
        int creditScore = ((Number) seller.get("creditScore")).intValue();
        if (creditScore < MIN_CREDIT_SCORE) {
            throw new BusinessRuleException(
                    "SELLER_CREDIT_TOO_LOW",
                    "卖家信用分 " + creditScore + " 低于门槛 " + MIN_CREDIT_SCORE + "，暂不允许发布商品");
        }

        BigDecimal price = request.price();
        if (price.compareTo(MIN_PRICE) < 0 || price.compareTo(MAX_PRICE) > 0) {
            throw new BusinessRuleException(
                    "PRICE_OUT_OF_RANGE", "商品价格须在 " + MIN_PRICE + " ~ " + MAX_PRICE + " 元之间，当前为 " + price);
        }

        String title = request.title().trim();
        if (productRepository.existsBySellerIdAndTitleAndStatusIn(request.sellerId(), title, UNFINISHED_STATUSES)) {
            throw new DuplicateResourceException("该卖家已存在同标题的未售出商品: " + title);
        }

        // 业务检查通过后再创建商品，保存卖家编号和发布时的昵称。
        Product product = new Product(
                title, request.description(), price, request.category(), request.conditionLevel(),
                request.sellerId(), (String) seller.get("nickname"));
        // JPA 将实体写入 t_product，再转换成接口返回对象。
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return ProductResponse.from(requireProduct(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(
            ProductStatus status, ProductCategory category, Long sellerId, int page, int size) {
        // 限制每页条数，默认按创建时间倒序。
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        // 所有可选条件共同生效，避免 sellerId 覆盖状态与分类过滤。
        org.springframework.data.jpa.domain.Specification<Product> filters = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (sellerId != null) predicates.add(cb.equal(root.get("sellerId"), sellerId));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (category != null) predicates.add(cb.equal(root.get("category"), category));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        Page<Product> result = productRepository.findAll(filters, pageable);
        return PageResponse.from(result.map(ProductResponse::from));
    }

    @Transactional
    public ProductResponse approve(Long id) {
        Product product = requireProduct(id);
        requireStatus(product, ProductStatus.PENDING, "审核上架");
        product.setStatus(ProductStatus.ON_SALE);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse purchase(Long productId, Long buyerId) {
        Product product = requireProduct(productId);
        // 跨服务调 user-service 查买家
        Map<String, Object> buyer = requireUser(buyerId, "买家");

        if (product.getSellerId().equals(buyerId)) {
            throw new BusinessRuleException("SELF_PURCHASE_NOT_ALLOWED", "不能购买自己发布的商品");
        }
        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw new BusinessRuleException(
                    "PRODUCT_NOT_ON_SALE", "商品当前状态为 " + product.getStatus() + "，不可购买");
        }
        int buyerCredit = ((Number) buyer.get("creditScore")).intValue();
        if (buyerCredit < MIN_CREDIT_SCORE) {
            throw new BusinessRuleException(
                    "BUYER_CREDIT_TOO_LOW",
                    "买家信用分 " + buyerCredit + " 低于门槛 " + MIN_CREDIT_SCORE + "，暂不允许下单");
        }

        // 当前只改变商品状态。顺序重复购买会被状态检查拦截，并发竞争仍需原子更新。
        product.setStatus(ProductStatus.SOLD);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse takeOffShelf(Long id) {
        Product product = requireProduct(id);
        requireStatus(product, ProductStatus.ON_SALE, "下架");
        product.setStatus(ProductStatus.OFF_SHELF);
        return ProductResponse.from(productRepository.save(product));
    }

    private Product requireProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在: id=" + id));
    }

    /** 微服务拆分：通过 HTTP 调 user-service 获取用户 */
    private Map<String, Object> requireUser(Long id, String roleName) {
        Map<String, Object> user = userClient.findById(id);
        if (user == null) {
            throw new ResourceNotFoundException(roleName + "不存在: id=" + id);
        }
        return user;
    }

    private void requireStatus(Product product, ProductStatus expected, String action) {
        if (product.getStatus() != expected) {
            throw new BusinessRuleException(
                    "INVALID_STATUS_TRANSITION",
                    "商品当前状态为 " + product.getStatus() + "，无法执行" + action + "，要求状态为 " + expected);
        }
    }
}
