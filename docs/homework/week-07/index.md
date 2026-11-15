# 第七周 · 服务注册与发现

## 基础题答案

按题目给出的选项顺序：**1 B，2 B，3 A，4 B，5 A，6 A，7 A，8 B，9 B，10 A**。

1. B：避免消费者写死服务实例地址。
2. B：提供者上报服务名、地址与端口等实例信息。
3. A：服务发现提供当前已知的服务实例列表。
4. B：连接失效被感知后，清理关联临时实例并通知订阅者。
5. A：命名空间用于隔离开发、测试和生产环境。
6. A：分组用于同一命名空间中的服务逻辑归类。
7. A：元数据可以支持按版本或区域选择实例。
8. B：注册连接存活不等于业务正常，不能假定实例已自动摘除。
9. B：网络分区发生时需要权衡一致性与可用性。
10. A：可以继续尝试缓存实例，但注册与列表变化感知可能受影响；缓存不保证实例始终可用。

## 服务注册与发现实现

### 服务划分与注册中心

个人项目拆为三个独立服务，均位于 `services/`，按业务能力注册到同一个 Nacos 分组：

| 服务 | 端口 | 数据归属 | 注册分组 |
| --- | --- | --- | --- |
| `user-service` | 8081 | `shangxiaotao_user` | `PERSONAL_GROUP` |
| `product-service` | 8080 | `shangxiaotao_product` | `PERSONAL_GROUP` |
| `purchase-service` | 8083 | `shangxiaotao_purchase` | `PERSONAL_GROUP` |

根目录 `compose.yaml` 提供 `nacos/nacos-server:v3.2.4`，控制台映射到本机 `18080`，服务端口 `8848` 与 gRPC 端口 `9848` 同时暴露；Nacos 使用持久卷保存注册与配置数据，管理员凭据由 `.env` 注入，仓库只保留 `.env.example` 模板。

三个服务统一继承 Spring Boot 4.0.7，采用 Spring Cloud 2025.1.0 与 Spring Cloud Alibaba 2025.1.0.0 BOM，Java 版本为 25，未单独覆盖 Nacos Client 版本。

### 业务调用方式

商品发布需要校验卖家是否存在。`product-service` 的 `UserClient` 在每次发布时通过 `DiscoveryClient` 查询 `user-service` 实例，不缓存启动时的固定地址：

```java
var instances = discovery.getInstances("user-service");
if (instances.isEmpty()) {
    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "user-service 无可用实例");
}
String url = instances.get(0).getUri() + "/api/users/" + id;
```

`RestTemplate` 配置连接超时 3 秒、读取超时 5 秒。调用结果按语义区分：

- 用户不存在（远程 404）→ 保留 404，不创建商品。
- 无可用实例或连接失败 → 503，不创建商品。
- 依赖成功 → 正常创建商品并返回 201。

`purchase-service` 以同样方式按服务名调用 `product-service`，不再写死容器 IP。单元测试 `UserClientTest` 覆盖空实例、逐次解析实例地址、旧地址连接失败三种情况；各服务测试关闭 Nacos 并使用 H2，不依赖真实注册中心或 MySQL。

## 运行与验证

从仓库根目录启动依赖与应用：

```bash
cp .env.example .env        # 首次运行，填写本地密码
docker compose up -d --build
bash scripts/smoke-test.sh
```

`smoke-test.sh` 在本次复核中的输出：

```text
PASS: Nacos console
PASS: application health UP
PASS: application health UP
PASS: application health UP
All configured checks passed. Business flow needs separate verification.
```

发布流程的上下线验证：

```bash
count() { curl -s 'http://127.0.0.1:8080/api/products?size=1' | jq -r '.totalElements'; }

# 1. 正常发布：用户服务在线
curl -i -H 'Content-Type: application/json' \
  -d '{"title":"第七周验证-A","price":99.00,"category":"TEXTBOOK","conditionLevel":"NEW","sellerId":1}' \
  http://127.0.0.1:8080/api/products

# 2. 停止用户服务后再次发布
docker compose stop user-service
count
curl -i -H 'Content-Type: application/json' \
  -d '{"title":"第七周验证-B","price":99.00,"category":"TEXTBOOK","conditionLevel":"NEW","sellerId":1}' \
  http://127.0.0.1:8080/api/products

# 3. 恢复用户服务并等待重新注册
docker compose start user-service
count
curl -i -H 'Content-Type: application/json' \
  -d '{"title":"第七周验证-C","price":99.00,"category":"TEXTBOOK","conditionLevel":"NEW","sellerId":1}' \
  http://127.0.0.1:8080/api/products
```

本次复核结果：

| 步骤 | HTTP 状态 | 商品总数 |
| --- | --- | --- |
| 用户服务在线，发布 A | 201 | 46 → 47 |
| 停止 user-service，发布 B | 503 | 47（未新增） |
| 恢复 user-service，发布 C | 201 | 47 → 48 |

提交时截图对应当时环境（商品数 18 → 18 → 19），数据绝对值随环境增长，前后差值一致。依赖下线时返回 503 且不写入商品，恢复后调用成功，说明调用方没有把依赖故障当成业务成功。

## 健康状态说明

以商小淘用户服务为例：Nacos SDK 的 gRPC 连接存活只表示客户端与注册中心保持连接；Nacos 中的实例状态表示注册中心所知的实例情况；Actuator 的应用健康可以反映数据库等依赖状态。数据库故障可能使 `/actuator/health` 返回 DOWN，同时 SDK 连接依然正常，不能认为 Nacos 默认会轮询该健康接口并自动摘除实例。服务发现也不保证跨服务写操作的原子性。

## 截图

- [smoke-test 通过](screenshots/01-smoke-test.png)
- [UserClientTest 三项测试通过](screenshots/02-user-client-tests.png)
- [停止 user-service 后 smoke 检查失败，恢复后重新通过](screenshots/03-user-service-down.png)

仓库链接：<https://github.com/jjjiang77/microservices-practice-2423040225>

课堂实践中的课程与选课服务在课堂环境完成，本仓库只保留个人项目代码与材料。
