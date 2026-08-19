# 漫界 — 部署指南

> 服务器: 39.106.192.137 (阿里云 ECS, Alibaba Cloud Linux 8)
> 用户: root
> 密码: 已配置

---

## 1. 前置条件

- Docker 已安装 (26.1.3)
- 项目文件已同步到 `/opt/manjie/`

## 2. 首次部署

```bash
# SSH 到服务器
ssh root@39.106.192.137

# 进入项目目录
cd /opt/manjie

# 配置环境变量
vim .env   # 修改 JWT_SECRET, DB_PASSWORD 等

# 启动所有服务
docker compose -f docker/docker-compose.yml up -d

# 查看日志
docker compose -f docker/docker-compose.yml logs -f api
```

## 3. 数据库迁移

```bash
# 进入 API 容器执行迁移
docker exec -it manjie-api-1 npx typeorm migration:run -d dist/database/data-source

# 或首次启动时自动同步（开发环境）
# 设置 NODE_ENV=development 时 TypeORM synchronize=true 会自动建表
```

## 4. 更新部署

```bash
# 本地构建
cd apps/api && npm run build

# 同步到服务器
rsync -avz --delete --exclude node_modules ./ root@39.106.192.137:/opt/manjie/

# 重启服务
ssh root@39.106.192.137 "cd /opt/manjie && docker compose restart api"
```

## 5. 服务状态

```bash
# 查看所有服务
docker compose -f docker/docker-compose.yml ps

# 查看日志
docker compose logs -f --tail=100

# 重启单个服务
docker compose restart api

# 停止所有
docker compose down
```

## 6. 备份

```bash
# 数据库备份
docker exec -t manjie-postgres-1 pg_dump -U manjie manjie > backup_$(date +%Y%m%d).sql

# 定时备份（crontab）
0 3 * * * docker exec -t manjie-postgres-1 pg_dump -U manjie manjie > /backups/manjie_$(date +\%Y\%m\%d).sql
```