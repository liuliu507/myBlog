# ================= 第一阶段：构建 =================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# 1. 先复制 pom.xml，下载依赖（利用缓存，加快后续构建）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. 再复制源码，打包
COPY src ./src
RUN mvn package -DskipTests -B

# ================= 第二阶段：运行 =================
FROM eclipse-temurin:21-jre

WORKDIR /app

# 从构建阶段只复制 jar 包
COPY --from=builder /build/target/*.jar app.jar

# 暴露端口（仅声明，实际映射在 compose 中）
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]