#!/bin/bash
# 漫界一键部署脚本
# 在服务器上直接运行

set -e

echo "=== 漫界一键部署 ==="

cd /opt/manjie

# 1. 配置阿里云 Docker 镜像加速
echo ">>> 配置 Docker 镜像加速..."
mkdir -p /etc/docker
cat > /etc/docker/daemon.json << 'EOF'
{
  "registry-mirrors": [
    "https://registry.cn-hangzhou.aliyuncs.com",
    "https://mirror.ccs.tencentyun.com"
  ],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
EOF
systemctl restart docker

# 2. 生成环境变量
echo ">>> 生成环境变量..."
cat > .env << ENVEOF
NODE_ENV=development
PORT=3000
CORS_ORIGIN=*

DB_HOST=postgres
DB_PORT=5432
DB_USERNAME=manjie
DB_PASSWORD=manjie_dev_pass
DB_DATABASE=manjie

REDIS_HOST=redis
REDIS_PORT=6379

JWT_SECRET=manjie-jwt-$(date +%s | md5sum | head -c 16)
JWT_REFRESH_SECRET=manjie-ref-$(date +%s | md5sum | head -c 16)

RATE_LIMIT_GLOBAL=60
ENVEOF

# 3. 创建 Docker 网络
echo ">>> 创建网络..."
docker network create manjie-net 2>/dev/null || true

# 4. 启动 PostgreSQL
echo ">>> 启动 PostgreSQL..."
docker run -d --name manjie-postgres \
  --network manjie-net \
  -e POSTGRES_DB=manjie \
  -e POSTGRES_USER=manjie \
  -e POSTGRES_PASSWORD=manjie_dev_pass \
  -v pgdata:/var/lib/postgresql/data \
  --restart unless-stopped \
  postgres:16-alpine

# 5. 启动 Redis
echo ">>> 启动 Redis..."
docker run -d --name manjie-redis \
  --network manjie-net \
  -v redisdata:/data \
  --restart unless-stopped \
  redis:7-alpine redis-server --appendonly yes

# 6. 等待数据库就绪
echo ">>> 等待数据库..."
sleep 10

# 7. 构建并启动 API
echo ">>> 构建并启动 API..."
docker build -t manjie-api apps/api/
docker run -d --name manjie-api \
  --network manjie-net \
  -p 3000:3000 \
  --env-file .env \
  --restart unless-stopped \
  manjie-api

# 8. 启动 Nginx
echo ">>> 启动 Nginx..."
docker run -d --name manjie-nginx \
  --network manjie-net \
  -p 80:80 \
  -v $(pwd)/nginx/conf.d:/etc/nginx/conf.d \
  --restart unless-stopped \
  nginx:alpine

# 9. 检查状态
echo "=== 部署完成 ==="
docker ps
echo ""
echo "API: http://localhost:3000/v1"
echo "API 文档: http://localhost:3000/api-docs"