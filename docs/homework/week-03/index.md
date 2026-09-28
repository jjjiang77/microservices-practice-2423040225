# Week 03 作业文档 — Spring Boot 工程创建与启动测试

## 本周计划

| 任务 | 说明 | 状态 |
|------|------|------|
| 项目提案 | 在 `docs/project-proposal.md` 记录项目名称、目标用户、优先业务场景和核心模型 | ✅ 已完成 |
| 创建 Spring Boot 工程 | 在 `monolith/` 下创建 Maven 工程，包含启动类、配置文件、GET 接口和 Actuator | ✅ 已完成 |
| 启动测试 | 编写 `@SpringBootTest` contextLoads 测试，运行 `./mvnw test` 验证 | ✅ 已完成 |
| 补充运行说明 | 更新 README，写明环境要求、命令、接口地址和未实现功能 | ✅ 已完成 |

## 一、项目提案

详见 [docs/project-proposal.md](../../project-proposal.md)。

- **项目名称**：商小淘 ShangXiaoTao
- **目标用户**：高校学生（买家/卖家/管理员）
- **优先业务场景**：商品发布与浏览
- **核心模型**：Product（商品）、User（用户）

## 二、工程创建与运行

### 技术栈

| 项目 | 版本 |
|------|------|
| Java | 25 |
| Spring Boot | 4.0.7 |
| Maven | 3.9.9（内置 Wrapper） |
| 包名 | com.zjgsu.jby |

### 工程结构

```
monolith/
├── pom.xml
├── mvnw, mvnw.cmd
├── .mvn/wrapper/maven-wrapper.properties
└── src/
    ├── main/
    │   ├── java/com/zjgsu/jby/
    │   │   ├── Application.java
    │   │   └── controller/HelloController.java
    │   └── resources/application.yml
    └── test/
        └── java/com/zjgsu/jby/ApplicationTests.java
```

### 启动命令

```bash
cd monolith
./mvnw spring-boot:run
```

### 接口验证

#### 1. 问候接口

```bash
curl http://localhost:8080/api/hello
```

响应：

```json
{"project":"商小淘 ShangXiaoTao","description":"校园二手交易平台","status":"running"}
```

#### 2. 健康检查

```bash
curl http://localhost:8080/actuator/health
```

响应：

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

## 三、启动测试

### 测试命令

```bash
cd monolith
./mvnw test
```

### 测试结果

```
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 测试代码

`ApplicationTests.java` 使用 `@SpringBootTest` 注解，`contextLoads()` 方法验证 Spring 应用上下文可正常加载。

## 四、本周完成内容汇总

1. **项目提案**：在 `docs/project-proposal.md` 记录了商小淘项目的名称、目标用户、优先实现业务场景（商品发布与浏览）和两个核心模型（Product、User）。

2. **Spring Boot 工程创建**：
   - `monolith/pom.xml`：Spring Boot 4.0.7 parent，Java 25，依赖 spring-boot-starter-web、spring-boot-starter-actuator、spring-boot-starter-test
   - `monolith/src/main/java/com/zjgsu/jby/Application.java`：`@SpringBootApplication` 启动类
   - `monolith/src/main/java/com/zjgsu/jby/controller/HelloController.java`：`GET /api/hello` 接口
   - `monolith/src/main/resources/application.yml`：端口 8080，暴露 health 和 info 端点
   - Maven Wrapper：`mvnw`、`mvnw.cmd`、`.mvn/wrapper/maven-wrapper.properties`

3. **启动测试**：`ApplicationTests.java` 包含 `@SpringBootTest` contextLoads 测试，`./mvnw test` 通过。

4. **运行说明**：README 已更新，包含环境要求（Java 25、Maven 3.9+）、启动和测试命令、接口访问地址、当前尚未实现的业务能力。

## 五、截图说明

截图保存于 `docs/homework/week-03/screenshots/` 目录，需包含：

- 启动命令和 Spring Boot banner
- `/api/hello` 接口响应
- `/actuator/health` 接口响应
- `./mvnw test` 通过结果
