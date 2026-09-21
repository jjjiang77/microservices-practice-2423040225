# 作业 01：开发环境与个人仓库

## 环境检查

> 本节记录开发环境检查命令的输出结果。

### Java

```bash
$ java --version
openjdk 17.0.20.1 2026-08-18
OpenJDK Runtime Environment Homebrew (build 17.0.20.1+0)
OpenJDK 64-Bit Server VM Homebrew (build 17.0.20.1+0, mixed mode, sharing)
```

通过 Homebrew 安装 `openjdk@17`（JDK 17 LTS）。

### Maven

```bash
$ mvn --version
Apache Maven 3.9.16 (2bdd9fddda4b155ebf8000e807eb73fd829a51d5)
Maven home: /opt/homebrew/Cellar/maven/3.9.16/libexec
Java version: 27, vendor: Homebrew, runtime: /opt/homebrew/Cellar/openjdk/27/libexec/openjdk.jdk/Contents/Home
Default locale: zh_CN_#Hans, platform encoding: UTF-8
OS name: "mac os x", version: "26.6.2", arch: "aarch64", family: "mac"
```

通过 Homebrew 安装 `maven`（Maven 依赖的 openjdk 27 由 Homebrew 自动安装，不影响系统的 JDK 17）。

### Git

```bash
$ git --version
git version 2.50.1 (Apple Git-155)
```

macOS 系统自带 Git，版本 2.50.1。

### Docker

```bash
$ docker version
Client:
 Version:           29.8.0
 API version:       1.56
 Go version:        go1.26.8
 Git commit:        88096ef
 Built:             Thu Sep  3 21:49:43 2026
 OS/Arch:           darwin/arm64
 Context:           desktop-linux

Server: Docker Desktop 4.91.0 (239619)
 Engine:
  Version:          29.8.0
  API version:      1.56 (minimum version 1.40)
  Go version:       go1.26.8
  Git commit:       3ce5872
  Built:            Thu Sep  3 21:49:37 2026
  OS/Arch:          linux/arm64
  Experimental:     false
 containerd:
  Version:          v2.3.4
  GitCommit:        db8809540e1a7a9da5d518876894933ff55692ab
 runc:
  Version:          1.4.3
  GitCommit:        v1.4.3-0-gbb14dabe
 docker-init:
  Version:          0.19.0
  GitCommit:        de40ad0
```

```bash
$ docker compose version
Docker Compose version v5.5.1
```

通过 Homebrew 安装 `--cask docker`（Docker Desktop 4.91.0），使用 macOS Apple Virtualization Framework 运行 Linux 引擎。

---

## 概念回答

### 1. 什么是微服务架构？

微服务架构是一种将单个应用程序拆分为**多个小型服务**的架构风格，每个服务都围绕特定的业务能力构建，运行在**独立的进程**中，并通过**轻量级协议**（通常是 HTTP/REST 或 gRPC）进行通信。

每个微服务都可以由**独立的团队**使用**不同的技术栈**进行开发、部署和扩展，服务之间通过定义良好的 API 进行协作。从系统角度看，微服务架构强调的是**单一职责**（每个服务只做一件事）和**去中心化**（没有统一的业务逻辑和数据模型），整体系统通过服务的组合来实现复杂的业务需求。

### 2. 微服务和单体架构的主要区别是什么？

单体架构将所有功能模块打包在**同一个部署单元**中，模块边界通过包、命名空间来划分，但它们共享同一个进程、同一套数据库，技术栈和部署节奏必须保持一致；而微服务架构则把系统按业务能力拆分成**多个独立部署的小服务**，每个服务都有自己的进程、数据库或数据集合，可以独立伸缩。

具体区别体现在几个方面：

- **部署方式**：单体必须整体部署，改一处可能牵动全身；微服务可以独立部署单个服务，发布频率和回滚粒度都更细。
- **技术栈**：单体通常被一种技术栈绑定；微服务允许不同服务用不同语言或框架实现。
- **可扩展性**：单体只能整体水平扩展；微服务可以只针对瓶颈服务做扩容，资源利用更高效。
- **故障隔离**：单体的一个 bug 可能拖垮整个系统；微服务的故障通常被限制在单个服务内。
- **团队协作**：单体团队容易出现代码冲突和协调成本；微服务支持小团队独立负责一个服务，Conway 定律的影响更明显。

但微服务也不是银弹，它会带来分布式事务、服务治理、运维复杂度等新挑战。

