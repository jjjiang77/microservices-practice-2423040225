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

本周（Week 03）仅做以上规划，不要求实现业务模型。后续课程将逐步实现数据库建模、持久化、服务拆分、认证授权、消息系统等完整功能。
