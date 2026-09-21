# 作业 01 提交材料：开发环境与个人仓库

**课程**：微服务开发
**姓名**：蒋博宇
**学号**：2423040225
**GitHub 用户名**：jjjiang77
**提交日期**：2026-09-21

---

## 1. GitHub 仓库链接

**仓库 URL**：https://github.com/jjjiang77/microservices-practice-2423040225

- 仓库名称：`microservices-practice-2423040225`（按要求格式 `microservices-practice-学号`）
- 仓库可见性：**Public**（公开）
- 远程地址：可在终端执行 `git remote -v` 验证

---

## 2. git log --oneline --graph 截图

> 在仓库目录下执行 `git log --oneline --graph` 后截屏，证明至少有 2 次提交记录。

![git log --oneline --graph](screenshots/git-log.png)

---

## 3. 环境版本检查截图

### 3.1 Java

> 命令：`java --version`

![java 版本检查](screenshots/java-version.png)

### 3.2 Maven

> 命令：`mvn --version`

![maven 版本检查](screenshots/mvn-version.png)

### 3.3 Docker

> 命令：`docker version`

![docker 版本检查](screenshots/docker-version.png)

> 命令：`docker compose version`

![docker compose 版本检查](screenshots/docker-compose-version.png)

> ⚠️ 如 Docker 暂时无法安装成功，需在 `docs/homework/week-01/index.md` 的「问题记录」中说明原因、系统版本和解决计划。

---

## 4. 本周文字说明（100-200 字）

本周完成首次作业：创建个人仓库 microservices-practice-2423040225 并搭建 docs、src 目录，完成两次 Git 提交；用 Homebrew 安装 Java 17、Maven、Docker 并完成版本检查；撰写 index.md 中四个微服务概念问题，理解了微服务与单体架构区别、"先单体后拆分"教学路径的合理性，及可重复测试脚本的工程价值，为后续作业奠基。

（字数：197 字）

---

## 附录：仓库目录结构

```
microservices-practice-2423040225/
├── README.md                              # 课程、姓名、学号、仓库用途
├── docs/
│   └── homework/
│       └── week-01/
│           ├── index.md                   # 环境检查 + 概念回答 + 问题记录
│           ├── SUBMISSION.md              # 本文件（学习通提交材料）
│           └── screenshots/               # 截图存放目录
└── src/                                   # 项目源码目录（待后续作业填充）
```