# 使用轻量级 JDK 21
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 【关键修改】：不再使用模糊的通配符，精确只拿 auth-service 的包
COPY auth-service/target/*.jar app.jar

# 限制内存，防止 2G 内存服务器崩溃
ENTRYPOINT ["java", "-Xmx128m", "-Xms128m", "-jar", "app.jar"]