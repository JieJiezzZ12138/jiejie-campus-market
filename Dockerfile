# 使用轻量级 JDK 21
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 【核心修改点】
# 这里的 **/target/*.jar 表示：不管藏在哪个子目录的 target 文件夹下，只要是 jar 包，都拷过来！
COPY **/target/*.jar app.jar

# 限制内存，防止 2G 内存服务器崩溃
ENTRYPOINT ["java", "-Xmx128m", "-Xms128m", "-jar", "app.jar"]