package com.zjgsu.jby.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zjgsu.jby.entity.Product;
import com.zjgsu.jby.entity.User;
import com.zjgsu.jby.repository.ProductRepository;
import com.zjgsu.jby.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * API 层测试：验证路由、请求响应体与 HTTP 状态码（正常 + 错误响应）。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductApiTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private MockMvc mockMvc;
    private User seller;
    private User buyer;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        seller = userRepository.save(new User("2423330001", "卖家小李", "计算机科学与技术学院"));
        buyer = userRepository.save(new User("2423330002", "买家小王", "统计与数学学院"));
    }

    @Test
    @DisplayName("POST /api/users 注册成功返回 201 与 Location")
    void registerUserReturns201() throws Exception {
        var result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":"2423330100","nickname":"小商","department":"计算机科学与技术学院"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.studentId").value("2423330100"))
                .andExpect(jsonPath("$.creditScore").value(100))
                .andReturn();

        assertThat(result.getResponse().getHeader("Location")).startsWith("/api/users/");
    }

    @Test
    @DisplayName("POST /api/users 学号重复返回 409")
    void registerUserDuplicateReturns409() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":"2423330001","nickname":"重复学号","department":"统计与数学学院"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("POST /api/users 昵称为空返回 400")
    void registerUserWithBlankNicknameReturns400() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":"2423330200","nickname":"","department":"统计与数学学院"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("POST /api/products 发布成功返回 201，状态为 PENDING")
    void publishProductReturns201() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishBody("考研数学复习全书", "45.00", seller.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.sellerNickname").value("卖家小李"))
                .andExpect(jsonPath("$.price").value(45.00));
    }

    @Test
    @DisplayName("POST /api/products 价格超上限返回 422 与业务规则编码")
    void publishProductWithTooHighPriceReturns422() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishBody("高端电瓶车", "5000.00", seller.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PRICE_OUT_OF_RANGE"));
    }

    @Test
    @DisplayName("GET /api/products?status=ON_SALE 返回审核上架后的商品")
    void listOnSaleProducts() throws Exception {
        Long productId = publishAndApprove("宿舍小冰箱", "300.00");

        mockMvc.perform(get("/api/products").param("status", "ON_SALE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(productId))
                .andExpect(jsonPath("$.content[0].status").value("ON_SALE"));
    }

    @Test
    @DisplayName("GET /api/products/{id} 返回商品详情")
    void getProductDetailReturns200() throws Exception {
        Long productId = publishAndApprove("二手自行车", "500.00");

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.category").value("TEXTBOOK"))
                .andExpect(jsonPath("$.sellerId").value(seller.getId()));
    }

    @Test
    @DisplayName("GET /api/products/{id} 商品不存在返回 404")
    void getMissingProductReturns404() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/products?status=UNKNOWN 参数非法返回 400")
    void listWithInvalidStatusReturns400() throws Exception {
        mockMvc.perform(get("/api/products").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("POST /api/products/{id}/purchase 购买自己的商品返回 422")
    void purchaseOwnProductReturns422() throws Exception {
        Long productId = publishAndApprove("自己的闲置", "60.00");

        mockMvc.perform(post("/api/products/{id}/purchase", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"buyerId":%d}
                                """.formatted(seller.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SELF_PURCHASE_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("POST /api/products/{id}/purchase 他人购买成功后商品变为 SOLD")
    void purchaseByOtherBuyerReturnsSold() throws Exception {
        Long productId = publishAndApprove("二手台灯", "40.00");

        mockMvc.perform(post("/api/products/{id}/purchase", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"buyerId":%d}
                                """.formatted(buyer.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD"));
    }

    /** 通过 API 发布商品并审核上架，返回商品 id（自增 id 从数据库读回，不额外引入 JSON 解析依赖） */
    private Long publishAndApprove(String title, String price) throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishBody(title, price, seller.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(title));

        Long productId = productRepository
                .findBySellerId(seller.getId(), PageRequest.of(0, 10))
                .getContent()
                .stream()
                .filter(product -> product.getTitle().equals(title))
                .findFirst()
                .map(Product::getId)
                .orElseThrow(() -> new IllegalStateException("商品未写入数据库: " + title));

        mockMvc.perform(post("/api/products/{id}/approve", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ON_SALE"));

        return productId;
    }

    private String publishBody(String title, String price, Long sellerId) {
        return """
                {"title":"%s","description":"校园面交","price":%s,"category":"TEXTBOOK",
                 "conditionLevel":"LIKE_NEW","sellerId":%d}
                """.formatted(title, price, sellerId);
    }
}
