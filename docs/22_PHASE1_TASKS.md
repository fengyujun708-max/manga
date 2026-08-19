# 漫界 — Phase 1 开发任务清单

> 目标：完成漫界基础壳，打通客户端↔后端全链路
> 依赖：后端 API 已部署运行（Docker Compose 已启动）
> 预估：3-5 天（AI 全自动开发）

---

## P1.0 基础设施（先决条件）

### 1.0.1 服务器部署
- [ ] 安装 docker-compose 二进制到服务器
- [ ] 确认 PostgreSQL 和 Redis 正常运行
- [ ] 确认 API 容器启动成功
- [ ] 运行 `npm run migration:run` 自动建表
- [ ] 验证 `POST /v1/auth/register` 返回正常

### 1.0.2 本地开发环境
- [ ] 安装 Flutter SDK (3.5+)
- [ ] 安装 FVM 管理 Flutter 版本
- [ ] 配置 Android/iOS 开发环境
- [ ] 运行 `flutter pub get` 安装依赖
- [ ] 运行 `build_runner` 生成代码（freezed/json_serializable）

---

## P1.1 App 品牌化（Branding）

### 1.1.1 启动画面
- [ ] 创建 `assets/images/splash_logo.png`（漫界 Logo）
- [ ] 实现 `SplashPage`：Logo 淡入动画 + 渐变背景
- [ ] 启动时检查 Token 有效性 → 自动跳转首页/登录页
- [ ] 设置 `flutter_native_splash` 原生启动屏

### 1.1.2 图标与命名
- [ ] 生成各平台 App 图标（`flutter_launcher_icons`）
- [ ] 修改 Android `AndroidManifest.xml` 中 app_name → "漫界"
- [ ] 修改 iOS `Info.plist` 中 CFBundleDisplayName → "漫界"
- [ ] 设置包名（Android: `com.manjie.app`, iOS: `com.manjie.app`）

---

## P1.2 设计系统完整实现（Design System）

### 1.2.1 颜色系统
```
已完成：基础色板（primary/accent/surface/background）
需补充：
- 语义色：success/error/warning/info（各 4 级深浅）
- 状态色：active/inactive/disabled/hover
- 渐变预设：hero gradient / card gradient / tag gradient
- 透明层级：overlay 各层级不透明度定义
```

### 1.2.2 文字系统
```
需补充：
- 完整层级：display/large/medium/small + title/body/label 各 3 级
- 行高预设：1.2/1.4/1.6/1.8
- 字重映射：Light/Regular/Medium/SemiBold/Bold
- 字号阶梯：10/12/14/16/18/20/24/28/32/40
```

### 1.2.3 间距系统
- [ ] 定义 4/8/12/16/20/24/32/40/48/64 间距阶梯
- [ ] 定义内边距预设（card padding / page padding / section padding）
- [ ] 定义圆角阶梯（4/8/12/16/20/24 → pill）

### 1.2.4 通用组件库
- [ ] `ManjieButton`：filled/outlined/text/icon 四种变体，loading 状态
- [ ] `ManjieCard`：基础卡片，支持按压反馈、阴影层级
- [ ] `ManjieTextField`：输入框，支持 label/error/icon/clear
- [ ] `ManjieChip`：标签组件，可选/可关闭/多色变体
- [ ] `ManjieDialog`：弹窗，支持 alert/confirm/input 模式
- [ ] `ManjieBottomSheet`：底部弹窗，支持拖拽关闭
- [ ] `ManjieShimmer`：骨架屏加载组件
- [ ] `ManjieEmptyState`：空状态，支持 icon + title + subtitle + action
- [ ] `ManjieErrorState`：错误状态，支持重试按钮
- [ ] `ManjieToast`：轻提示（success/error/warning/info）

---

## P1.3 网络层完善（Network Layer）

### 1.3.1 API Client 增强
- [ ] 完成 Token 自动刷新闭环（401 拦截 → 刷新 → 重试）
- [ ] 统一错误处理：解析后端错误码 → 中文提示
- [ ] 请求/响应日志（debug 模式）
- [ ] 网络状态监听（`connectivity_plus`）
- [ ] 请求队列（离线时缓存，在线后重发）

### 1.3.2 API 接口对接
- [ ] 创建 `api_endpoints.dart` 集中管理所有接口路径
- [ ] 实现 `AuthApi`：sendCode / register / login / smsLogin / refresh / logout
- [ ] 实现 `UserApi`：getProfile / updateProfile / changePassword
- [ ] 实现 `ComicApi`：getHomeFeed / getComicDetail / getChapters / search
- [ ] 实现 `CommunityApi`：getPosts / getPostDetail / createPost / addComment

