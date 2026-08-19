# 漫界 - 全栈漫画阅读平台

## 项目概述

漫界是一个全栈漫画阅读平台，采用微服务架构，包含后端 API、管理后台、Flutter 客户端、源同步 Worker 等组件。

## 技术栈

### 后端
- **框架**: NestJS (TypeScript)
- **数据库**: PostgreSQL 16 + Redis 7
- **认证**: JWT + Argon2 / bcryptjs
- **API 文档**: Swagger (OpenAPI 3.0)
- **定时任务**: @nestjs/schedule (Cron)
- **HTTP 客户端**: @nestjs/axios

### 客户端
- **框架**: Flutter 3.x (Dart)
- **状态管理**: flutter_bloc
- **路由**: go_router
- **网络**: dio
- **本地存储**: flutter_secure_storage + shared_preferences

### 管理后台
- **框架**: Next.js 14 (App Router)
- **UI 组件**: Ant Design 5
- **图表**: ECharts

### 基础设施
- **容器化**: Docker + Docker Compose
- **反向代理**: Nginx
- **CI/CD**: GitHub Actions
- **监控**: Sentry (可选)

## 项目结构

```
manjie/
├── apps/
│   ├── api/           # NestJS 后端 API
│   ├── admin/         # Next.js 管理后台
│   ├── worker/        # 源同步 Worker (定时同步上游源)
│   └── source-sync/   # 旧版源同步 (已合并到 worker)
├── client/             # Flutter 客户端
├── docker/             # Docker Compose 配置
├── nginx/              # Nginx 反向代理配置
├── scripts/            # 部署/种子数据/修复脚本
├── docs/               # 架构文档
└── package.json        # 根 package.json (workspaces)
```

## 核心功能模块

### Phase 0-1: 基础设施
- ✅ PostgreSQL + Redis + Nginx 部署
- ✅ NestJS 基础架构 (模块化架构)
- ✅ JWT 认证系统 (Access/Refresh Token)
- ✅ Docker Compose 编排

### Phase 2: 用户系统
- ✅ 手机号注册/登录/验证码
- ✅ 密码管理 (bcryptjs)
- ✅ 设备管理/注销
- ✅ 账号注销

### Phase 3: 核心阅读器
- ✅ 瀑布流/单页/双页阅读模式
- ✅ 亮度/色温/背景色调节
- ✅ 章节导航/进度记录
- ✅ 离线下载/预加载

### Phase 4: 漫画源系统 ⭐
- ✅ **MangaSource SDK** - 统一源接口
- ✅ **QuickJS 沙箱** - 安全运行第三方 JS 源
- ✅ **SourceManager** - 源安装/更新/启用/禁用/测试
- ✅ **源注册表 API** - 源注册/发现/下载/测试
- ✅ **源市场 UI** - 浏览/搜索/安装/管理源
- ✅ **SourceManager** - 本地源管理/更新检查
- ✅ **QuickJS 沙箱** - 安全执行第三方 JS 源
- ✅ **自动同步 Worker** - 定时从上游同步源 (每 6 小时)
- ✅ **bcryptjs** 替代 argon2 (避免原生编译问题)

### Phase 5: 社区系统
- ✅ 帖子/评论/点赞/收藏
- ✅ 漫画引用卡片
- ✅ 求漫/求源系统
- ✅ AI 智能匹配求漫

### Phase 6: 求漫/求源
- ✅ 发布/回答/采纳最佳答案
- ✅ AI 智能匹配推荐

### Phase 7: 管理后台
- ✅ 用户/源/版本/公告/配置管理
- ✅ 仪表盘统计

### Phase 8: 更新系统
- ✅ 强制/可选更新
- ✅ Remote Config 远程配置
- ✅ 维护模式

### Phase 9: CI/CD
- ✅ GitHub Actions 自动构建/测试/部署
- ✅ Docker 镜像构建推送
- ✅ 自动化部署脚本

## 核心架构设计

### 漫画源插件系统
```
MangaSource (抽象接口)
    ├── search(keyword, page, pageSize)
    ├── getDetail(comicId)
    ├── getChapters(comicId)
    ├── getPages(comicId, chapterId)
    ├── getPopular()
    ├── getLatest()
    ├── getCategories()
    ├── getByCategory(categoryId)
    ├── login(credentials)
    ├── getFavorites()
    ├── addFavorite(comicId)
    └── removeFavorite(comicId)
```

### 源注册表格式
```json
{
  "sources": [
    {
      "id": "bika",
      "name": "哔咔漫画",
      "version": "2.1.0",
      "author": "Breeze Team",
      "description": "哔咔漫画源",
      "icon": "🔥",
      "repositoryUrl": "https://github.com/deretame/Breeze",
      "downloadUrl": "https://cdn.example.com/sources/bika.js",
      "sha256": "abc123...",
      "minAppVersion": "1.0.0",
      "capabilities": ["search", "detail", "chapters", "pages", "login", "favorites"],
      "downloads": 15230,
      "rating": 4.8,
      "metadata": {}
    }
  ]
}
```

