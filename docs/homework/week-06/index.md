# 第六周 · 服务拆分与本地编排

本周把商小淘从单体拆成三个可独立构建、独立部署的服务，并用一份 Compose 文件在本地统一编排。三个服务均已进入根 `pom.xml` 聚合，代码位于 `services/`。

## 一、服务拆分设计

按业务能力划分服务边界，每个服务只拥有自己的数据：

| 服务 | 端口 | 职责 | 数据库 | 主要接口 |
| --- | --- | --- | --- | --- |
| `user-service` | 8081 | 学生注册、用户查询、信用资料 | `shangxiaotao_user` | `POST/GET /api/users` |
| `product-service` | 8080 | 商品发布、审核、筛选、购买状态流转 | `shangxiaotao_product` | `POST /api/products`、`POST /api/products/{id}/purchase` |
| `purchase-service` | 8083 | 购买入口，调用商品服务完成跨服务购买 | `shangxiaotao_purchase`（预留） | `POST /api/purchases` |

拆分原则：

- 商品表只保留 `seller_id` 作为业务引用，卖家昵称等资料由商品服务在运行时向用户服务查询，不再跨库联表。
- 购买服务不直接访问商品库，只通过服务接口发起购买，保证商品状态的唯一写入口在商品服务。
- 购买库暂不建业务表，保留独立连接，供后续订单持久化使用。

调用关系：`purchase-service → product-service → user-service`。商品发布和购买前都会校验卖家或买家是否存在；依赖不可用时返回 503，而不是把失败当成成功。

## 二、代码拆分

```text
services/
├── user-service/            # UserController、UserService、UserRepository、ApplicationTests
├── product-service/         # ProductController、ProductService、UserClient、UserClientTest
└── purchase-service/        # PurchaseController、PurchaseService、ProductClient、PurchaseControllerTest
```

每个服务至少包含一个 controller、一个 service 和一个测试：

- `user-service`：`UserController` 负责 HTTP 接口，`UserService` 负责学号唯一、信用分等规则，`ApplicationTests` 使用 H2 启动上下文。
- `product-service`：`ProductController` + `ProductService`，`UserClient` 封装对用户服务的调用，`UserClientTest` 覆盖空实例、逐次解析实例地址和旧地址连接失败。
- `purchase-service`：`PurchaseController` 只做请求转发，`PurchaseService` 负责 Feign 调用与异常映射，`PurchaseControllerTest` 覆盖成功响应和下游 503。

三个服务的 `pom.xml` 统一使用 `com.zjgsu.jby` 作为 groupId，各自带 Dockerfile 与 `.dockerignore`，可以单独构建。

## 三、本地编排

根目录 `compose.yaml` 一次拉起 Nacos、MySQL 和三个业务服务：

- Nacos 3.2.4 使用 standalone 模式，控制台映射 `127.0.0.1:18080`，服务端口 8848 与 gRPC 端口 9848 同时暴露。
- 一个 MySQL 8.4.11 实例承载三个独立业务库。`database/bootstrap.sh` 在空卷首次启动时建库、建表、导入示例数据，并创建三个账号；每个账号只被授予自己同名库的增删改查权限。
- MySQL 配置健康检查，业务服务等待 `service_healthy` 后再启动，避免先连库失败。
- 数据使用命名卷 `mysql-data`、`nacos-data`、`nacos-logs` 持久化。
- `product-service-replica` 复用商品服务定义，不映射宿主机端口，用于第八周的双实例负载均衡验证。

启动命令：

```bash
cp .env.example .env          # 首次运行，填写数据库与 Nacos 密码
docker compose up -d --build  # 构建并启动全部容器
docker compose ps             # 查看状态
bash scripts/smoke-test.sh    # 健康检查
```

当前环境 `docker compose ps` 的结果：

```text
SERVICE                   STATUS                    PORTS
mysql                     Up 30 minutes (healthy)   3306/tcp, 33060/tcp
nacos                     Up 30 minutes             127.0.0.1:8848->8848/tcp, 127.0.0.1:9848->9848/tcp, 127.0.0.1:18080->8080/tcp
product-service           Up 19 minutes             127.0.0.1:8080->8080/tcp
product-service-replica   Up 19 minutes             8080/tcp
purchase-service          Up 30 minutes             127.0.0.1:8083->8083/tcp
user-service              Up 3 minutes              127.0.0.1:8081->8081/tcp
```

## 四、验证

### 1. 健康检查

```bash
bash scripts/smoke-test.sh
```

```text
PASS: Nacos console
PASS: application health UP
PASS: application health UP
PASS: application health UP
All configured checks passed. Business flow needs separate verification.
```

### 2. 跨服务调用

商品服务发布商品时通过用户服务读取卖家昵称：

```bash
curl -s http://127.0.0.1:8081/api/users/1
curl -i -X POST http://127.0.0.1:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"title":"第六周跨服务验证","description":"验证跨服务调用是否通","price":199.00,"category":"ELECTRONICS","conditionLevel":"LIKE_NEW","sellerId":1}'
```

返回 `201`，商品状态为 `PENDING`，响应中的 `sellerNickname` 来自用户服务。购买链路：

```bash
curl -i -X POST http://127.0.0.1:8083/api/purchases \
  -H 'Content-Type: application/json' \
  -d '{"productId":1,"buyerId":2}'
```

购买成功返回 `200`，商品状态变为 `SOLD`。

### 3. 端到端脚本

```bash
./scripts/e2e-test.sh            # 正常流程
VERIFY_FAILURE=1 ./scripts/e2e-test.sh   # 故障流程
```

脚本自行创建卖家和买家，为每笔购买创建并审核一个独立测试商品，逐笔调用购买接口并断言返回 `SOLD`。故障模式停掉两个商品实例，断言购买接口返回 `503`，随后自动恢复实例。

本次故障模式结果：多笔购买均为 `SOLD`；停止 `product-service` 与 `product-service-replica` 后返回 `{"status":503,"error":"Service Unavailable"}`；脚本重新启动实例后环境恢复。

### 4. 服务边界检查

拆分后商品服务不再直接使用用户仓库或跨库联表，卖家资料统一经 HTTP 获取；购买服务也不再直接写商品库。依赖下线时调用方返回 503 且不写入数据。

## 截图

- [服务健康状态与跨服务调用](screenshots/01-service-structure.png)
- [商品发布自动补全卖家昵称](screenshots/02-service-code.png)
- [Compose 构建三个服务镜像](screenshots/03-compose-build.png)
- [容器编排状态与健康接口](screenshots/04-compose-ps.png)
- [smoke-test 通过](screenshots/05-smoke-test.png)
- [端到端购买与故障注入](screenshots/06-cross-service-verification.png)

仓库链接：<https://github.com/jjjiang77/microservices-practice-2423040225>