### 3. 为什么本课程先实现单体系统，再逐步拆分为微服务？

本课程采用"**单体先行、逐步拆分**"的教学路径，主要基于以下考虑：

- **降低入门门槛**：微服务涉及服务注册、配置中心、网关、分布式事务等多个复杂概念。如果一开始就讲微服务，初学者会被工具链和运维负担淹没，反而难以理解"为什么需要微服务"以及"它解决了单体架构的什么痛点"。
- **建立完整的工程视角**：从单体起步，可以让同学完整体验**需求分析 → 编码 → 测试 → 部署 → 文档**的全流程，理解模块边界、接口设计、数据一致性等基础问题。这些问题在微服务中会**放大**呈现，只有先在单体里见过，才能在拆分时知道"切在哪里最合理"。
- **拆分过程本身就是学习**：把一个已经能跑的单体系统拆成微服务，会涉及到边界识别、数据迁移、接口兼容性、部署流水线改造等多个实战问题，比凭空设计一个"完美"的微服务架构更有收获。
- **贴合企业真实演进路径**：现实中大多数公司的系统都是从单体开始的，很少有项目从零就采用微服务。先单体再拆分的过程与工业界的演进路线一致，能让同学更快适应真实工作场景。

### 4. 为什么作业需要提供可重复运行的测试或验证脚本？

微服务系统天然是**分布式的**，多个服务通过网络协作，任何一次修改都可能影响上下游的行为。如果没有可重复执行的验证脚本，每次提交后都得手动跑一遍所有场景，既费时又容易遗漏，长期下来代码会逐渐"腐烂"——某个改动悄悄破坏了一个没人注意的功能。

可重复运行的脚本带来的核心价值有三点：

- **自动化验证**：开发者和评审者都能用同一份脚本快速确认"我做的事情确实是对的"，减少人为判断的差异。
- **回归保护**：当系统规模变大、作业难度提升时，历史的验证脚本可以保证之前的正确性不会被新改动破坏。
- **可复现的工程实践**：脚本本身就是文档，它告诉其他人"这个系统应该怎么跑通"，让接手作业的同学或助教不需要从零摸索。

对于微服务课程来说，这一点尤其重要——作业会从单体逐步演进到多服务，没有脚本支撑，后续的部署、集成测试、跨服务验证几乎无法完成。

---

## 问题记录

### Java / Maven 安装情况

安装成功。通过 Homebrew 安装：
- `openjdk@17`（JDK 17.0.20.1 LTS），使用 `java_home` 配置 `JAVA_HOME` 环境变量。
- `maven`（Apache Maven 3.9.16），Maven 运行时使用 Homebrew 自动安装的 openjdk 27，与系统 JDK 17 独立，不影响项目编译。

### Docker 安装情况

安装成功，但过程中遇到问题并已解决。

- **当前操作系统版本**：macOS 26.6.2（Tahoe），Apple Silicon（arm64）
- **安装尝试过程**：
  1. 通过 `brew install --cask docker` 安装 Docker Desktop 4.91.0。
  2. 启动后 Docker daemon 无法启动，报错 `Error response from daemon: Docker Desktop is unable to start`。
  3. 查看日志发现 VM 引擎一直 stalled，HTTP 503。
  4. 进一步排查发现 Docker Desktop 启动时尝试安装 Rosetta 2 失败，错误信息：`Internal Virtualization error. Failed to install Rosetta.`
  5. 通过命令行手动执行 `softwareupdate --install-rosetta --agree-to-license` 成功安装 Rosetta 2。
  6. 重启 Docker Desktop 后 Engine 成功启动。
- **失败原因分析**：Docker Desktop 在 macOS 上使用 Apple Virtualization Framework 运行 Linux VM，启动时需要 Rosetta 2 来支持 x86 镜像的运行。Docker Desktop 内置的 Rosetta 安装流程在 macOS 26.6.2 上失败，导致 VM 无法启动。
- **下一步解决计划**：Docker 已正常运行，无需额外操作。后续如遇到 x86 镜像兼容性问题，可考虑使用 `--platform linux/amd64` 配合 Rosetta 2 运行。

### 其他问题

- Maven 使用的 Java 版本（27）与系统 `java --version`（17）不一致：这是 Homebrew 的正常行为，Maven 自带依赖的 openjdk 27 仅用于 Maven 运行，项目编译和运行仍使用系统配置的 JDK 17，不影响开发。