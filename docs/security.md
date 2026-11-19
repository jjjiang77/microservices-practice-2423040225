# 认证与授权

本周在原有服务拆分基础上加入统一入口与 JWT 认证。网关负责“谁能进来”，用户服务负责“你是谁”，下游服务只接受网关转发或带内部密钥的调用。

## 一、认证流程

```text
1. 客户端 POST /api/auth/login  {studentId, password}
2. gateway-service 命中公开路径，直接转发到 user-service
3. user-service 读取 t_user.password_hash，用 BCrypt 校验密码
4. 校验通过后签发 HS256 JWT：sub=学号, uid, role, iat, exp
5. 客户端访问业务接口时携带 Authorization: Bearer <token>
6. gateway-service 删除客户端自带的身份头 → 验签 → 校验角色
7. 验签通过后注入 X-User-Id / X-User-Role / X-Gateway-Secret 并转发
8. user-service 的 GatewayGuardFilter 校验内部密钥，管理端再校验角色
```

失败语义：

| 情况 | 返回 |
| --- | --- |
| 未携带 Token 访问受保护接口 | 401 `UNAUTHORIZED` |
| Token 格式错误、签名不符、已过期 | 401 `INVALID_TOKEN` |
| 非 ADMIN 访问 `/api/admin/**` | 403 `ACCESS_DENIED` |
| 学号不存在或密码错误 | 401 `INVALID_CREDENTIALS` |
| 绕过网关直连下游受保护接口 | 401 `UNTRUSTED_REQUEST` |

## 二、角色与权限

系统保留两种角色（`UserRole`）：

| 角色 | 可访问接口 | 说明 |
| --- | --- | --- |
| 匿名 | `POST /api/auth/login`、`POST /api/users`、`GET /actuator/health` | 登录、注册、健康检查 |
| STUDENT | `GET /api/users/**`、`/api/products/**`、`/api/purchases/**` | 日常买卖 |
| ADMIN | 上述接口 + `GET /api/admin/users` | 用户管理 |

JWT 中的 `role` 是唯一可信来源；网关不会读取客户端传入的 `X-User-Role`。

## 三、网关路由表

| 路由 id | 断言 | 目标 | 鉴权 |
| --- | --- | --- | --- |
| user-service | `/api/users/**,/api/auth/**,/api/admin/**` | `lb://user-service` | 按路径区分公开/登录/管理员 |
| product-service | `/api/products/**` | `lb://product-service` | 需登录 |
| purchase-service | `/api/purchases/**` | `lb://purchase-service` | 需登录 |

网关端口 8080，健康检查 `GET /actuator/health`。业务服务仅绑定 `127.0.0.1` 供调试，对外只暴露网关。

## 四、身份头防伪

- 网关在路由前删除 `X-User-Id`、`X-User-Role`、`X-Gateway-Secret`，无论原请求是否携带。
- 只有 JWT 验签通过后才重新写入这三个头，值来自 Token 声明与服务端配置。
- `X-Gateway-Secret` 由 `GATEWAY_INTERNAL_SECRET` 注入，网关、用户服务、商品服务三方一致。
- 下游服务不得把身份头当作可选提示：用户服务对非公开接口强制校验内部密钥。

## 五、下游防绕过

1. **内部密钥**：`GatewayGuardFilter` 拦截所有非公开请求，`X-Gateway-Secret` 缺失或不匹配即 401。
2. **服务间调用**：商品服务 `UserClient` 调用 `GET /api/users/{id}` 时同样携带该密钥，不依赖公网入口。
3. **端口收敛**：Compose 只把网关发布到宿主机；用户、商品、购买服务均为 `127.0.0.1` 绑定，生产部署不映射。
4. **角色二次校验**：管理端接口在网关校验一次，在用户服务再按 `X-User-Role` 校验一次，避免单点失效。

## 六、密码与密钥

- 注册接口 `POST /api/users` 必填 `password`（8-64 位）。
- 使用 `BCryptPasswordEncoder(10)`，每次加盐；数据库列 `password_hash VARCHAR(100)`。
- 代码、SQL、日志、Token 中都不出现明文密码。
- `JWT_SECRET` 至少 32 字符随机值，可用 `openssl rand -base64 32` 生成；`gateway-service` 与 `user-service` 必须一致。
- `.env` 已被 `.gitignore` 忽略，`.env.example` 只保留占位符。

## 七、测试账号

| 学号 | 密码 | 角色 |
| --- | --- | --- |
| 2423040225 | Student@123 | STUDENT |
| 2423040999 | Student@123 | STUDENT |
| 2423040001 | Student@123 | STUDENT（低信用） |
| admin | Admin@123 | ADMIN |

账号来自 `database/seed.sql`，只在空数据卷首次初始化时导入。

## 八、迁移与限制

- 已有数据卷不会自动补 `password_hash` 列，需执行：

  ```sql
  ALTER TABLE shangxiaotao_user.t_user
      ADD COLUMN password_hash VARCHAR(100) NULL AFTER role;
  ```

- 从旧库迁移的用户没有密码哈希，登录会返回 401，需要重新注册或由管理员重置。
- 当前 Token 为无状态 JWT，未实现刷新与吊销；登出由客户端删除 Token 完成。
- 暂未接入 Spring Security 的完整过滤链，仅使用其 `spring-security-crypto` 做密码哈希，鉴权集中在网关。
