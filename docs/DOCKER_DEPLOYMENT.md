# Jemall Docker 容器化部署说明

本项目已提供完整 Docker Compose 编排，用于统一开发、测试、生产环境中的运行依赖。

## 容器组成

- `frontend`: Nginx 托管 Vue 构建产物，并反向代理 API 到网关
- `gateway-service`: Spring Cloud Gateway，统一入口
- `auth-service`: 认证与用户注册登录
- `product-service`: 商品、分类、收藏、评价、图片上传
- `order-service`: 购物车、订单、地址、通知、客服消息
- `mysql`: MySQL 8.0，自动初始化表结构和演示数据
- `redis`: Redis 7，缓存商品列表、分类等热点数据
- `nacos`: Nacos 2.4.0，服务发现，兼容 Apple Silicon / arm64 环境

## 首次启动

```bash
cp .env.example .env
docker compose up -d --build
```

如果 Docker Hub 拉取 `node`、`nginx`、`maven`、`eclipse-temurin` 超时，可以在 `.env` 中替换基础镜像：

```env
MAVEN_IMAGE=docker.1ms.run/library/maven:3.9-eclipse-temurin-17
JRE_IMAGE=docker.1ms.run/library/eclipse-temurin:17-jre-alpine
NODE_IMAGE=docker.1ms.run/library/node:22-alpine
NGINX_IMAGE=docker.1ms.run/library/nginx:1.27-alpine
APP_PLATFORM=linux/amd64
```

Apple Silicon 如果遇到镜像源没有 arm64 manifest，保留 `APP_PLATFORM=linux/amd64` 即可，由 Docker Desktop 负责兼容运行。

然后重新执行：

```bash
docker compose build --no-cache
docker compose up -d
```

访问地址：

- 前端商城：`http://localhost`
- 网关 API：`http://localhost:8080`
- Nacos 控制台：`http://localhost:8848/nacos`

## 数据初始化

`docker-compose.yml` 会在 MySQL 首次创建数据卷时自动执行：

```text
database/reset-seed-ecommerce.sql
```

该脚本会创建/补齐课程项目所需表，并写入演示账号、商品、订单相关基础数据。

如果已经启动过容器且需要重新初始化数据库：

```bash
docker compose down -v
docker compose up -d --build
```

`down -v` 会删除 MySQL/Redis/图片等 Docker 数据卷，请确认不需要保留旧数据后再执行。

## 配置统一

所有服务使用 `prod` profile，并通过环境变量连接容器网络中的依赖：

- MySQL: `mysql:3306`
- Redis: `redis:6379`
- Nacos: `nacos:8848`

常用端口和密码在 `.env` 中配置，开发、测试、生产可以复用同一份镜像，只替换 `.env` 即可。

## 前端代理

前端镜像使用 Nginx：

- 静态页面由 `/` 提供
- `/auth/**`、`/product/**`、`/order/**`、`/cart/**`、`/user/**`、`/images/**` 自动代理到 `gateway-service:8080`
- Vue Router 刷新页面时回退到 `index.html`

因此浏览器只需要访问 `http://localhost`，不需要单独配置跨域。

## 常用命令

查看服务状态：

```bash
docker compose ps
```

查看日志：

```bash
docker compose logs -f gateway-service
docker compose logs -f product-service
```

重启单个服务：

```bash
docker compose restart product-service
```

停止服务：

```bash
docker compose down
```

## 课程报告可写的容器化亮点

- 使用 Dockerfile 多阶段构建后端 JAR 与前端静态资源，减少对本机环境的依赖
- 使用 Compose 编排 MySQL、Redis、Nacos、Gateway、业务服务和前端 Nginx
- 使用 `.env` 抽离端口、密码、命名空间等环境差异
- 使用 Docker volume 保存数据库、Redis、商品图片上传文件
- 使用 Nginx 将前端和 API 统一到同一访问入口，降低跨域和部署差异
