# 项目提案 — 商小淘 ShangXiaoTao

## 项目名称

**商小淘 ShangXiaoTao**

浙工商校园二手交易平台。"商小淘"得名于浙工商学生的自称"浙小商"——"商"是浙工商的商，也是做买卖的商；"小淘"是爱淘货的小角色。从学生身份自然过渡到交易动作，自带校园认同感。

## 目标用户

| 角色 | 描述 | 核心需求 |
|------|------|----------|
| 买家 | 有购买需求的在校学生 | 浏览商品、搜索筛选、与卖家沟通、下单支付、安全交割、交易评价 |
| 卖家 | 有闲置物品出售的在校学生 | 快速发布商品、管理上下架、回复咨询、处理订单 |
| 管理员 | 平台运营维护人员 | 商品审核、用户管理、举报处理、数据统计 |

买家与卖家角色可互换，同一学生既可买也可卖。

## 优先实现业务场景

**商品发布与浏览**

这是平台最核心的链路：卖家发布商品 → 商品经审核后上架展示 → 买家浏览搜索 → 查看商品详情。该场景是后续聊天协商、下单交易、评价等流程的前置基础，优先实现可以快速验证从数据录入到展示的完整链路。

## 核心模型

### 1. Product（商品）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 商品唯一标识 |
| title | String | 商品标题 |
| description | String | 商品描述 |
| price | BigDecimal | 商品价格 |
| category | String | 商品分类（教材/电子产品/日用品/大件/服饰） |
| condition | String | 新旧程度（全新/九成新/八成新等） |
| sellerId | Long | 卖家用户 ID |
| status | Enum | 商品状态（待审核/在售/已售/已下架） |
| imageUrl | String | 商品图片 URL |
| createdAt | LocalDateTime | 发布时间 |
| updatedAt | LocalDateTime | 更新时间 |

### 2. User（用户）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 用户唯一标识 |
| studentId | String | 学号 |
| nickname | String | 昵称 |
| avatar | String | 头像 URL |
| department | String | 院系 |
| contact | String | 联系方式 |
| creditScore | Integer | 信用分 |
| role | Enum | 角色（学生/管理员） |
| createdAt | LocalDateTime | 注册时间 |
| updatedAt | LocalDateTime | 更新时间 |

## 当前阶段说明

- **Week 03**：完成工程搭建与启动验证（`/api/hello`、`/actuator/health`）。
- **Week 04（本周）**：在 `monolith/` 中实现"商品发布与浏览"一条完整业务链路，含模型、Service、REST API、业务规则和 MySQL 持久化，详见下文。

## Week 04 本周实现

### 一、本周要实现的一条核心业务流程

**商品发布与浏览**：

卖家发布商品 → Service 校验业务规则（价格区间、卖家信用分、重复发布） → 商品落库为**待审核 PENDING** → 审核上架变为**在售 ON_SALE** → 买家分页浏览/查看商品详情 → 买家下单变为**已售 SOLD**（或卖家主动下架变为 OFF_SHELF）。

该链路是第 03 周提案中"优先实现业务场景"的落地，也是后续聊天协商、订单交易、评价的前置基础。

### 二、参与流程的业务实体及其关系

| 实体 | 表 | 关键字段 | 说明 |
|------|----|----------|------|
| User（用户） | t_user | studentId（唯一）、nickname、department、creditScore、role | 学生，买卖双方共用同一角色，可同时发布和购买商品 |
| Product（商品） | t_product | title、price、category、conditionLevel、status、seller | 二手物品，归属于一个卖家 |

关系：**User 1 — N Product**。一个用户可发布多个商品；每个商品必须归属一个卖家，通过 `t_product.seller_id` 非空外键指向 `t_user.id`。详细表结构与约束见 [docs/database.md](database.md)。

### 三、需要在 Service 中检查的业务规则

全部集中在 `ProductService`（`com.zjgsu.jby.service.ProductService`），共 6 条，每条都有对应测试：

| # | 业务规则 | 违反时响应 | 测试 |
|---|----------|-----------|------|
| 1 | 商品价格必须 > 0 且 ≤ 2000 元（校园二手闲置定价上限） | 422 `PRICE_OUT_OF_RANGE` | `ProductServiceTest` |
| 2 | 卖家信用分 ≥ 60 才允许发布商品 | 422 `SELLER_CREDIT_TOO_LOW` | `ProductServiceTest` |
| 3 | 同一卖家不能重复发布同标题的未售出商品（PENDING/ON_SALE） | 409 `DUPLICATE_RESOURCE` | `ProductServiceTest` |
| 4 | 新商品状态固定为 PENDING；仅 PENDING 可上架、仅 ON_SALE 可下架或被购买 | 422 `INVALID_STATUS_TRANSITION` | `ProductServiceTest` |
| 5 | 买家不能购买自己发布的商品 | 422 `SELF_PURCHASE_NOT_ALLOWED` | `ProductServiceTest` / `ProductApiTest` |
| 6 | 买家信用分 ≥ 60 才能下单 | 422 `BUYER_CREDIT_TOO_LOW` | `ProductServiceTest` |

另外，学号唯一（`UserService.register` + 数据库唯一约束）重复注册返回 409。

