# 漫界 — 项目规格说明书

> 产品名：漫界（Manjie）
> 版本：v1.0.0（规划中）
> 本文档目标：AI Coding Agent 可直接执行的第一份工程规格

---

## 1. 项目定位

漫界 = 漫画阅读器 + 漫画源平台 + 用户社区 + 求漫/求源平台 + 内容发现平台 + 云端源管理系统

**不是** Breeze 二改，而是以 Breeze 为阅读器内核，重构为拥有独立品牌、独立后端、独立账号体系、独立社区、独立源管理平台的完整产品。

---

## 2. 技术栈决策

| 模块 | 技术选型 | 理由 |
|------|----------|------|
| 客户端 | Flutter (Dart) | Breeze 已是 Flutter，复用生态 |
| 后端 | NestJS (TypeScript) | TS 生态与 Flutter 接近，AI 生成一致性好 |
| 数据库 | PostgreSQL | 成熟、稳定、全文搜索 |
| 缓存 | Redis | 验证码、Session、Rate Limit |
| 存储 | S3/OSS（阿里云 OSS） | 头像、图片、附件 |
| CDN | Cloudflare / 阿里云 CDN | 源分发 |
| 管理后台 | React + Next.js | 与 NestJS 共享 TS 类型 |
| 搜索 | PostgreSQL → Meilisearch | 第一阶段用 PG，后续迁移 |
| 部署 | Docker + Docker Compose | 阿里云 ECS 部署 |
| CI/CD | GitHub Actions | 自动构建、测试、发布 |
| 监控 | Sentry + 自建 | 错误追踪 |

---

## 3. 许可证边界策略

### 3.1 引入的第三方代码

| 项目 | 许可证 | 使用方式 | 义务 |
|------|--------|----------|------|
| Breeze (deretame/Breeze) | MPL-2.0 | 阅读器内核，修改后使用 | 修改过的 MPL 文件必须开源；可与其他文件不同许可证 |
| Venera Source API 设计参考 | 仅参考接口设计 | 不直接复制代码 | 无义务 |
| Venera JS 源适配 | GPL-3.0 | 兼容层运行（不修改源本身） | 源本身保持 GPL-3.0，适配器代码需兼容 |

### 3.2 MPL-2.0 合规要求

对于 Breeze 源码中**修改过的文件**：
- 必须公开修改后的版本
- 必须在修改过的文件中保留 MPL-2.0 许可证声明
- 不修改的、新写的文件可以任意许可证

### 3.3 漫界品牌策略

- **UI/产品品牌**：完全重写，无 Breeze 痕迹
- **代码架构**：重新组织，目录结构、命名空间全部变化
- **业务逻辑**：完全自己设计（账号、社区、求漫、求源）
- **第三方许可**：在 `LICENSES/` 目录保留完整的 MPL-2.0、GPL-3.0 原文和版权声明
- **App 关于页面**：显示「本应用使用了以下开源项目」和对应许可证

---

## 4. 总体架构

```
┌─────────────────────────────────────────────┐
│              漫界 App (Flutter)               │
│  Android / iOS / Windows / macOS / Linux     │
└──────────────────┬──────────────────────────┘
                   │
          HTTPS / WebSocket
                   │
┌──────────────────┴──────────────────────────┐
│            Cloudflare / Nginx                │
└──────┬───────────────────────────┬──────────┘
       │                           │
┌──────▼──────┐          ┌────────▼────────┐
│  漫界 API    │          │ 漫界 Source CDN  │
│  NestJS     │          │ Source Registry   │
└──────┬──────┘          └────────┬────────┘
       │                          │
┌──────┼──────────┐      ┌───────┼────────┐
│      │          │      │       │        │
▼      ▼          ▼      ▼       ▼        ▼
PG   Redis  OSS/S3    Breeze源 Venera源 自研源
```

---

## 5. 客户端架构（Flutter）

```
lib/
├── app/
│   ├── app.dart
│   ├── router/
│   ├── theme/
│   ├── localization/
│   └── config/
│
├── core/
│   ├── network/
│   ├── storage/
│   ├── security/
│   ├── update/
│   ├── analytics/
│   └── error/
│
├── features/
│   ├── auth/
│   ├── home/
│   ├── discover/
│   ├── reader/
│   ├── library/
│   ├── community/
│   ├── request/
│   ├── sources/
│   ├── notification/
│   ├── profile/
│   └── settings/
│
├── plugins/
│   ├── runtime/
│   ├── manager/
│   ├── registry/
│   └── compatibility/
│
├── models/
├── repositories/
└── services/
```