### 1.3.3 WebSocket 连接
- [ ] 建立 WebSocket 连接（认证后自动连接）
- [ ] 自动重连机制（指数退避）
- [ ] 消息路由：通知/评论/点赞实时推送

---

## P1.4 认证流程完整实现（Auth Flow）

### 1.4.1 登录页面完善
- [ ] 密码登录：手机号 + 密码 → 跳转首页
- [ ] 验证码登录：手机号 + 验证码 → 自动注册/登录
- [ ] 忘记密码：手机号 + 验证码 → 设置新密码
- [ ] 登录状态持久化：Token 存 SecureStorage
- [ ] 自动登录：启动时检查 Token → 有效则直接进首页

### 1.4.2 注册页面
- [ ] 三步骤注册：手机号 → 验证码 → 设置密码/昵称
- [ ] 表单验证：手机号格式、密码强度（8位+大小写+数字）
- [ ] 注册成功 → 自动登录 → 跳转首页

### 1.4.3 Token 管理
- [ ] 双 Token 策略：Access Token(15min) + Refresh Token(7天)
- [ ] 刷新 Token 轮换（每次刷新都会换发新的 Refresh Token）
- [ ] 退出登录：清除 Token + 通知服务端作废 Session

---

## P1.5 首页完整实现（Home Page）

### 1.5.1 Hero Banner
- [ ] 后端接口：`GET /v1/home/banner` → 返回 Banner 列表
- [ ] 自动轮播（3秒间隔，手指暂停）
- [ ] 指示器（圆点/进度条变体）
- [ ] 点击跳转漫画详情页
- [ ] 缓存策略：启动时先显示缓存，再拉取新数据

### 1.5.2 推荐板块
- [ ] 后端接口：`GET /v1/home/sections` → 返回多板块数据
- [ ] 板块类型：继续阅读 / 最近更新 / 猜你喜欢 / 热门漫画 / 编辑推荐
- [ ] 横向滚动卡片列表（支持懒加载）
- [ ] 每个板块显示 "查看全部 →" 按钮
- [ ] 空状态处理：继续阅读为空时隐藏该板块

### 1.5.3 漫画卡片组件
- [ ] 封面图（`CachedNetworkImage` + 渐变色占位）
- [ ] 标题（最多 1 行，超出省略）
- [ ] 更新话数/评分标签
- [ ] 点击 → 漫画详情页
- [ ] 长按 → 操作菜单（收藏/下载/不再推荐）

---

## P1.6 搜索页面（Search）

### 1.6.1 搜索入口
- [ ] 首页顶部搜索图标 → 搜索页面
- [ ] 搜索历史记录（本地存储，最多 20 条）
- [ ] 热门搜索标签（后端接口）

### 1.6.2 搜索实现
- [ ] 后端接口：`GET /v1/comic/search?q=xxx&page=1&limit=20`
- [ ] 防抖搜索（300ms）
- [ ] 搜索结果网格展示
- [ ] 加载更多（上拉翻页）
- [ ] 空结果提示

---

## P1.7 漫画详情页（Comic Detail）

### 1.7.1 页面结构
```
┌─────────────────────────────────┐
│  ComicCover (大图 + 渐变遮罩)   │
│  返回按钮        收藏/分享按钮  │
│                                 │
│  标题 / 作者 / 评分             │
│  标签列表                       │
│  简介（展开/收起）              │
│                                 │
│  [开始阅读] [收藏] [下载]       │
├─────────────────────────────────┤
│  章节列表（正序/倒序切换）      │
│  ├── 第 125 话 ── 2025-01-15   │
│  ├── 第 124 话 ── 2025-01-10   │
│  └── ...                        │
├─────────────────────────────────┤
│  相关推荐                       │
│  社区讨论入口                   │
└─────────────────────────────────┘
```

### 1.7.2 接口
- [ ] `GET /v1/comic/:id` → 漫画详情
- [ ] `GET /v1/comic/:id/chapters` → 章节列表（分页）
- [ ] `POST /v1/comic/:id/favorite` → 收藏/取消收藏
- [ ] `GET /v1/comic/:id/recommend` → 相关推荐

---

## P1.8 阅读器骨架（Reader Shell）

### 1.8.1 阅读器基础
- [ ] 阅读器页面（全屏沉浸模式）
- [ ] 章节内容加载（支持第三方源代理）
- [ ] 翻页模式：左右翻页 / 上下滚动 / 瀑布流
- [ ] 阅读进度记录（自动保存到后端）
- [ ] 图片预加载（提前 3 页）

