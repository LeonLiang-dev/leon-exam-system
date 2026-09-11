# ============================================================
# Leon 在线考试系统 — Linux 后端镜像
# 基础镜像: Temurin JDK 17 JRE (Alpine, 体积小)
# ============================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 应用 JAR（先构建: 项目根目录执行 ./build.sh，再把产物拷到 app/app.jar）
COPY app/app.jar /app/app.jar

EXPOSE 8080

# 健康检查（/api/v1/health 免鉴权；alpine 自带 busybox wget）
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/api/v1/health || exit 1

ENTRYPOINT ["java", "-Xms256m", "-Xmx1024m", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]
