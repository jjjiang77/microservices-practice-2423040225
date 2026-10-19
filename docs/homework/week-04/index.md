# Week 04 作业文档 — 单体应用与数据库持久化

本周在 `monolith/` 中实现"商小淘"选题的一条完整业务流程：**商品发布与浏览**，包含模型、Service、REST API、业务规则与 MySQL 持久化。

## 一、业务建模

### 实现思路

1. 从第 03 周提案的"优先实现业务场景"（商品发布与浏览）出发，确定本周打通的链路：
   卖家发布商品 → 校验业务规则 → 落库为待审核 PENDING → 审核上架 ON_SALE → 买家浏览/查看详情 → 下单 SOLD（或卖家下架 OFF_SHELF）。
2. 抽象出参与该流程的两个业务实体：`User`（用户/学生）与 `Product`（商品），关系为 **User 1 — N Product**（`t_product.seller_id` 非空外键指向 `t_user.id`）。
3. 把选题特有的约束沉淀为 Service 中的业务规则（价格区间、信用分门槛、重复发布、状态流转、不能自购），每条规则都有对应测试。

设计文档：[docs/project-proposal.md](../../project-proposal.md)、[docs/database.md](../../database.md)。

### 关键结论

- 两个实体足以支撑该链路，且关系方向清晰（商品是关系拥有端，卖家不可为空）。
- 业务规则集中在 Service 层，Controller 只做 HTTP 输入输出，便于后续周次拆分微服务时迁移。

## 二、单体应用实现

### 实现思路

在 `com.zjgsu.jby` 基础包下分层：

| 层 | 包 | 内容 |
|----|----|------|
| 模型 | `entity` | `User`、`Product`、`UserRole`、`ProductCategory`、`ProductCondition`、`ProductStatus` |
| 数据访问 | `repository` | `UserRepository`、`ProductRepository`（Spring Data JPA 派生查询） |
| 业务 | `service` | `UserService`（注册/查询）、`ProductService`（6 条业务规则） |
| 接口 | `controller` | `UserController`、`ProductController`（仅解析请求、调用 Service、返回状态码） |
| 传输对象 | `dto` | `CreateUserRequest`、`UserResponse`、`CreateProductRequest`、`ProductResponse`、`PurchaseRequest`、`PageResponse`、`ApiErrorResponse` |
| 异常 | `exception` | `ResourceNotFoundException`(404)、`DuplicateResourceException`(409)、`BusinessRuleException`(422)、`GlobalExceptionHandler` |

### REST 接口（路径前缀 `/api`）

| 方法 | 路径 | 说明 | 状态码 |
|------|------|------|--------|
| POST | `/api/users` | 注册学生，成功后 `Location: /api/users/{id}` | 201 / 400 / 409 |
| GET | `/api/users/{id}` | 查询用户 | 200 / 404 |
| POST | `/api/products` | 发布商品（固定 PENDING） | 201 / 400 / 404 / 409 / 422 |
| GET | `/api/products` | 分页浏览，支持 `status`、`category`、`sellerId` | 200 / 400 |
| GET | `/api/products/{id}` | 商品详情 | 200 / 404 |
| POST | `/api/products/{id}/approve` | 审核上架 PENDING → ON_SALE | 200 / 404 / 422 |
| POST | `/api/products/{id}/purchase` | 买家下单 ON_SALE → SOLD | 200 / 404 / 422 |
| POST | `/api/products/{id}/off-shelf` | 卖家下架 ON_SALE → OFF_SHELF | 200 / 404 / 422 |

### 运行命令

```bash
cd monolith
JAVA_HOME=<JDK 25+> ./mvnw spring-boot:run    # 启动（需先完成第三节的 MySQL 建库）
curl -i http://localhost:8080/api/products
```

### 结果

`POST /api/products` 返回 201，响应体 `status` 为 `PENDING`，`Location` 指向新建商品；`GET /api/products?status=ON_SALE` 返回分页结构（`content`/`page`/`size`/`totalElements`/`totalPages`）；业务规则违反时返回 422 并携带规则编码（如 `PRICE_OUT_OF_RANGE`）。

截图见 `screenshots/`（启动日志、curl 请求与响应）。

## 三、数据库持久化

### 实现思路

