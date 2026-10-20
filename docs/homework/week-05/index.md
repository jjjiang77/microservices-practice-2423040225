# Week 05 作业文档 — Docker 容器化

将 `monolith/` 单体工程（商小淘：商品发布与浏览链路）与 MySQL 数据库容器化，实现可重复的镜像构建、一键启动、业务 API 验证与数据持久化验证。

相关文件：

| 文件 | 位置 | 作用 |
|------|------|------|
| `Dockerfile` | `monolith/` | 多阶段构建（Maven 构建 → JRE 运行） |
| `.dockerignore` | `monolith/` | 排除 `target/`、IDE 配置、`.env` 等无关/敏感内容 |
| `compose.yaml` | 仓库根目录 | 编排 `app` + `mysql` 两个服务，构建上下文 `./monolith` |
| `.env.example` | 仓库根目录 | 环境变量模板，复制为 `.env` 使用（真实 `.env` 被忽略） |

## 一、Dockerfile 与镜像构建

### 实现思路

`monolith/Dockerfile` 采用**多阶段构建**：

1. **构建阶段** `FROM maven:3.9-eclipse-temurin-25 AS build`：使用带 Maven 与 JDK 25 的镜像。先 `COPY pom.xml` 并 `mvn dependency:go-offline` 预热依赖（利用镜像层缓存，只改源码时不重复下载依赖），再 `COPY src` 并 `mvn clean package -DskipTests` 打出可运行 JAR。
2. **运行阶段** `FROM eclipse-temurin:25-jre`：只带 JRE，仅 `COPY --from=build` 复制构建阶段的 `*.jar`，不含 Maven、源码与构建缓存。

多阶段构建的作用：**把"编译环境"和"运行环境"分开**——构建需要 Maven + 完整 JDK，运行只需 JRE；最终镜像更小、启动更快、暴露的软件与攻击面更少，同时**本机无需安装 Maven / Java** 也能产出可运行镜像。

`.dockerignore` 排除：`target/`、`build/`、`.git/`、IDE 配置（`.idea`、`*.iml`、`.vscode`）、系统文件（`.DS_Store`）、密码/环境文件（`.env`、`*.local`）、日志（`*.log`）。

### 构建命令

```bash
# 在仓库根目录执行，构建上下文指向 ./monolith
docker build -t shangxiaotao:week05 ./monolith

# 查看镜像
docker images shangxiaotao:week05
```

### 结果

镜像 `shangxiaotao:week05` 构建成功，`docker images` 可看到对应 IMAGE ID、SIZE 与 TAG。

## 二、Compose 编排与启动

### 实现思路

仓库根目录 `compose.yaml`（**不使用已废弃的 `version` 字段**）：

| 要点 | 实现 |
|------|------|
| 服务 | `app`（构建上下文 `./monolith`，镜像 `shangxiaotao:week05`）+ `mysql`（`mysql:8.4.11`） |
| 端口映射 | 应用 `"${APP_PORT:-8080}:8080"` |
| 共享网络 | 自定义网络 `shangxiaotao`，两服务同网 |
| 服务名连接 | 应用环境变量 `DB_URL=jdbc:mysql://mysql:3306/shangxiaotao?...`，通过**服务名 `mysql`** 而非 localhost |
| 命名数据卷 | `mysql-data:/var/lib/mysql` |
| 环境变量 | 连接地址/账号/密码由 `.env` → 容器环境变量注入；提供 `.env.example`，`.env` 在 `.gitignore` 中 |
| 健康检查 | `mysql` 配 `mysqladmin ping` 健康检查；`app` 用 `depends_on: mysql: condition: service_healthy` 等数据库就绪再启动 |
| 初始化 | `schema.sql`、`data.sql` 挂载到 `/docker-entrypoint-initdb.d/`（按 `01-`、`02-` 前缀顺序先建表后导数据） |
| 字符集 | 初始化脚本开头加 `SET NAMES utf8mb4;`，避免容器内 `mysql` 客户端默认 `latin1` 导致中文种子数据双重编码乱码 |

