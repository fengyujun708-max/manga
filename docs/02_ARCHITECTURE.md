# 漫界 — 架构设计文档

> 本文档定义漫界的完整架构体系，包括：目录结构、模块职责、数据流、接口约定、组件设计。
> AI Coding Agent 在开始任何模块开发前，必须先读取本文档。

---

## 1. 整体架构分层

```
┌──────────────────────────────────────────────────────────────────┐
│                    表现层 (Presentation)                         │
│  UI (Flutter Widgets)  /  ViewModel / Controller / Cubit/Bloc   │
├──────────────────────────────────────────────────────────────────┤
│                    业务层 (Business Logic)                       │
│  服务层 (Services) / 用例层 (UseCases) / 领域模型 (Models)      │
├──────────────────────────────────────────────────────────────────┤
│                    数据层 (Data Access)                          │
│  Repository / API Client / Local Storage / Cache                │
├──────────────────────────────────────────────────────────────────┤
│                    基础设施层 (Infrastructure)                   │
│  Network / Database / File System / Device APIs                 │
└──────────────────────────────────────────────────────────────────┘
```

---

## 2. 客户端目录结构详解

### 2.1 `app/` — 应用入口与全局配置

```
app/
├── app.dart                  # MaterialApp 配置，路由注册
├── router/
│   ├── router.dart           # AutoRoute 路由定义
│   └── router.gr.dart        # 自动生成
├── theme/
│   ├── theme.dart            # 主题定义
│   ├── colors.dart           # 品牌色板
│   ├── typography.dart       # 字体层级
│   ├── dimensions.dart       # 间距/圆角系统
│   └── dark_mode.dart        # 深色模式
├── localization/
│   ├── app_localizations.dart
│   ├── zh_CN.dart
│   └── en_US.dart
└── config/
    ├── app_config.dart       # 全局配置常量
    ├── environment.dart      # 环境区分 (dev/staging/prod)
    └── constants.dart        # 通用常量
```

### 2.2 `core/` — 基础设施层

```
core/
├── network/
│   ├── api_client.dart       # Dio 封装，拦截器
│   ├── api_endpoints.dart    # 所有 API 端点定义
│   ├── interceptors/
│   │   ├── auth_interceptor.dart
│   │   ├── log_interceptor.dart
│   │   └── error_interceptor.dart
│   ├── websocket_client.dart
│   └── exceptions.dart       # 统一异常定义
│
├── storage/
│   ├── secure_storage.dart   # Token 等敏感信息
│   ├── local_storage.dart    # 本地 KV 存储
│   └── database.dart         # ObjectBox/SQLite 封装
│
├── security/
│   ├── crypto_utils.dart
│   └── device_fingerprint.dart
│
├── update/
│   ├── update_checker.dart
│   └── force_update.dart
│
├── analytics/
│   └── analytics_service.dart
│
└── error/
    ├── error_handler.dart
    └── error_reporting.dart
```

### 2.3 `features/` — 业务模块

每个 feature 遵循以下结构：

```
features/<feature_name>/
├── bloc/                        # 状态管理
│   ├── <feature>_bloc.dart
│   ├── <feature>_event.dart
│   └── <feature>_state.dart
├── repository/                  # 数据仓库
│   └── <feature>_repository.dart
├── models/                      # 领域模型
│   └── <feature>_model.dart
├── view/                        # UI 页面
│   └── <feature>_page.dart
└── widgets/                     # 私有组件
    └── ...
```

#### 2.3.1 `auth/` — 用户认证

```
features/auth/
├── bloc/
│   ├── auth_bloc.dart
│   ├── auth_event.dart
│   └── auth_state.dart
├── repository/
│   └── auth_repository.dart
├── models/
│   ├── user.dart
│   ├── login_request.dart
│   ├── register_request.dart
│   └── token_pair.dart
├── view/
│   ├── login_page.dart
│   ├── register_page.dart
│   ├── forgot_password_page.dart
│   └── phone_verify_page.dart
└── widgets/
    ├── phone_input.dart
    ├── verify_code_input.dart
    └── password_input.dart
```

#### 2.3.2 `home/` — 首页

```
features/home/
├── bloc/
│   └── home_bloc.dart
├── repository/
│   └── home_repository.dart
├── models/
│   ├── banner.dart
│   ├── comic_card.dart
│   └── home_section.dart
├── view/
│   └── home_page.dart
└── widgets/
    ├── hero_banner.dart
    ├── comic_card_item.dart
    ├── section_header.dart
    └── section_list.dart
```

#### 2.3.3 `reader/` — 阅读器

