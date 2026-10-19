# 数据库设计 — 商小淘 ShangXiaoTao

本文档对应 Week 04 实现的"商品发布与浏览"链路，数据库为 MySQL 8.0+，建库建表脚本见 [monolith/sql/schema.sql](../../monolith/sql/schema.sql)，可重复执行的测试数据见 [monolith/sql/data.sql](../../monolith/sql/data.sql)。

## 一、表关系

```
t_user (用户/学生)                 t_product (商品)
┌──────────────────┐              ┌──────────────────────────┐
│ id (PK)          │1            N│ id (PK)                  │
│ student_id (UK)  ├──────────────┤ seller_id (FK → t_user.id)│
│ nickname         │              │ title / price / category │
│ credit_score     │              │ condition_level / status │
└──────────────────┘              └──────────────────────────┘
```

- 一个用户（卖家）可以发布多个商品；一个商品必须且只能归属一个卖家。
- 关系通过 `t_product.seller_id` 非空外键维护，删除用户会被外键约束阻止，避免商品变成孤儿数据。
- JPA 映射：`User` 为 `@OneToMany(mappedBy = "seller")` 反向端，`Product.seller` 为 `@ManyToOne`（关系拥有端）。

## 二、表结构

### 1. t_user（用户表）

| 字段 | 类型 | 允许空 | 说明 |
|------|------|--------|------|
| id | BIGINT AUTO_INCREMENT | 否 | 主键 |
| student_id | VARCHAR(20) | 否 | 学号，校内身份唯一标识 |
| nickname | VARCHAR(32) | 否 | 昵称 |
| department | VARCHAR(40) | 是 | 院系 |
| role | VARCHAR(16) | 否 | 角色，默认 `STUDENT`；取值 `STUDENT` / `ADMIN` |
| credit_score | INT | 否 | 信用分，默认 100；低于 60 被业务规则拦截 |
| created_at | DATETIME(6) | 否 | 创建时间（`@PrePersist` 写入） |
| updated_at | DATETIME(6) | 否 | 更新时间（`@PreUpdate` 写入） |

约束与索引：

| 名称 | 类型 | 字段 | 作用 |
|------|------|------|------|
| PRIMARY KEY | 主键 | id | 唯一标识 |
| uk_user_student_id | 唯一约束 | student_id | 同一学号只能注册一次，数据库层兜底 |
| DEFAULT 100 | 默认值 | credit_score | 新用户初始信用分 |

### 2. t_product（商品表）

| 字段 | 类型 | 允许空 | 说明 |
|------|------|--------|------|
| id | BIGINT AUTO_INCREMENT | 否 | 主键 |
| title | VARCHAR(60) | 否 | 商品标题 |
| description | VARCHAR(500) | 是 | 商品描述 |
| price | DECIMAL(10,2) | 否 | 价格（元），金额使用定点数避免浮点误差 |
| category | VARCHAR(24) | 否 | 分类：`TEXTBOOK`/`ELECTRONICS`/`DAILY`/`LARGE_ITEM`/`CLOTHING` |
| condition_level | VARCHAR(16) | 否 | 新旧程度：`NEW`/`LIKE_NEW`/`GOOD`/`FAIR`（列名避开 MySQL 保留字 `CONDITION`） |
| status | VARCHAR(16) | 否 | 状态：`PENDING`/`ON_SALE`/`SOLD`/`OFF_SHELF`，默认 `PENDING` |
| seller_id | BIGINT | 否 | 卖家用户 id，外键指向 `t_user.id` |
| created_at | DATETIME(6) | 否 | 创建时间 |
| updated_at | DATETIME(6) | 否 | 更新时间 |

约束与索引：

| 名称 | 类型 | 字段 | 作用 |
|------|------|------|------|
| PRIMARY KEY | 主键 | id | 唯一标识 |
| fk_product_seller | 外键 | seller_id → t_user.id | 保证商品归属真实用户 |
| ck_product_price_positive | CHECK | price > 0 | 数据库层兜底价格为正 |
| idx_product_status | 普通索引 | status | 按状态浏览（在售列表） |
| idx_product_category | 普通索引 | category | 按分类浏览 |
| idx_product_seller_id | 普通索引 | seller_id | 按卖家查询"我的商品" |

> 枚举一律以字符串存储（`@Enumerated(EnumType.STRING)`），可读且不受枚举顺序变化影响。

## 三、建库与建表

```bash
cd monolith
mysql -u root < sql/schema.sql
```

`schema.sql` 使用 `CREATE DATABASE IF NOT EXISTS` / `CREATE TABLE IF NOT EXISTS`，可重复执行。

应用侧 `spring.jpa.hibernate.ddl-auto=update`：表结构以 `schema.sql` 的设计为准，Hibernate 仅在缺列时补充，不做破坏性变更。

## 四、测试数据准备（可重复执行）

| 场景 | 方式 | 说明 |
|------|------|------|
| 本地 MySQL 联调 | `monolith/sql/data.sql` | `INSERT IGNORE` + 固定主键，重复执行不报错、不产生重复数据。含 3 个用户（含 1 个信用分 40 的低信用用户）和 2 个商品（1 个在售、1 个待审核） |
| 自动化测试（H2） | 测试代码 `@BeforeEach` 造数 | 每个测试方法在事务中执行并回滚，H2 使用 `ddl-auto=create-drop`，因此 `./mvnw test` 可重复通过 |

```bash
cd monolith
mysql -u root < sql/data.sql          # 覆盖执行也不会重复插入
mysql -u root -e "SELECT id, student_id, nickname, credit_score FROM shangxiaotao.t_user;"
```

## 五、与 JPA 实体的对应关系

| 表 | 实体 | 位置 |
|----|------|------|
| t_user | `com.zjgsu.jby.entity.User` | monolith/src/main/java/com/zjgsu/jby/entity/User.java |
| t_product | `com.zjgsu.jby.entity.Product` | monolith/src/main/java/com/zjgsu/jby/entity/Product.java |
| — | `UserRepository` / `ProductRepository` | monolith/src/main/java/com/zjgsu/jby/repository/ |
