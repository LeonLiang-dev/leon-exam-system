# 部署指南

## 开发环境

### 环境要求

| 工具 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 推荐 Eclipse Temurin 17.0.19 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 18+ | 前端构建 |
| npm | 9+ | 前端包管理 |
| MySQL | 8.0 | 数据库 |
| IDE | IntelliJ IDEA | 后端开发 |
| VS Code | 最新 | 前端开发 |

### 开发启动

#### 1. 数据库

```bash
# 安装 MySQL (macOS)
brew install mysql@8.0
brew services start mysql@8.0

# 创建数据库
mysql -u root -p -e "CREATE DATABASE wts DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

# 导入数据
mysql -u root -p wts < sql/init/wts.v1.4.1.sql
mysql -u root -p wts < sql/migrations/V2_add_point_column.sql
mysql -u root -p wts < sql/migrations/V10_question_type_spec_reset.sql
```

#### 2. 后端

```bash
cd wts-server

# 编译
mvn clean compile

# 运行（开发环境）
cd wts-app
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

或在 IntelliJ IDEA 中直接运行 `WtsApplication.java`。

**默认配置（application-dev.yml）：**
- 端口: 8080
- 数据库: localhost:3306/wts
- 用户名: root / 密码: 12345678

#### 3. 前端

```bash
cd wts-web

# 安装依赖（使用国内镜像加速）
npm install --registry=https://registry.npmmirror.com

# 启动开发服务器
npm run dev
```

- 前端地址: http://localhost:8000
- API 代理: `/api` → `http://localhost:8080`

## 生产部署

### 方式一：JAR 包部署（推荐）

```bash
# 一键构建（前端+后端打包为单个JAR）
chmod +x build.sh
./build.sh

# 部署运行
CORS_ALLOWED_ORIGIN_PATTERNS=https://exam.example.com \
JWT_SECRET=替换为至少32字节的随机密钥 \
java -jar dist/wts-app-3.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

**生产配置（application-prod.yml）：**
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://你的数据库地址:3306/wts?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: 你的数据库用户
    password: 你的数据库密码
```

### 方式二：Nginx 反向代理 + JAR

```nginx
server {
    listen 80;
    server_name exam.example.com;

    # 前端静态资源
    location / {
        root /var/www/nemotion/dist;
        try_files $uri $uri/ /index.html;
    }

    # API 代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### 方式三：Docker 部署

```dockerfile
# Dockerfile (参考)
FROM eclipse-temurin:17-jre-alpine
COPY dist/wts-app-3.0.0-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]
```

```bash
docker build -t nemotion .
docker run -d -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/wts \
  nemotion
```

## 系统配置

### application.yml 主要配置项

```yaml
# 服务端口
server:
  port: 8080

# 数据库
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/wts
    username: root
    password: 12345678

# MyBatis-Plus
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: input

# JWT
jwt:
  secret: your-secret-key-here
  expiration: 86400000  # 24小时

# CORS
app:
  cors:
    allowed-origin-patterns: http://localhost:8000,http://127.0.0.1:8000
```

### 默认账号

| 角色 | 用户名 | 密码 | 类型 |
|------|--------|------|------|
| 超级管理员 | sysadmin | 12345678 | type=3 |

学生账号由管理员在用户管理中创建或批量导入。生产环境务必修改默认密码和 JWT secret！

## 常见问题

### Q: 前端 npm install 很慢？
使用国内镜像: `npm install --registry=https://registry.npmmirror.com`

### Q: Maven 依赖下载失败？
检查 `~/.m2/settings.xml` 配置阿里云镜像仓库。

### Q: 前端启动后显示 403？
确保后端已启动并且数据库连接正常。JWT token 过期时需要重新登录。

### Q: MySQL 连接失败？
检查 MySQL 服务是否启动，用户名密码是否正确。macOS: `brew services start mysql@8.0`

### Q: 如何打包成教师机双击运行的 Windows 安装包？
见 [Windows 教师机安装包](windows-teacher-installer.md)。该方案会打包启动器、内置 JRE、内置 MariaDB 和 Spring Boot JAR，并默认限制学生只能从教师机同网段访问。