```
features/reader/
├── bloc/
│   ├── reader_bloc.dart
│   ├── reader_event.dart
│   └── reader_state.dart
├── repository/
│   └── reader_repository.dart
├── models/
│   ├── chapter.dart
│   ├── page_data.dart
│   └── reader_settings.dart
├── view/
│   ├── reader_page.dart
│   └── reader_shell.dart
└── widgets/
    ├── modes/
    │   ├── webtoon_mode.dart       # 瀑布流模式
    │   ├── single_page_mode.dart   # 单页模式
    │   └── dual_page_mode.dart     # 双页模式
    ├── controls/
    │   ├── reader_settings_sheet.dart
    │   ├── chapter_navigator.dart
    │   └── brightness_slider.dart
    └── overlay/
        ├── chapter_transition.dart
        └── reading_progress.dart
```

#### 2.3.4 `community/` — 社区

```
features/community/
├── bloc/
│   ├── post_list_bloc.dart
│   ├── post_detail_bloc.dart
│   └── comment_bloc.dart
├── repository/
│   └── community_repository.dart
├── models/
│   ├── post.dart
│   ├── comment.dart
│   ├── comic_ref.dart
│   └── reaction.dart
├── view/
│   ├── community_page.dart
│   ├── post_detail_page.dart
│   └── create_post_page.dart
└── widgets/
    ├── post_card.dart
    ├── comment_item.dart
    ├── comic_ref_card.dart
    └── rich_editor.dart
```

#### 2.3.5 `request/` — 求漫/求源

```
features/request/
├── bloc/
│   ├── manga_request_bloc.dart
│   └── source_request_bloc.dart
├── repository/
│   └── request_repository.dart
├── models/
│   ├── manga_request.dart
│   ├── source_request.dart
│   └── request_status.dart
├── view/
│   ├── manga_request_page.dart
│   ├── source_request_page.dart
│   └── create_request_page.dart
└── widgets/
    ├── request_card.dart
    ├── request_status_badge.dart
    └── answer_section.dart
```

#### 2.3.6 `sources/` — 源管理

```
features/sources/
├── bloc/
│   └── source_manager_bloc.dart
├── repository/
│   └── source_repository.dart
├── models/
│   ├── manga_source.dart
│   ├── source_manifest.dart
│   └── source_status.dart
├── view/
│   ├── source_manager_page.dart
│   └── source_detail_page.dart
└── widgets/
    ├── source_card.dart
    ├── source_settings.dart
    └── source_test_panel.dart
```

### 2.4 `plugins/` — 插件系统

```
plugins/
├── runtime/
│   ├── js_engine.dart           # QuickJS 封装
│   ├── sandbox.dart             # 沙箱限制
│   └── plugin_context.dart      # 插件上下文
├── manager/
│   ├── plugin_manager.dart
│   ├── plugin_installer.dart
│   └── plugin_updater.dart
├── registry/
│   ├── source_registry.dart
│   └── registry_sync.dart
└── compatibility/
    ├── breeze_adapter.dart      # Breeze 源适配器
    └── venera_adapter.dart      # Venera 源适配器
```

### 2.5 `repositories/` — 全局仓库

```
repositories/
├── comic_repository.dart        # 漫画数据仓库
├── user_repository.dart         # 用户数据仓库
├── sync_repository.dart         # 同步数据仓库
└── cache_repository.dart        # 缓存数据仓库
```

### 2.6 `services/` — 全局服务

```
services/
├── notification_service.dart
├── download_service.dart
├── cache_service.dart
├── sync_service.dart
└── background_task_service.dart
```

---

## 3. 后端架构（NestJS）

### 3.1 目录结构

