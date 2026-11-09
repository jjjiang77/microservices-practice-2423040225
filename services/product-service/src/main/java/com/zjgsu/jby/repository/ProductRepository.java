package com.zjgsu.jby.repository;

import com.zjgsu.jby.entity.Product;
import com.zjgsu.jby.entity.ProductCategory;
import com.zjgsu.jby.entity.ProductStatus;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 商品数据访问层。支持按状态、分类、卖家分页查询，以及重复标题校验。
 */
public interface ProductRepository extends JpaRepository<Product, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Product> {

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByCategory(ProductCategory category, Pageable pageable);

    Page<Product> findByStatusAndCategory(ProductStatus status, ProductCategory category, Pageable pageable);

    /** sellerId 为跨服务用户标识，不建立 JPA 用户关联 */
    Page<Product> findBySellerId(Long sellerId, Pageable pageable);

    /** 判断某卖家是否已存在处于指定状态的同标题商品，用于重复发布校验 */
    // Spring Data 根据方法名生成查询，用于判断卖家是否已发布同标题的未售商品。
    boolean existsBySellerIdAndTitleAndStatusIn(Long sellerId, String title, Collection<ProductStatus> statuses);
}
