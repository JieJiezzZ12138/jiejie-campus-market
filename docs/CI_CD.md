# CI/CD 使用说明

## 已实现内容

当前仓库已提供按服务拆分的 GitHub Actions 部署流程：

- `.github/workflows/deploy-auth-service.yml`
- `.github/workflows/deploy-product-service.yml`
- `.github/workflows/deploy-gateway-service.yml`
- `.github/workflows/deploy-order-service.yml`

每个服务的流程包含：

- 在 `main` 分支对应服务代码或 Dockerfile 变化时触发
- 支持 `workflow_dispatch` 手动触发
- 使用 JDK 17 和 Maven 构建对应服务 JAR
- 构建 Docker 镜像并推送到阿里云容器镜像仓库
- 通过 SSH 登录服务器，拉取最新镜像并重启对应容器

## 你需要配置的 GitHub Secrets

在仓库 `Settings -> Secrets and variables -> Actions` 添加：

- `REGISTRY_USER`: 阿里云容器镜像仓库用户名
- `REGISTRY_PASSWORD`: 阿里云容器镜像仓库密码
- `REMOTE_HOST`: 服务器 IP/域名
- `SSH_PRIVATE_KEY`: 服务器 SSH 私钥
- `DB_PASSWORD`: 生产数据库密码
- `REDIS_PASSWORD`: 生产 Redis 密码
- `NACOS_HOST`: Nacos 地址

## 当前覆盖范围

目前自动部署覆盖后端四个核心服务：

- 认证服务 `auth-service`
- 商品服务 `product-service`
- 订单服务 `order-service`
- 网关服务 `gateway-service`

前端、MySQL、Redis、Nacos 的本地容器化部署由 `docker-compose.yml` 统一管理，详见 `docs/DOCKER_DEPLOYMENT.md`。

## 首次联调建议

1. 先在本地执行 `mvn -q -DskipTests compile` 和 `npm run build`，确认基础构建稳定。
2. 配置 GitHub Actions 所需 Secrets。
3. 手动触发单个服务的部署 workflow，确认镜像构建、推送和服务器重启流程正常。
4. 再通过服务路径触发自动部署，验证按服务增量发布。
