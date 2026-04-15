# 使用轻量级 JDK 21
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 【关键修改】：精确只拿 auth-service 的包，防止拿错 common 包
COPY auth-service/target/*.jar app.jar

# 限制内存，防止服务器崩溃
ENTRYPOINT ["java", "-Xmx128m", "-Xms128m", "-jar", "app.jar"]