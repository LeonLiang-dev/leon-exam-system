# 后端 JAR 放置位置

本目录放置部署用的后端 JAR，Dockerfile 会把它打进镜像。

```bash
cp ../../dist/wts-app-3.0.0-SNAPSHOT.jar app/app.jar
```

> JAR 由项目根目录 `./build.sh`（或 `build.bat`）构建，前端静态资源已内嵌，跨平台可直接使用。
