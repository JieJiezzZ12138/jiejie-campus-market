# CI/CD 使用说明

## 已实现内容
- CI (`.github/workflows/ci.yml`):
  - 后端: `mvn -B -ntp -DskipTests clean package`
  - 前端: `npm ci && npm run build`
  - 触发: 所有 `push` 和 `pull_request`
- CD (`.github/workflows/cd.yml`):
  - `main` 分支 push 自动打包
  - 支持 `workflow_dispatch` 手动触发
  - 自动上传构建产物到 GitHub Actions Artifacts
  - 配置好 SSH secrets 后可自动上传并在服务器解压

## 你需要配置的 GitHub Secrets
在仓库 `Settings -> Secrets and variables -> Actions` 添加：
- `DEPLOY_HOST`: 服务器 IP/域名
- `DEPLOY_PORT`: SSH 端口（可选，默认 22）
- `DEPLOY_USER`: SSH 用户
- `DEPLOY_SSH_KEY`: 私钥内容（建议专用部署密钥）
- `DEPLOY_PATH`: 服务器部署目录（例如 `/opt/my-system-cloud`）
- `DEPLOY_SCRIPT`: 服务器上的部署脚本路径（可选，例如 `/opt/my-system-cloud/deploy.sh`）

## 建议的服务器 deploy.sh 示例
将下列脚本放在服务器（路径与 `DEPLOY_SCRIPT` 一致）：

```bash
#!/usr/bin/env bash
set -euo pipefail

RELEASE_DIR="$1"
BACKEND_DIR="$RELEASE_DIR/backend"
FRONTEND_DIST="$RELEASE_DIR/frontend/dist"
APP_HOME="/opt/my-system-cloud"

mkdir -p "$APP_HOME"
cp -f "$BACKEND_DIR"/*.jar "$APP_HOME"/
rm -rf "$APP_HOME/frontend-dist"
cp -R "$FRONTEND_DIST" "$APP_HOME/frontend-dist"

# 这里替换成你自己的服务重启命令
# 例如 systemd: systemctl restart gateway-service auth-service product-service order-service
# 或者 supervisor/docker-compose

echo "Deploy completed: $RELEASE_DIR"
```

## 首次联调建议
1. 先只跑 CI，确认构建稳定。
2. 再配置 DEPLOY secrets。
3. 手动触发 CD（`workflow_dispatch` 选择 `deploy=true`）验证服务器发布流程。
