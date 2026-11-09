package com.zjgsu.jby.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zjgsu.jby.entity.Product;
import com.zjgsu.jby.entity.ProductCategory;
import com.zjgsu.jby.entity.ProductCondition;
import com.zjgsu.jby.entity.ProductStatus;
import com.zjgsu.jby.entity.User;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository 层读写测试：验证 Spring Data JPA 派生查询与数据库约束。
 * 使用 H2 内存库（application-test.yml），每个测试方法结束自动回滚，可重复执行。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("保存用户后可按 id、学号查询，并校验学号是否存在")
    void saveAndFindUser() {
        User saved = userRepository.save(new User("2423110001", "小商", "计算机科学与技术学院"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(userRepository.findById(saved.getId()))
                .isPresent()
                .get()
                .extracting(User::getNickname)
                .isEqualTo("小商");
        assertThat(userRepository.findByStudentId("2423110001")).isPresent();
        assertThat(userRepository.existsByStudentId("2423110001")).isTrue();
        assertThat(userRepository.existsByStudentId("2423119999")).isFalse();
    }

    @Test
    @DisplayName("同一学号重复写入触发数据库唯一约束")
    void studentIdIsUnique() {
        userRepository.saveAndFlush(new User("2423110002", "用户A", "统计与数学学院"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("2423110002", "用户B", "统计与数学学院")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("可按卖家、状态、分类分页查询商品，并判断同卖家同标题是否存在")
    void queryProducts() {
        User seller = userRepository.save(new User("2423110003", "卖家小李", "计算机科学与技术学院"));
        productRepository.saveAndFlush(new Product(
                "数据结构教材", "九成新，无笔记", new BigDecimal("25.00"),
                ProductCategory.TEXTBOOK, ProductCondition.LIKE_NEW, seller));

        Product onSale = new Product(
                "宿舍小冰箱", "毕业清仓", new BigDecimal("300.00"),
                ProductCategory.LARGE_ITEM, ProductCondition.GOOD, seller);
        onSale.setStatus(ProductStatus.ON_SALE);
        productRepository.saveAndFlush(onSale);

        var bySeller = productRepository.findBySellerId(
                seller.getId(), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));
        assertThat(bySeller.getTotalElements()).isEqualTo(2);

        assertThat(productRepository.findByStatus(ProductStatus.ON_SALE, PageRequest.of(0, 10)).getContent())
                .singleElement()
                .extracting(Product::getTitle)
                .isEqualTo("宿舍小冰箱");

        assertThat(productRepository
                        .findByStatusAndCategory(ProductStatus.PENDING, ProductCategory.TEXTBOOK, PageRequest.of(0, 10))
                        .getTotalElements())
                .isEqualTo(1);

        assertThat(productRepository.existsBySellerIdAndTitleAndStatusIn(
                        seller.getId(), "数据结构教材", List.of(ProductStatus.PENDING)))
                .isTrue();
        assertThat(productRepository.existsBySellerIdAndTitleAndStatusIn(
                        seller.getId(), "数据结构教材", List.of(ProductStatus.SOLD)))
                .isFalse();
    }
}
