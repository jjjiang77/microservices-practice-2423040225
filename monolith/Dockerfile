# 商小淘 ShangXiaoTao — 单体应用镜像
# 多阶段构建：构建阶段用带 Maven + JDK 的镜像编译打包，运行阶段只保留 JRE 与 JAR。
# 这样本机无需安装 Maven / Java，也能产出可运行的镜像。

# ===== 阶段一：构建（build）=====
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

# 先复制 pom.xml 并预热依赖，利用镜像层缓存：
# 只改源码时，这一层不会失效，无需重复下载依赖。
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 再复制源码并打包（跳过测试，测试在本地/CI 单独执行）
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ===== 阶段二：运行（runtime）=====
# 只带 JRE，不含 Maven、源码与构建缓存，镜像更小、攻击面更少。
FROM eclipse-temurin:25-jre
WORKDIR /app

# 仅拷贝构建阶段产出的可运行 JAR
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080

# 按容器内存上限计算堆大小，避免容器内 JVM 误判宿主机内存
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