### 源代码规范
```javascript
// 标准源代码格式
const source = {
  id: 'bika',
  name: '哔咔漫画',
  version: '2.1.0',
  author: 'Breeze Team',
  description: '哔咔漫画源',
  icon: '🔥',
  
  // 必需方法
  async search(keyword, page = 1, pageSize = 20) { ... },
  async getDetail(comicId) { ... },
  async getChapters(comicId) { ... },
  async getPages(comicId, chapterId) { ... },
  
  // 可选方法
  async getPopular({ page = 1, pageSize = 20 }) { ... },
  async getCategories() { ... },
  async login(credentials) { ... },
  async getFavorites() { ... },
  async addFavorite(comicId) { ... }
};

module.exports = source;
```

### QuickJS 沙箱安全限制
```dart
// 允许的 API
- fetch (仅 HTTP/HTTPS)
- DOMParser (HTML 解析)
- atob/btoa (Base64)
- setTimeout/setInterval (限制最大 30s)
- console.log/warn/error
- localStorage (内存存储)
- JSON.parse/stringify

// 禁止的操作
- eval/Function 构造器
- require/import
- 文件系统/进程/网络原始访问
- Native API 调用
```

## 部署指南

### 1. 环境准备
```bash
# 服务器要求
- 2核 4GB 内存以上
- Docker 24+ / Docker Compose 2.x
- 域名已解析到服务器 IP
```

### 2. 部署步骤
```bash
# 1. 克隆代码
git clone https://github.com/your-org/manjie.git
cd manjie

# 2. 配置环境变量
cp apps/api/.env.example apps/api/.env
# 编辑 .env 填入数据库密码、JWT 密钥等

# 3. 启动服务
docker compose -f docker/docker-compose.yml up -d

# 4. 验证服务
curl http://your-domain/v1/app/config
curl http://your-domain/v1/comic/home
```

### 3. 数据库迁移
```bash
# 运行迁移
cd apps/api && npm run migration:run

# 种子数据
npm run seed
```

### 4. 客户端构建
```bash
cd client
flutter pub get
flutter build apk --release
# 输出: build/app/outputs/flutter-apk/app-release.apk
```

## API 接口文档
- Swagger UI: `http://your-domain:3000/api-docs`
- 核心模块: Auth, User, Comic, Community, Source, Request, Notification, Announcement, Admin, Update

## 源开发指南

### 1. 创建源文件
```javascript
// bika.js
const source = {
  id: 'my-source',
  name: '我的漫画源',
  version: '1.0.0',
  icon: '📚',
  
  async search(keyword, page = 1, pageSize = 20) {
    const html = await fetch(`https://example.com/search?q=${keyword}&page=${page}`);
    const $ = DOMParser.parseFromString(html, 'text/html');
    return $('.comic-item').map(el => ({
      id: el.attr('data-id'),
      title: el.find('.title').text(),
      coverUrl: el.find('img').attr('src'),
      author: el.find('.author').text(),
    }));
  },
  
  async getDetail(comicId) { ... },
  async getChapters(comicId) { ... },
  async getPages(comicId, chapterId) { ... }
};

module.exports = source;
```

### 2. 发布源
1. 将源代码上传到 CDN/对象存储
2. 生成 manifest.json
3. 计算 SHA256: `sha256sum source.js`
4. 在管理后台注册源

### 3. 源测试清单
- [ ] 搜索功能正常
- [ ] 详情页完整显示
- [ ] 章节列表正确
- [ ] 图片加载正常
- [ ] 登录/收藏功能 (如支持)

## 监控与运维

### 关键指标
- API 响应时间 < 200ms
- 错误率 < 0.1%
- 源可用性 > 99%
- 数据库连接池使用 < 80%

### 日志查看
```bash
# API 日志
docker logs manjie-api -f

# Worker 日志
docker logs manjie-worker -f

# Nginx 访问日志
docker logs manjie-nginx -f
```

### 常见问题排查
| 问题 | 排查步骤 |
|------|----------|
| 源无法加载 | 检查源代码语法、网络连通性、SHA256 校验 |
| API 500 | 查看 `docker logs manjie-api` |
| 源同步失败 | 检查 Worker 日志、上游仓库可达性 |
| 图片加载失败 | 检查 Nginx 代理、防盗链设置 |

## 贡献指南
1. Fork 项目
2. 创建功能分支: `git checkout -b feature/xxx`
3. 提交变更: `git commit -m 'feat: add xxx'`
4. 推送分支: `git push origin feature/xxx`
5. 创建 Pull Request

## 许可证
MPL-2.0 License - 详见 [LICENSE](LICENSE)

---

**漫界团队** | 以技术让漫画阅读更自由