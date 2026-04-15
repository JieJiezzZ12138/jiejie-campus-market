# 使用轻量级 JDK 21
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 精确抓取 auth-service 目录下 target 里的 jar 包
COPY auth-service/target/*.jar app.jar

ENTRYPOINT ["java", "-Xmx128m", "-Xms128m", "-jar", "app.jar"]