FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 这里我们依然采用之前的动态 COPY 逻辑，但启动参数是关键
COPY **/target/*.jar app.jar

# 极致压榨：限制堆内存 64M，使用串行垃圾回收器（省 CPU 和内存）
ENTRYPOINT ["java", "-Xmx64m", "-Xms64m", "-XX:MaxMetaspaceSize=64m", "-Xss256k", "-XX:+UseSerialGC", "-jar", "app.jar"]