### 1.8.2 阅读器设置
- [ ] 亮度调节（滑动条）
- [ ] 色温调节（暖/冷）
- [ ] 背景色（白/米黄/灰/黑）
- [ ] 字体大小（仅文字模式）
- [ ] 翻页方向（LTR/RTL/TTB）

### 1.8.3 阅读器交互
- [ ] 点击中间 → 显示设置面板
- [ ] 点击左侧 → 上一页
- [ ] 点击右侧 → 下一页
- [ ] 长按 → 菜单（目录/书签/设置）
- [ ] 章节切换（上一章/下一章）
- [ ] 章节列表（侧边弹出）

---

## P1.9 书架页面（Library）

### 1.9.1 书架功能
- [ ] 收藏列表（网格/列表切换）
- [ ] 正在阅读（带进度条）
- [ ] 已下载（离线章节管理）
- [ ] 自定义收藏夹（新建/重命名/删除）
- [ ] 漫画更新提示（红点 + 数量）

### 1.9.2 接口
- [ ] `GET /v1/favorites` → 收藏列表
- [ ] `GET /v1/history` → 阅读历史
- [ ] `DELETE /v1/favorites/:id` → 取消收藏
- [ ] `POST /v1/favorites/folders` → 管理收藏夹

---

## P1.10 发现页面（Discover）

### 1.10.1 功能
- [ ] 分类标签（水平滚动选择）
- [ ] 漫画网格（3列瀑布流）
- [ ] 筛选（排序：最新/最热/评分最高）
- [ ] 搜索入口

### 1.10.2 接口
- [ ] `GET /v1/comic/discover?category=xxx&sort=xxx&page=1`
- [ ] `GET /v1/comic/categories` → 分类列表

---

## P1.11 设置页面（Settings）

### 1.11.1 设置项
- [ ] 阅读设置：翻页方向/背景色/亮度/预加载数量
- [ ] 下载设置：下载路径/同时下载数/图片质量
- [ ] 缓存管理：查看缓存大小/一键清理
- [ ] 数据同步：WebDAV 配置（可选）
- [ ] 关于页面：版本号/许可证/开源项目列表
- [ ] 检查更新（调用后端接口）

---

## P1.12 个人中心（Profile）

### 1.12.1 功能
- [ ] 用户信息展示（头像/昵称/手机号）
- [ ] 编辑资料（修改昵称/头像/手机号）
- [ ] 阅读统计（总阅读时长/已读漫画数）
- [ ] 消息中心入口
- [ ] 漫画源管理入口
- [ ] 退出登录

---

## P1.13 错误处理与日志（Error Handling）

### 1.13.1 全局错误处理
- [ ] 统一异常捕获（`runZonedGuarded`）
- [ ] 网络错误提示（友好中文文案）
- [ ] 空数据状态（占位图 + 引导文案）
- [ ] 重试机制（失败自动重试 3 次）

### 1.13.2 日志系统
- [ ] 分级日志（debug/info/warn/error）
- [ ] 日志文件写入（调试用）
- [ ] 崩溃日志收集（Sentry 接入）

---

## P1.14 验收标准

### 14.1 功能验收
- [ ] 用户可完成注册 → 登录 → 浏览首页 → 搜索漫画 → 查看详情 → 收藏
- [ ] 阅读器可正常翻页 → 设置 → 跳转章节
- [ ] 书架可显示收藏 → 更新提示 → 阅读历史
- [ ] 个人中心可查看/编辑资料 → 退出登录

### 14.2 性能验收
- [ ] 首页加载 < 2秒（首屏）
- [ ] 漫画列表滚动 60fps
- [ ] 图片缓存命中率 > 80%
- [ ] 翻页响应 < 200ms

### 14.3 异常场景
- [ ] 无网络 → 友好提示 + 缓存展示
- [ ] Token 过期 → 自动刷新（无感）
- [ ] 服务器 500 → 错误页 + 重试按钮
- [ ] 空数据 → 占位引导

---

## 执行优先级

```
P1.0 → P1.1 → P1.2 → P1.3 →  P1.4
                                   ↓
P1.5 → P1.6 → P1.7 → P1.8 → P1.9 → P1.10 → P1.11 → P1.12
                                                         ↓
P1.13 → P1.14
```

**关键路径**：P1.0(部署) → P1.2(组件) → P1.4(登录) → P1.5(首页) → P1.8(阅读器)

**并行执行**：
- P1.1(品牌化) + P1.2(组件) + P1.3(网络层) 可并行
- P1.6(搜索) + P1.9(书架) + P1.10(发现) 可并行
- P1.11(设置) + P1.12(个人中心) 可并行