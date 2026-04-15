# 使用轻量级的 Java 21 运行环境
FROM eclipse-temurin:21-jre-alpine

# 设置工作目录
WORKDIR /app

# 复制 Auth 服务的 jar 包到容器中并重命名为 app.jar
# (注意：路径要跟你的实际 target 路径对上)
COPY auth-service/target/auth-service-1.0.0-exec.jar app.jar

# 声明暴露的端口（假设 auth-service 是 8081，按需修改）
EXPOSE 8081

# 启动命令 (环境变量会在 docker run 时注入)
ENTRYPOINT ["java", "-jar", "app.jar"]