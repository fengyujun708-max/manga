# 漫界 — 一键部署

## 1. SSH 登录
```bash
ssh root@39.106.192.137
```

## 2. 确保 Docker 运行
```bash
systemctl start docker
```

## 3. 配置镜像加速（阿里云专用）
```bash
cat > /etc/docker/daemon.json << 'EOF'
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com"
  ]
}
EOF
systemctl restart docker
```

## 4. 拉取镜像
```bash
docker pull postgres:16-alpine
docker pull redis:7-alpine
docker pull nginx:alpine
```

## 5. 启动服务
```bash
cd /opt/manjie
bash scripts/deploy.sh
```

## 6. 验证
```bash
curl http://localhost:3000/v1/api-docs
curl http://localhost:3000/v1/comic/home
```

## 问题排查
- 拉取镜像超时 → 换镜像源或 `docker pull` 重试
- docker compose 损坏 → 用 `docker run` 替代（脚本已兼容）
- 端口冲突 → 检查 `lsof -i:3000`