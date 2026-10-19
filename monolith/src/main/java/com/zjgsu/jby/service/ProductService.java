package com.zjgsu.jby.service;

import com.zjgsu.jby.dto.CreateProductRequest;
import com.zjgsu.jby.dto.PageResponse;
import com.zjgsu.jby.dto.ProductResponse;
import com.zjgsu.jby.entity.Product;
import com.zjgsu.jby.entity.ProductCategory;
import com.zjgsu.jby.entity.ProductStatus;
import com.zjgsu.jby.entity.User;
import com.zjgsu.jby.exception.BusinessRuleException;
import com.zjgsu.jby.exception.DuplicateResourceException;
import com.zjgsu.jby.exception.ResourceNotFoundException;
import com.zjgsu.jby.repository.ProductRepository;
import com.zjgsu.jby.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品业务逻辑：发布 → 审核上架 → 浏览 → 下单/下架。
 * 所有选题业务规则集中在 Service 层，Controller 只负责 HTTP 输入输出。
 */
@Service
public class ProductService {

    /** 校园二手单品价格下限（元） */
    public static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    /** 校园二手单品价格上限（元），限制校内闲置物品定价 */
    public static final BigDecimal MAX_PRICE = new BigDecimal("2000.00");
    /** 发布商品与下单所需的最低信用分 */
    public static final int MIN_CREDIT_SCORE = 60;
    /** 占用"同卖家同标题"名额的状态：未售出的商品 */
    private static final List<ProductStatus> UNFINISHED_STATUSES =
            List.of(ProductStatus.PENDING, ProductStatus.ON_SALE);
    /** 单页最大条数，防止一次拉取过多数据 */
    private static final int MAX_PAGE_SIZE = 50;

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ProductService(ProductRepository productRepository, UserRepository userRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    /**
     * 发布商品。业务规则：
     * 1) 卖家必须存在；2) 卖家信用分不低于 60；3) 价格在 [0.01, 2000] 元之间；
     * 4) 同一卖家不能存在同标题的未售出商品；5) 新商品状态固定为待审核 PENDING。
     */
    @Transactional
    public ProductResponse publish(CreateProductRequest request) {
        User seller = requireUser(request.sellerId(), "卖家");
        if (seller.getCreditScore() < MIN_CREDIT_SCORE) {
            throw new BusinessRuleException(
                    "SELLER_CREDIT_TOO_LOW",
                    "卖家信用分 " + seller.getCreditScore() + " 低于门槛 " + MIN_CREDIT_SCORE + "，暂不允许发布商品");
        }

        BigDecimal price = request.price();
        if (price.compareTo(MIN_PRICE) < 0 || price.compareTo(MAX_PRICE) > 0) {
            throw new BusinessRuleException(
                    "PRICE_OUT_OF_RANGE", "商品价格须在 " + MIN_PRICE + " ~ " + MAX_PRICE + " 元之间，当前为 " + price);
        }

        String title = request.title().trim();
        if (productRepository.existsBySellerIdAndTitleAndStatusIn(seller.getId(), title, UNFINISHED_STATUSES)) {
            throw new DuplicateResourceException("该卖家已存在同标题的未售出商品: " + title);
        }

        Product product = new Product(
                title, request.description(), price, request.category(), request.conditionLevel(), seller);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return ProductResponse.from(requireProduct(id));
    }

    /**
     * 分页浏览商品，可按状态、分类、卖家过滤，按发布时间倒序。
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(
            ProductStatus status, ProductCategory category, Long sellerId, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Product> result;
        if (sellerId != null) {
            result = productRepository.findBySellerId(sellerId, pageable);
        } else if (status != null && category != null) {
            result = productRepository.findByStatusAndCategory(status, category, pageable);
        } else if (status != null) {
            result = productRepository.findByStatus(status, pageable);
        } else if (category != null) {
            result = productRepository.findByCategory(category, pageable);
        } else {
            result = productRepository.findAll(pageable);
        }
        return PageResponse.from(result.map(ProductResponse::from));
    }

    /**
     * 审核上架。业务规则：仅"待审核"商品可上架，上架后状态为 ON_SALE。
     */
    @Transactional
    public ProductResponse approve(Long id) {
        Product product = requireProduct(id);
        requireStatus(product, ProductStatus.PENDING, "审核上架");
        product.setStatus(ProductStatus.ON_SALE);
        return ProductResponse.from(productRepository.save(product));
    }

    /**
     * 买家下单。业务规则：不能买自己发布的商品；商品必须处于在售状态；买家信用分不低于 60。
     */
    @Transactional
    public ProductResponse purchase(Long productId, Long buyerId) {
        Product product = requireProduct(productId);
        User buyer = requireUser(buyerId, "买家");

        if (product.getSeller().getId().equals(buyer.getId())) {
            throw new BusinessRuleException("SELF_PURCHASE_NOT_ALLOWED", "不能购买自己发布的商品");
        }
        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw new BusinessRuleException(
                    "PRODUCT_NOT_ON_SALE", "商品当前状态为 " + product.getStatus() + "，不可购买");
        }
        if (buyer.getCreditScore() < MIN_CREDIT_SCORE) {
            throw new BusinessRuleException(
                    "BUYER_CREDIT_TOO_LOW",
                    "买家信用分 " + buyer.getCreditScore() + " 低于门槛 " + MIN_CREDIT_SCORE + "，暂不允许下单");
        }

        product.setStatus(ProductStatus.SOLD);
        return ProductResponse.from(productRepository.save(product));
    }

    /**
     * 卖家下架。业务规则：仅在售商品可下架。
     */
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

    private User requireUser(Long id, String roleName) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(roleName + "不存在: id=" + id));
    }

    private void requireStatus(Product product, ProductStatus expected, String action) {
        if (product.getStatus() != expected) {
            throw new BusinessRuleException(
                    "INVALID_STATUS_TRANSITION",
                    "商品当前状态为 " + product.getStatus() + "，无法执行" + action + "，要求状态为 " + expected);
        }
    }
}