1. 接入 MySQL：`pom.xml` 增加 `spring-boot-starter-data-jpa` 与 `mysql-connector-j`，`application.yml` 配置默认数据源 `jdbc:mysql://localhost:3306/shangxiaotao`，`ddl-auto=update`。
2. 表结构以 SQL 脚本为准设计并显式声明约束：`monolith/sql/schema.sql`（主键、`uk_user_student_id` 唯一约束、`fk_product_seller` 外键、`ck_product_price_positive` CHECK、3 个查询索引）。
3. 数据访问全部放在 `repository` 包，使用 `JpaRepository` + 方法名派生查询（如 `findByStatus`、`existsBySellerIdAndTitleAndStatusIn`）。
4. API 输入统一由 DTO 承载（`@Valid` 校验格式），业务规则在 Service 校验。
5. 测试数据准备方式：
   - MySQL 联调：`monolith/sql/data.sql`，`INSERT IGNORE` + 固定主键，可重复执行；
   - 自动化测试：H2 内存库 + 测试代码 `@BeforeEach` 造数，事务回滚保证可重复。

### 运行命令

```bash
brew install mysql && brew services start mysql
mysql -u root < monolith/sql/schema.sql
mysql -u root < monolith/sql/data.sql
mysql -u root -e "CREATE USER IF NOT EXISTS 'shangxiaotao'@'localhost' IDENTIFIED BY 'shangxiaotao'; GRANT ALL PRIVILEGES ON shangxiaotao.* TO 'shangxiaotao'@'localhost'; FLUSH PRIVILEGES;"
mysql -u shangxiaotao -pshangxiaotao -D shangxiaotao -e "SHOW TABLES; SELECT id,title,status,seller_id FROM t_product;"
```

### 结果

`SHOW TABLES` 返回 `t_user`、`t_product`；启动应用后通过 API 发布的商品可在 MySQL 中查到对应行，`status` 随 approve/purchase 流转而更新（`PENDING → ON_SALE → SOLD`）。

截图见 `screenshots/`（建表结果、表数据、应用连接 MySQL 后接口返回的持久化数据）。

## 四、测试与验证

### 实现思路

三类测试分别覆盖题目要求的三个验证点，全部使用 H2（`application-test.yml`），`./mvnw test` 自动准备数据、可重复通过：

| 测试类 | 覆盖内容 |
|--------|----------|
| `repository/UserRepositoryTest` | Repository 读写：保存/查询用户、学号唯一约束触发 `DataIntegrityViolationException`、商品按卖家/状态/分类分页查询 |
| `service/ProductServiceTest` | Service 业务规则：价格区间、卖家信用分、重复标题、状态流转、自购限制、买家信用分、资源不存在 |
| `controller/ProductApiTest` | API 正常与错误响应：201/400/404/409/422，分页查询、详情查询、下单成功与自购被拒 |

### 运行命令

```bash
cd monolith
JAVA_HOME=<JDK 25+> ./mvnw test
```

### 结果

```
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

其中 `UserRepositoryTest` 3 个、`ProductServiceTest` 11 个、`ProductApiTest` 11 个、`ApplicationTests` 1 个。

> 注意：项目 `java.version=25`，本机需用 JDK 25 及以上作为 `JAVA_HOME` 运行 `./mvnw`（本机默认 `java` 为 17 会导致测试 JVM 无法加载 class 文件）。

截图见 `screenshots/`（`./mvnw test` 通过结果）。

## 五、截图清单

| 文件命名建议 | 对应小题 | 内容 |
|--------------|----------|------|
| `01-app-start.png` | 2 单体应用实现 | `./mvnw spring-boot:run` 启动成功、Tomcat 端口 8080 |
| `02-api-create-list.png` | 2 单体应用实现 | `curl` 发布商品 201 + 查询列表 200 |
| `03-api-rule-422.png` | 2 单体应用实现 / 4 测试 | 价格超上限返回 422 `PRICE_OUT_OF_RANGE` |
| `04-mysql-tables-data.png` | 3 数据库持久化 | MySQL 中 `SHOW TABLES` 与 `t_product` 数据 |
| `05-mvn-test.png` | 4 测试与验证 | `./mvnw test` → `Tests run: 26, Failures: 0, Errors: 0` |