```
src/
├── main.ts
├── app.module.ts
│
├── common/
│   ├── decorators/
│   │   ├── current-user.decorator.ts
│   │   ├── roles.decorator.ts
│   │   └── public.decorator.ts
│   ├── guards/
│   │   ├── jwt-auth.guard.ts
│   │   ├── roles.guard.ts
│   │   └── throttle.guard.ts
│   ├── interceptors/
│   │   ├── response-transform.interceptor.ts
│   │   └── logging.interceptor.ts
│   ├── filters/
│   │   └── http-exception.filter.ts
│   ├── pipes/
│   │   └── validation.pipe.ts
│   ├── dto/
│   │   └── pagination.dto.ts
│   └── constants/
│       ├── error-codes.ts
│       └── roles.ts
│
├── modules/
│   ├── auth/
│   │   ├── auth.module.ts
│   │   ├── auth.controller.ts
│   │   ├── auth.service.ts
│   │   ├── strategies/
│   │   │   ├── jwt.strategy.ts
│   │   │   └── jwt-refresh.strategy.ts
│   │   └── dto/
│   │       ├── login.dto.ts
│   │       ├── register.dto.ts
│   │       └── refresh-token.dto.ts
│   │
│   ├── user/
│   │   ├── user.module.ts
│   │   ├── user.controller.ts
│   │   ├── user.service.ts
│   │   └── entities/
│   │       └── user.entity.ts
│   │
│   ├── sms/
│   │   ├── sms.module.ts
│   │   ├── sms.service.ts
│   │   └── providers/
│   │       ├── aliyun-sms.provider.ts
│   │       └── console-sms.provider.ts  # 测试用
│   │
│   ├── comic/
│   │   ├── comic.module.ts
│   │   ├── comic.controller.ts
│   │   ├── comic.service.ts
│   │   └── entities/
│   │       ├── comic.entity.ts
│   │       └── chapter.entity.ts
│   │
│   ├── community/
│   │   ├── community.module.ts
│   │   ├── controllers/
│   │   │   ├── post.controller.ts
│   │   │   └── comment.controller.ts
│   │   ├── services/
│   │   │   ├── post.service.ts
│   │   │   └── comment.service.ts
│   │   └── entities/
│   │       ├── post.entity.ts
│   │       └── comment.entity.ts
│   │
│   ├── request/
│   │   ├── request.module.ts
│   │   ├── controllers/
│   │   │   ├── manga-request.controller.ts
│   │   │   └── source-request.controller.ts
│   │   ├── services/
│   │   │   ├── manga-request.service.ts
│   │   │   └── source-request.service.ts
│   │   └── entities/
│   │       ├── manga-request.entity.ts
│   │       └── source-request.entity.ts
│   │
│   ├── source/
│   │   ├── source.module.ts
│   │   ├── source.controller.ts
│   │   ├── source.service.ts
│   │   ├── sync/
│   │   │   └── source-sync.service.ts
│   │   ├── test/
│   │   │   └── source-test.service.ts
│   │   └── entities/
│   │       ├── source-registry.entity.ts
│   │       └── source-version.entity.ts
│   │
│   ├── notification/
│   │   ├── notification.module.ts
│   │   ├── notification.service.ts
│   │   └── entities/
│   │       └── notification.entity.ts
│   │
│   ├── announcement/
│   │   ├── announcement.module.ts
│   │   ├── announcement.controller.ts
│   │   └── services/
│   │       └── announcement.service.ts
│   │
│   └── admin/
│       ├── admin.module.ts
│       └── controllers/
│           ├── user-management.controller.ts
│           ├── content-management.controller.ts
│           ├── source-management.controller.ts
│           └── dashboard.controller.ts
│
└── database/
    ├── migrations/
    └── seeds/
```

### 3.2 核心数据流

```
Client Request
    │
    ▼
Nginx (HTTPS termination)
    │
    ▼
AuthGuard (JWT verify)
    │
    ▼
RolesGuard (RBAC check)
    │
    ▼
ThrottleGuard (Rate limit)
    │
    ▼
Controller
    │
    ▼
Service (Business Logic)
    │
    ▼
Repository / TypeORM
    │
    ▼
PostgreSQL
```

---

## 4. 数据流设计

### 4.1 认证流程

```
注册流程：
Client → POST /auth/register/send-code → SMS Service → Redis (code:5min)
Client → POST /auth/register/verify → Validate code → Create user → JWT pair

登录流程：
Client → POST /auth/login → Validate credentials → JWT pair
Client → POST /auth/refresh → Validate refresh token → New JWT pair

验证码登录：
Client → POST /auth/sms/send-code → SMS Service → Redis (code:5min)
Client → POST /auth/sms/login → Validate code → JWT pair
```

### 4.2 阅读流程

```
Client → GET /comic/:id → ComicController → ComicService → PG → Response
Client → GET /comic/:id/chapters → ChapterService → PG → Response
Client → GET /reader/:chapterId/pages → 第三方源 → 代理 → Response
           ↓
           (首页请求漫画源，获取图片URL，返回给客户端)
```

### 4.3 源同步流程

```
上游源仓库 (GitHub) → Webhook/Cron → Sync Service
    ↓
拉取最新源 JS 文件
    ↓
SHA256 校验
    ↓
JS 静态检查 (AST 分析)
    ↓
Sandbox 测试 (连接/搜索/详情/章节/图片)
    ↓
Staging Registry
    ↓
管理员审核 / 自动规则
    ↓
Production Registry
    ↓
CDN 分发
    ↓
漫界 App 拉取更新
```

---

## 5. 接口设计规范

### 5.1 统一响应格式

```typescript
// 成功
{
  code: 0,
  message: "success",
  data: {} | [] | null
}

// 分页
{
  code: 0,
  message: "success",
  data: {
    items: [],
    total: 100,
    page: 1,
    limit: 20,
    totalPages: 5
  }
}

// 错误
{
  code: 1001,
  message: "验证码错误",
  data: null
}
```

### 5.2 错误码范围

| 范围 | 模块 |
|------|------|
| 1000-1999 | 认证/授权 |
| 2000-2999 | 用户 |
| 3000-3999 | 漫画 |
| 4000-4999 | 社区 |
| 5000-5999 | 求漫/求源 |
| 6000-6999 | 源系统 |
| 7000-7999 | 通知 |
| 8000-8999 | 管理后台 |
| 9000-9999 | 系统错误 |

