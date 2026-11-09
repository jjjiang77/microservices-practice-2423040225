package com.zjgsu.jby.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.zjgsu.jby.dto.CreateProductRequest;
import com.zjgsu.jby.dto.ProductResponse;
import com.zjgsu.jby.entity.ProductCategory;
import com.zjgsu.jby.entity.ProductCondition;
import com.zjgsu.jby.entity.ProductStatus;
import com.zjgsu.jby.entity.User;
import com.zjgsu.jby.exception.BusinessRuleException;
import com.zjgsu.jby.exception.DuplicateResourceException;
import com.zjgsu.jby.exception.ResourceNotFoundException;
import com.zjgsu.jby.repository.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service 层业务规则测试：覆盖价格区间、信用分门槛、重复发布、状态流转、自购限制等选题规则。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private UserRepository userRepository;

    private User seller;

    @BeforeEach
    void setUp() {
        seller = userRepository.save(new User("2423220001", "卖家小李", "计算机科学与技术学院"));
    }

    @Test
    @DisplayName("发布商品：状态为待审核 PENDING，价格与卖家正确落库")
    void publishCreatesPendingProduct() {
        ProductResponse created = productService.publish(request("考研数学复习全书", "45.00"));

        assertThat(created.id()).isNotNull();
        assertThat(created.status()).isEqualTo(ProductStatus.PENDING.name());
        assertThat(created.price()).isEqualByComparingTo(new BigDecimal("45.00"));
        assertThat(created.sellerId()).isEqualTo(seller.getId());
        assertThat(created.category()).isEqualTo(ProductCategory.TEXTBOOK.name());
    }

    @Test
    @DisplayName("业务规则：价格超过校园二手上限 2000 元被拒绝")
    void publishRejectsPriceAboveUpperBound() {
        BusinessRuleException ex = expectBusinessRule(() -> productService.publish(request("电瓶车", "5000.00")));

        assertThat(ex.getCode()).isEqualTo("PRICE_OUT_OF_RANGE");
        assertThat(ex.getMessage()).contains("2000.00");
    }

    @Test
    @DisplayName("业务规则：价格必须大于 0")
    void publishRejectsNonPositivePrice() {
        BusinessRuleException ex = expectBusinessRule(() -> productService.publish(request("免费送", "0.00")));

        assertThat(ex.getCode()).isEqualTo("PRICE_OUT_OF_RANGE");
    }

    @Test
    @DisplayName("业务规则：卖家信用分低于 60 不允许发布商品")
    void publishRejectsLowCreditSeller() {
        User lowCreditSeller = userRepository.save(new User("2423220002", "低信用同学", "环境科学与工程学院", 40));

        BusinessRuleException ex = expectBusinessRule(
                () -> productService.publish(request("机械键盘", "120.00", lowCreditSeller.getId())));

        assertThat(ex.getCode()).isEqualTo("SELLER_CREDIT_TOO_LOW");
    }

    @Test
    @DisplayName("业务规则：同一卖家不能重复发布同标题的未售出商品")
    void publishRejectsDuplicateTitle() {
        productService.publish(request("考研英语真题", "30.00"));

        DuplicateResourceException ex = expectDuplicate(
                () -> productService.publish(request("考研英语真题", "35.00")));

        assertThat(ex.getMessage()).contains("考研英语真题");
    }

    @Test
    @DisplayName("业务规则：仅待审核商品可上架，重复上架被拒绝")
    void approveMovesPendingToOnSaleAndRejectsSecondApprove() {
        ProductResponse pending = productService.publish(request("四六级词汇书", "18.00"));

        ProductResponse onSale = productService.approve(pending.id());
        assertThat(onSale.status()).isEqualTo(ProductStatus.ON_SALE.name());

        BusinessRuleException ex = expectBusinessRule(() -> productService.approve(pending.id()));
        assertThat(ex.getCode()).isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    @DisplayName("买家下单：在售商品变为已售")
    void purchaseMovesOnSaleToSold() {
        User buyer = userRepository.save(new User("2423220003", "买家小王", "统计与数学学院"));
        ProductResponse onSale = publishOnSale("宿舍小风扇", "35.00");

        ProductResponse sold = productService.purchase(onSale.id(), buyer.getId());

        assertThat(sold.status()).isEqualTo(ProductStatus.SOLD.name());
    }

    @Test
    @DisplayName("业务规则：买家不能购买自己发布的商品")
    void purchaseRejectsSelfPurchase() {
        ProductResponse onSale = publishOnSale("自己的闲置", "50.00");

        BusinessRuleException ex = expectBusinessRule(() -> productService.purchase(onSale.id(), seller.getId()));

        assertThat(ex.getCode()).isEqualTo("SELF_PURCHASE_NOT_ALLOWED");
    }

    @Test
    @DisplayName("业务规则：待审核（未上架）商品不可被购买")
    void purchaseRejectsProductNotOnSale() {
        User buyer = userRepository.save(new User("2423220004", "买家小张", "统计与数学学院"));
        ProductResponse pending = productService.publish(request("未上架商品", "20.00"));

        BusinessRuleException ex = expectBusinessRule(() -> productService.purchase(pending.id(), buyer.getId()));

        assertThat(ex.getCode()).isEqualTo("PRODUCT_NOT_ON_SALE");
    }

    @Test
    @DisplayName("业务规则：已售商品不能下架")
    void takeOffShelfRejectsSoldProduct() {
        User buyer = userRepository.save(new User("2423220005", "买家小陈", "统计与数学学院"));
        ProductResponse onSale = publishOnSale("二手台灯", "40.00");
        productService.purchase(onSale.id(), buyer.getId());

        BusinessRuleException ex = expectBusinessRule(() -> productService.takeOffShelf(onSale.id()));

        assertThat(ex.getCode()).isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    @DisplayName("查询不存在的商品抛出 ResourceNotFoundException")
    void getByIdThrowsWhenMissing() {
        assertThat(expectNotFound(() -> productService.getById(999999L)).getMessage()).contains("999999");
    }

    private ProductResponse publishOnSale(String title, String price) {
        ProductResponse pending = productService.publish(request(title, price));
        return productService.approve(pending.id());
    }

    private CreateProductRequest request(String title, String price) {
        return request(title, price, seller.getId());
    }

    private CreateProductRequest request(String title, String price, Long sellerId) {
        return new CreateProductRequest(
                title, "校园面交，详聊", new BigDecimal(price), ProductCategory.TEXTBOOK,
                ProductCondition.LIKE_NEW, sellerId);
    }

    private static BusinessRuleException expectBusinessRule(Runnable action) {
        try {
            action.run();
        } catch (BusinessRuleException ex) {
            return ex;
        } catch (RuntimeException ex) {
            throw new AssertionError(
                    "预期 BusinessRuleException，实际为 " + ex.getClass().getSimpleName() + ": " + ex.getMessage(), ex);
        }
        throw new AssertionError("预期抛出 BusinessRuleException，但没有抛出");
    }

    private static DuplicateResourceException expectDuplicate(Runnable action) {
        try {
            action.run();
        } catch (DuplicateResourceException ex) {
            return ex;
        } catch (RuntimeException ex) {
            throw new AssertionError(
                    "预期 DuplicateResourceException，实际为 " + ex.getClass().getSimpleName() + ": " + ex.getMessage(), ex);
        }
        throw new AssertionError("预期抛出 DuplicateResourceException，但没有抛出");
    }

    private static ResourceNotFoundException expectNotFound(Runnable action) {
        try {
            action.run();
        } catch (ResourceNotFoundException ex) {
            return ex;
        } catch (RuntimeException ex) {
            throw new AssertionError(
                    "预期 ResourceNotFoundException，实际为 " + ex.getClass().getSimpleName() + ": " + ex.getMessage(), ex);
        }
        throw new AssertionError("预期抛出 ResourceNotFoundException，但没有抛出");
    }
}
