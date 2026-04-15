# 使用轻量级的 Java 21 运行环境
FROM eclipse-temurin:21-jre-alpine

# 设置工作目录
WORKDIR /app

# 复制 Auth 服务 jar 包到容器中并重命名为 app.jar
# 用通配符避免版本号/是否带 exec 后缀导致构建失败
COPY auth-service/target/auth-service-*.jar app.jar

# 声明暴露的端口（假设 auth-service 是 8081，按需修改）
EXPOSE 8081

# 启动命令 (环境变量会在 docker run 时注入)
ENTRYPOINT ["java", "-jar", "app.jar"]