应用侧 `application.yml` 已参数化：`url: ${DB_URL:...}`、`username: ${DB_USERNAME:...}`、`password: ${DB_PASSWORD:...}`，本机运行用默认值，容器运行由 Compose 注入。

> 踩坑记录：容器内 `mysql` 客户端默认 `character_set_client=latin1`，直接导入 UTF-8 的 `data.sql` 会把中文**双重编码**成乱码。因此两个 SQL 脚本开头都加了 `SET NAMES utf8mb4;`，让服务端按 utf8mb4 解释导入字节，种子数据中的中文才能正确落库。

### 启动命令

```bash
cp .env.example .env          # 首次准备配置模板
docker compose up -d --build
docker compose ps
```

### 结果

`docker compose ps` 显示 `app` 与 `mysql` 两个服务均 `running`（mysql 为 `healthy`）。

## 三、个人业务 API 验证

容器映射到本机 `8080`，沿用上周业务实现，验证健康接口与 2 个业务 API（一次创建 + 一次查询）。

```bash
# 健康接口
curl -i http://localhost:8080/actuator/health

# 创建①：注册卖家用户（201）
curl -i -X POST http://localhost:8080/api/users -H 'Content-Type: application/json' \
  -d '{"studentId":"2423040225","nickname":"小商同学","department":"计算机科学与技术学院"}'

# 创建②：卖家发布商品（201 + status=PENDING）
curl -i -X POST http://localhost:8080/api/products -H 'Content-Type: application/json' \
  -d '{"title":"考研数学复习全书","description":"九成新，校内面交","price":45.00,"category":"TEXTBOOK","conditionLevel":"LIKE_NEW","sellerId":1}'

# 查询：分页浏览 / 商品详情（200）
curl -i "http://localhost:8080/api/products?status=ON_SALE&page=0&size=10"
curl -i http://localhost:8080/api/products/1
```

### 结果

- `GET /actuator/health` → `200`，响应体 `{"status":"UP"}`
- `POST /api/users` → `201`，`Location: /api/users/1`
- `POST /api/products` → `201`，响应 `status=PENDING`
- `GET /api/products` / `GET /api/products/{id}` → `200`，返回分页结构与商品详情

## 四、数据持久化验证

通过业务接口创建一条记录，`docker compose down` 删除容器后再 `docker compose up -d` 重建，查询同一记录确认数据仍在。**不使用 `docker compose down -v`**（该命令会删除数据卷）。

```bash
# 1) 创建一条记录，记下返回的 id
curl -s -X POST http://localhost:8080/api/users -H 'Content-Type: application/json' \
  -d '{"studentId":"2423040777","nickname":"持久化验证用户","department":"计算机科学与技术学院"}'

# 2) 删除容器（保留数据卷）
docker compose down

# 3) 重建并等待就绪
docker compose up -d
docker compose ps

# 4) 查询同一条记录：id 与关键字段不变
curl -i http://localhost:8080/api/users/4   # 换成第 1 步返回的 id
```

### 结果与原理

重建后该记录仍可查询，`id`、`studentId`、`nickname` 等关键字段与重建前一致。原因是 MySQL 数据目录 `/var/lib/mysql` 挂载在**命名数据卷 `mysql-data`** 上，`docker compose down` 只删除容器不删除卷，新容器继续使用同一份数据；只有 `docker compose down -v`（或 `docker volume rm`）才会清空。

## 五、截图清单

截图存放于 `docs/homework/week-05/screenshots/`：

| 建议文件名 | 对应小题 | 内容 |
|------------|----------|------|
| `01-build-images.png` | 1 Dockerfile 与镜像构建 | `docker build` 成功输出 + `docker images shangxiaotao:week05` |
| `02-compose-ps.png` | 2 Compose 编排与启动 | `docker compose ps` 显示 app 与 mysql 均 running |
| `03-api-health.png` | 3 业务 API 验证 | `curl -i /actuator/health` → 200 `{"status":"UP"}` |
| `04-api-create-query.png` | 3 业务 API 验证 | `curl -i` 创建商品 201 + 查询列表 200 |
| `05-persistence.png` | 4 数据持久化验证 | down 前查询 / down+up 后查询同一条记录 |
