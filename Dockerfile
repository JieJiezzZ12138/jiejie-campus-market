# 使用轻量级 JDK 21
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 这里的路径一定要对应 mvn clean package 生成的 jar 包位置
# 如果是单模块，通常在 target/*.jar
# 如果是多模块，请确认路径
COPY target/*.jar app.jar

# 限制内存，防止 2G 内存服务器崩溃
ENTRYPOINT ["java", "-Xmx128m", "-Xms128m", "-jar", "app.jar"]