### 5.1 数据流原则

```
UI
 ↓
Controller / ViewModel
 ↓
Repository
 ↓
API Client
 ↓
Backend
```

**禁止**：UI 直接访问 API、直接访问数据库、直接请求 HTTP。

---

## 6. 后端架构（NestJS）

```
apps/
├── api/                    # 主 API 服务
│   ├── src/
│   │   ├── auth/
│   │   ├── user/
│   │   ├── comic/
│   │   ├── community/
│   │   ├── request/
│   │   ├── source/
│   │   ├── notification/
│   │   ├── admin/
│   │   └── common/
│   └── main.ts
│
├── worker/                 # 后台任务
│   ├── source-sync/
│   ├── notification/
│   └── cleanup/
│
└── admin/                  # 管理后台 API
```

---

## 7. 数据库核心表

### 7.1 用户系统
- `users`
- `user_devices`
- `user_sessions`
- `user_roles`
- `verification_codes`
- `password_reset_tokens`

### 7.2 漫画系统
- `comics`
- `comic_sources`
- `comic_source_bindings`
- `chapters`
- `comic_cache`

### 7.3 阅读系统
- `reading_history`
- `favorites`
- `favorite_folders`
- `downloads`

### 7.4 社区系统
- `posts`
- `post_comments`
- `post_likes`
- `post_favorites`
- `post_comic_refs`
- `post_attachments`

### 7.5 求漫/求源
- `manga_requests`
- `source_requests`

### 7.6 通知系统
- `notifications`
- `announcements`

### 7.7 源平台
- `source_registry`
- `source_versions`
- `source_sync_jobs`
- `source_test_results`

### 7.8 管理/运维
- `reports`
- `moderation_actions`
- `bans`
- `app_versions`
- `remote_configs`
- `audit_logs`

---

## 8. API 风格

- REST + WebSocket
- JWT 认证（Access Token + Refresh Token）
- 统一响应格式：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

- 错误码定义在 `common/error-codes.ts`
- 分页统一使用 `?page=1&limit=20`

---

## 9. 安全基线

### 9.1 密码
- 算法：Argon2id
- 禁止明文存储
- 禁止密码明文日志

### 9.2 验证码
- Redis 存储，有效期 5 分钟
- 发送频率限制：60秒/次
- IP 限制：10次/小时
- 手机号限制：5次/小时
- 防爆破：错误3次后锁定15分钟

### 9.3 API 安全
- 所有 API 限流（Rate Limit）
- CORS 白名单
- 请求体大小限制
- SQL 注入防护（TypeORM 参数化查询）
- XSS 防护（输入 sanitize）

### 9.4 源安全
- JS 沙箱运行（禁止文件系统、Shell、数据库、用户 Token 访问）
- 源之间隔离
- 源更新需测试通过

---

## 10. 部署架构

```
服务器：阿里云 ECS (39.106.192.137)
系统：Ubuntu 22.04 LTS
容器化：Docker + Docker Compose

服务：
├── nginx               # 反向代理
├── api                 # NestJS API 服务
├── admin               # 管理后台
├── worker              # 后台任务
├── postgres            # 数据库
├── redis               # 缓存
├── source-sync         # 源同步服务
└── monitoring          # 监控

域名规划：
├── api.manjie.xxx
├── admin.manjie.xxx
├── source.manjie.xxx
└── cdn.manjie.xxx
```

---

## 11. 开发阶段（Phase）

| Phase | 内容 | 预估 |
|-------|------|------|
| Phase 0 | 法务审计 + 架构设计 + 文档 | 1-2天 |
| Phase 1 | 漫界基础壳（Branding/Theme/路由/网络层） | 2-3天 |
| Phase 2 | 用户系统（注册/登录/验证码/密码） | 3-4天 |
| Phase 3 | 漫画阅读器（首页/搜索/详情/阅读器/书架） | 5-7天 |
| Phase 4 | 源系统（Registry/Manager/Adapter/沙箱） | 3-4天 |
| Phase 5 | 社区系统（帖子/评论/点赞/收藏/通知） | 4-5天 |
| Phase 6 | 求漫/求源系统 | 2-3天 |
| Phase 7 | 管理后台 | 3-4天 |
| Phase 8 | 更新系统 + Remote Config | 1-2天 |
| Phase 9 | CI/CD + 自动化 + 监控 | 2-3天 |

---

## 12. 下一步执行

详见 `02_ARCHITECTURE.md` — 详细的架构文档，包含每个模块的目录结构、接口定义、数据流图。