### 5.3 认证头

```
Authorization: Bearer <access_token>
X-Refresh-Token: <refresh_token>
```

---

## 6. 安全设计

### 6.1 Token 策略

- Access Token: JWT, 15分钟过期
- Refresh Token: 随机字符串, 7天过期, Redis 存储
- 每次刷新返回新的 Access Token + Refresh Token（Refresh Token Rotation）
- 旧 Refresh Token 作废

### 6.2 密码策略

- 最小长度：8 位
- 必须包含：大写字母 + 小写字母 + 数字
- 可选：特殊字符
- 加密：Argon2id (memory=19MB, iterations=2, parallelism=1)

### 6.3 Rate Limit

| 接口 | 限制 |
|------|------|
| 登录 | 5次/分钟/IP |
| 注册 | 3次/分钟/IP |
| 发送验证码 | 1次/60秒/手机号, 10次/小时/IP |
| 验证验证码 | 5次/分钟/IP |
| 发帖 | 3次/分钟/用户 |
| 评论 | 10次/分钟/用户 |
| 点赞 | 30次/分钟/用户 |
| 通用 API | 60次/分钟/用户 |

---

## 7. 部署配置

### 7.1 Docker Compose

```yaml
version: '3.8'

services:
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/conf.d:/etc/nginx/conf.d
      - ./nginx/ssl:/etc/nginx/ssl
    depends_on:
      - api

  api:
    build: ./apps/api
    environment:
      - NODE_ENV=production
      - DB_HOST=postgres
      - REDIS_HOST=redis
    depends_on:
      - postgres
      - redis

  postgres:
    image: postgres:16-alpine
    volumes:
      - pgdata:/var/lib/postgresql/data
    environment:
      - POSTGRES_DB=manjie
      - POSTGRES_PASSWORD=${DB_PASSWORD}

  redis:
    image: redis:7-alpine
    volumes:
      - redisdata:/data

  worker:
    build: ./apps/worker
    depends_on:
      - postgres
      - redis

  source-sync:
    build: ./apps/source-sync
    depends_on:
      - postgres

volumes:
  pgdata:
  redisdata:
```

### 7.2 域名规划

| 域名 | 用途 |
|------|------|
| api.manjie.xxx | API 服务 |
| admin.manjie.xxx | 管理后台 |
| source.manjie.xxx | 源注册表 CDN |
| cdn.manjie.xxx | 静态资源 CDN |

---

## 8. 目录索引

### 8.1 已生成文档

| 文件 | 内容 |
|------|------|
| `01_PROJECT_SPEC.md` | 项目规格说明书（本文的上层文档） |
| `02_ARCHITECTURE.md` | 架构设计文档（本文） |

### 8.2 待生成文档

| 文件 | 内容 | 优先级 |
|------|------|--------|
| `03_DATABASE_SCHEMA.md` | 完整数据库表定义 | Phase 0 |
| `04_API_SPEC.md` | 完整 API 接口定义 | Phase 0 |
| `05_AUTH_SPEC.md` | 认证系统详细设计 | Phase 0 |
| `06_COMMUNITY_SPEC.md` | 社区系统详细设计 | Phase 1 |
| `07_MANGA_REQUEST_SPEC.md` | 求漫/求源系统详细设计 | Phase 1 |
| `08_SOURCE_SYSTEM_SPEC.md` | 源系统详细设计 | Phase 1 |
| `09_VENERA_ADAPTER_SPEC.md` | Venera 兼容层设计 | Phase 1 |
| `10_UPDATE_SPEC.md` | 更新系统设计 | Phase 2 |
| `11_ANNOUNCEMENT_SPEC.md` | 公告系统设计 | Phase 2 |
| `12_ADMIN_SPEC.md` | 管理后台设计 | Phase 2 |
| `13_DESIGN_SYSTEM.md` | 设计系统/品牌指南 | Phase 0 |
| `14_FLUTTER_RULES.md` | Flutter 开发规范 | Phase 0 |
| `15_BACKEND_RULES.md` | 后端开发规范 | Phase 0 |
| `16_SECURITY_RULES.md` | 安全规则 | Phase 0 |
| `17_AI_CODING_RULES.md` | AI 编码规范 | Phase 0 |
| `18_TEST_PLAN.md` | 测试计划 | Phase 1 |
| `19_DOCKER_DEPLOYMENT.md` | Docker 部署指南 | Phase 2 |
| `20_CI_CD.md` | CI/CD 配置 | Phase 2 |
| `21_SOURCE_SYNC.md` | 源同步机制 | Phase 1 |
| `22_ROADMAP.md` | 开发路线图 | Phase 0 |