# ============================================================
# 高校选课平台 - 后端镜像
# 单镜像同时提供：REST API（Spring Boot）+ 前端静态页面（原生 ESM）
# ============================================================

# ---------- 阶段 1：构建 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先复制 pom，利用层缓存
COPY server-java/pom.xml ./server-java/pom.xml
RUN mvn -B -f server-java/pom.xml dependency:go-offline

# 再复制源码打包
COPY server-java/src ./server-java/src
RUN mvn -B -f server-java/pom.xml clean package -DskipTests

# ---------- 阶段 2：运行 ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

# 前端静态资源：application.yml 里配置为 file:../client/
# 以 /app 为工作目录时，../client 即 /client，所以放这里
COPY client /client

COPY --from=build /build/server-java/target/*.jar /app/app.jar

# 数据库初始化脚本，供首次启动或容器编排挂载使用
COPY server-java/src/main/resources/db /app/db

RUN addgroup -S app && adduser -S app -G app && chown -R app:app /app /client
USER app

ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Dfile.encoding=UTF-8", "-Duser.timezone=Asia/Shanghai", "-jar", "/app/app.jar"]
