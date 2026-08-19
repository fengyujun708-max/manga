#!/bin/bash
# 一键修复脚本 - 在服务器上运行
set -x

echo "=== 修复开始 $(date) ==="

# 1. 重启 Nginx（新配置无 SSL）
echo "--- 1. Nginx ---"
fuser -k 80/tcp 2>/dev/null
sleep 2
docker rm -f manjie-nginx 2>/dev/null
docker run -d --name manjie-nginx --network manjie-net -p 80:80 --restart unless-stopped nginx:alpine
sleep 3
docker cp /opt/manjie/nginx/conf.d/default.conf manjie-nginx:/etc/nginx/conf.d/default.conf
docker exec manjie-nginx nginx -t 2>&1 | tail -3
docker restart manjie-nginx
sleep 3
docker ps --format "{{.Names}} {{.Status}}" | grep nginx

# 2. 直接下载 python 检测 comic/home 是否 404
echo "--- 2. 检测 home 路由 ---"
RESP=$(curl -s -o /tmp/home.json -w "%{http_code}" http://localhost:3000/v1/comic/home)
echo "home HTTP: $RESP"
cat /tmp/home.json 2>/dev/null | head -c 200
echo ""

# 3. 如果是 404，重建 API（后台运行）
if [ "$RESP" = "404" ]; then
  echo "--- 3. 重建 API ---"
  cd /opt/manjie
  # 检查源码是否最新
  ls apps/api/src/modules/comic/comic.controller.ts 2>/dev/null || echo "comic controller 缺失!"
  docker build -t manjie-api:latest apps/api/ > /tmp/build.log 2>&1
  echo "build exit: $?"
  tail -5 /tmp/build.log
  docker rm -f manjie-api 2>/dev/null
  docker run -d --name manjie-api --network manjie-net -p 3000:3000 \
    --env-file /opt/manjie/.env --restart unless-stopped manjie-api:latest
  echo "API 重启，等待..."
  sleep 10
fi

# 4. 最终验证
echo "--- 4. 验证 ---"
curl -s http://localhost:3000/v1/app/config
echo ""
curl -s http://localhost:3000/v1/comic/home | head -c 300
echo ""
curl -s http://localhost/v1/app/config
echo ""
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo "=== 修复完成 $(date) ==="