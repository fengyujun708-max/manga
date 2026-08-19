#!/bin/bash
# 漫界部署修复脚本
# 修复：1. Nginx 端口占用 2. 首页路由 3. 完整验证

set -e

echo "=== 漫界修复脚本 ==="

# 1. 修复 Nginx 端口占用
echo ">>> 1. 修复 Nginx..."
fuser -k 80/tcp 2>/dev/null || true
sleep 2
docker rm -f manjie-nginx 2>/dev/null || true
docker run -d --name manjie-nginx \
  --network manjie-net \
  -p 80:80 \
  -v /opt/manjie/nginx/conf.d:/etc/nginx/conf.d \
  --restart unless-stopped \
  nginx:alpine 2>&1 || echo "Nginx 启动失败（可能镜像未拉取）"

# 2. 检查 API 运行状态
echo ">>> 2. 检查 API..."
docker ps | grep manjie-api || echo "API 未运行"
docker ps | grep manjie-postgres || echo "PostgreSQL 未运行"
docker ps | grep manjie-redis || echo "Redis 未运行"

# 3. 验证 API 路由
echo ">>> 3. 验证 API 路由..."
sleep 3
echo "--- 远程配置 ---"
curl -s http://localhost:3000/v1/app/config || echo " API 未响应"
echo ""
echo "--- 检查更新 ---"
curl -s "http://localhost:3000/v1/app/check-update?version=1.0.0&platform=android" || echo " API 未响应"
echo ""
echo "--- 首页 ---"
curl -s http://localhost:3000/v1/comic/home | head -3 || echo " 首页 404（需要重建 API）"
echo ""

# 4. 如果首页 404，重新构建 API
if curl -s http://localhost:3000/v1/comic/home | grep -q "404"; then
  echo ">>> 4. 重建 API（修复 home 路由）..."
  cd /opt/manjie
  docker build --no-cache -t manjie-api:latest apps/api/ 2>&1 | tail -5
  docker rm -f manjie-api 2>/dev/null
  docker run -d --name manjie-api \
    --network manjie-net \
    -p 3000:3000 \
    --env-file /opt/manjie/.env \
    --restart unless-stopped \
    manjie-api:latest
  sleep 8
  echo "--- 验证首页 ---"
  curl -s http://localhost:3000/v1/comic/home | head -3
fi

# 5. 最终状态
echo ""
echo "=== 最终状态 ==="
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""
curl -s -o /dev/null -w "API 响应代码: %{http_code}\n" http://localhost:3000/v1/app/config || echo "API 未响应"
curl -s -o /dev/null -w "API 文档响应: %{http_code}\n" http://localhost:3000/api-docs || echo "API 文档未响应"
echo "=== 完成 ==="