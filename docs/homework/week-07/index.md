# Week 07：服务注册与发现（进行中）

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

## 已有工程核查

- 现有 Java 包名为 `com.zjgsu.jby`，应继续保留。
- 个人项目现有 `monolith/`（商品服务）和 `user-service/`（用户服务）；尚未找到 `services/`、第三个业务服务、第 06 周设计文档或统一 BOM。
- 原有第 06 周修改尚未提交，不能覆盖或还原。
- 商品发布流程已调用用户服务校验卖家信用，适合作为本周验证流程。
- 当前 `UserClient` 把所有远程异常转为 null，会使依赖故障被误报为用户不存在，需要区分 404 与 503。
- 当前 RestTemplate 未设置连接和读取超时，需要补齐。
- 课堂目录现有课程与选课服务，未找到学生服务。
- 核查时 Docker 未运行，尚未进行真实 Nacos 注册、业务调用、上下线验证或运行截图。

## 实施及验收清单

以下是待执行事项，不代表已经验证成功。

1. 确认第 06 周完成版、实际服务边界和课程统一 BOM；在原仓库延续代码，整理到 `services/<服务名>/`，保留接口与数据归属。
2. 根 compose 加入 `nacos/nacos-server:v3.2.4`，控制台仅绑定本机 18080，区分 8848 服务端口与 9848 SDK 通信端口，配置持久卷和鉴权。
3. 本地 `.env` 保存真实密钥和密码，`.env.example` 仅提供占位说明；先单独启动 Nacos，完成管理员密码初始化再启动业务服务。
4. 三个业务服务采用各自 application.name，个人分组统一 `PERSONAL_GROUP`；课堂三个服务统一 `COURSE_GROUP`。
5. 商品服务每次发布商品均调用 `DiscoveryClient.getInstances("user-service")`，取实例 URI 拼接 `/api/users/{id}` 后发起 HTTP；不永久缓存启动时地址。
6. 无实例或连接失败返回 503，用户确实不存在仍返回 404；失败时不新增商品。保留连接与读取超时，删除旧 `USER_SERVICE_URL`。
7. 单元测试模拟 DiscoveryClient 与 HTTP 响应，覆盖成功、空实例、远程 404、连接失败；各服务测试关闭 Nacos 并使用 H2。
8. 依次记录正常发布、停止 user-service 后发布失败且商品数不变、恢复并重新注册后发布成功。每次使用新标题等有效参数，避免重复校验干扰。
9. 从根目录执行 `bash scripts/smoke-test.sh`，保存真实退出码及测试输出。

## 健康状态说明（可用于提交）

以商小淘用户服务为例：Nacos SDK 的 gRPC 连接存活只表示客户端与注册中心保持连接；Nacos 中的实例状态表示注册中心所知的实例情况；Actuator 的应用健康可以反映数据库等依赖状态。数据库故障可能使 `/actuator/health` 返回 DOWN，同时 SDK 连接依然正常，不能认为 Nacos 默认会轮询该健康接口并自动摘除实例。服务发现也不保证跨服务写操作的原子性。

## 提交入口

仓库链接：https://github.com/jjjiang77/microservices-practice-2423040225

课堂座位号必须填写实际座位。截图必须来自真实运行，待放入 `screenshots/`；目前未生成任何运行证明。部署、调用、上下线、脚本结果等作业系统说明应在实际验证完成后填写，不能把预期结果当作实际结果。

## 本次实际修改

已为现有两个个人服务、两个课堂服务添加 Discovery Starter 和 Nacos 配置，两边分别使用 PERSONAL_GROUP 与 COURSE_GROUP。UserClient 和 CourseClient 已改为逐次通过 DiscoveryClient 获取实例 URI；连接/读取超时为 3/5 秒，空实例与连接失败返回 503，资源不存在保留 404。个人项目新增 UserClientTest，尚未运行成功。根 compose 已添加 Nacos 3.2.4、本机端口及持久卷，环境模板已补充。

未找到课程 BOM，暂采用 Spring Cloud 2025.1.0 / Alibaba 2025.1.0.0 官方 BOM 组合，待与课程配置核对，未覆盖 Nacos Client 版本。参考：https://sca.aliyun.com/en/docs/2025.x/overview/version-explain/ 。

环境阻塞：Docker.app 启动失败，系统报告缺失可执行文件；Maven 首次检查无法写入默认依赖缓存。未宣称构建、测试或运行通过。原有商品测试仍引用拆分前的 User/UserRepository，需要随第06周测试改造。

编译检查补充：改用临时 Maven 缓存后遇到 Maven Central 域名解析失败，随后重试下载公开依赖。XML 解析已通过；这些静态检查不代表 Java 编译通过。

验证更新：公开依赖下载重试后，商品服务 `mvn -DskipTests compile` 已成功退出（0）。其余服务单独检查中。此结果仅证明商品主代码编译通过，不能代替测试或容器运行证据。

最新编译结果：商品、用户、课程、选课四个现有服务的主代码编译均成功退出（0）。测试与真实注册/业务运行尚未通过验收。

环境更新：通过 docker desktop start 成功启动 Docker。此前系统打开应用失败不能据此认定安装损坏。Nacos 本地鉴权配置已写入被忽略的 .env，正在启动容器。

## 个人项目服务发现新增调用

新增 `services/purchase-service/`（服务名 purchase-service，PERSONAL_GROUP，端口8083）。`POST /api/purchases` 的请求为 `{"productId":1,"buyerId":2}`。该服务逐次使用 DiscoveryClient 查询 product-service 实例并向其原有 `/api/products/{id}/purchase` 发起 HTTP 请求；空实例、连接失败返回503，商品不存在保留404。正常购买会将商品状态改为 SOLD。

三项业务服务源代码位于 `services/product-service/`、`services/user-service/`、`services/purchase-service/`，Compose 镜像由这些目录构建。根脚本已将三个健康接口列入默认检查。
