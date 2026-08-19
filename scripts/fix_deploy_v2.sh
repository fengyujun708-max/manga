#!/bin/bash
# 漫界部署修复脚本 v2
# 修复：Nginx 证书问题 + API comic/home 路由
# 用法：bash fix_deploy_v2.sh

set -e
echo "=== 漫界修复 v2 ==="

# 1. 修复 Nginx（去掉 SSL 证书依赖）
echo ">>> 1. 重启 Nginx..."
fuser -k 80/tcp 2>/dev/null || true
fuser -k 81/tcp 2>/dev/null || true
sleep 2
docker rm -f manjie-nginx 2>/dev/null || true
docker run -d --name manjie-nginx \
  --network manjie-net \
  -p 80:80 \
  -p 81:81 \
  --restart unless-stopped \
  nginx:alpine
# 先只启动镜像拿到默认配置，然后覆盖配置
sleep 2
cat > /tmp/nginx-proxy.conf << 'NGINXEOF'
upstream api_servers {
    server manjie-api:3000;
}

server {
    listen 80;
    server_name _;
    client_max_body_size 20m;

    location / {
        proxy_pass http://api_servers;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 60s;
    }

    location /ws {
        proxy_pass http://api_servers;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 86400s;
    }
}
NGINXEOF
docker cp /tmp/nginx-proxy.conf manjie-nginx:/etc/nginx/conf.d/default.conf
docker exec manjie-nginx nginx -t 2>&1 || true
docker restart manjie-nginx
echo "Nginx 重启完成"

# 2. 验证 API 路由（不重建，先看当前）
echo ">>> 2. 验证当前 API 路由..."
sleep 2
HAS_HOME=$(curl -s http://localhost:3000/v1/comic/home | grep -c "404" || true)
echo "home 路由 404 状态: $HAS_HOME (1=需要重建)"

# 3. 如果需要重建 API（利用 Docker 层缓存，加速）
if [ "$HAS_HOME" = "1" ]; then
  echo ">>> 3. 重建 API（带缓存加速）..."
  cd /opt/manjie
  # 确保最新源码
  tar xzf /tmp/manjie-src.tar.gz 2>/dev/null || true
  # 用缓存构建（npm install 层命中缓存）
  docker build -t manjie-api:latest apps/api/ 2>&1 | tail -10
  docker rm -f manjie-api 2>/dev/null || true
  docker run -d --name manjie-api \
    --network manjie-net -p 3000:3000 \
    --env-file /opt/manjie/.env \
    --restart unless-stopped \
    manjie-api:latest
  sleep 10
fi

# 4. 完整验证
echo ">>> 4. 完整验证..."
echo "--- 配置 ---"
curl -s http://localhost:3000/v1/app/config
echo ""
echo "--- 首页 ---"
curl -s http://localhost:3000/v1/comic/home | head -c 300
echo ""
echo "--- 分类 ---"
curl -s http://localhost:3000/v1/comic/categories | head -c 200
echo ""
echo "--- 更新检查 ---"
curl -s "http://localhost:3000/v1/app/check-update?version=1.0.0&platform=android"
echo ""
echo "--- Nginx 代理 ---"
curl -s http://localhost/v1/app/config
echo ""

echo "=== 容器状态 ==="
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo "=== 完成 ==="