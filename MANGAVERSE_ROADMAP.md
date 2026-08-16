# MangaVerse Enterprise Edition 总战略 V2.0

> 本文件是 MangaVerse 项目的最高优先级规划文档，所有 AI Agent 与本项目的一切修改都必须以此为纲。
>
> **最关键的判断（写给 AI 的第一句话）**：不要把 MangaVerse 做成 KotoToro 的修改版。KotoToro/Kotatsu 只是底层阅读技术的来源。目标是打造一个独立商业漫画平台。所有用户体验必须隐藏技术来源。客户端负责体验，服务器负责内容。删除所有面向开发者和极客的功能，保留阅读核心。
>
> 所有开发优先考虑：**稳定 > 功能数量；简单 > 复杂；用户体验 > 技术炫耀；低代码量 > 重复实现。**

## 产品定位（重新定义）

不要叫"漫画阅读器"，叫 **MangaVerse 漫画生态引擎**。

核心：内容聚合 + 智能分发 + 极速阅读 + 广告商业化 + 服务器控制。

一句话：KotoToro 只是底层阅读技术来源，产品目标是"中国版 Netflix 漫画"。

## 第一原则：客户端变轻，服务器变强

这是超过 KotoToro 的关键。

- 传统（错误）：App 直接承载漫画源、解析网页、漫画数据 → App 大、更新麻烦、容易失效、用户看到源。
- 新架构（正确）：**MangaVerse Cloud**（内容管理中心 / 漫画聚合系统 / AI 处理系统 / 广告系统 / 用户系统）→ Android App。**App 只是播放器。**

## 一、源码重构策略

不要 Fork 一个越来越乱的项目，采用"核心保留 + 产品重构"：

### 必须保留（黄金，不要动）
1. **Reader Engine**：Webtoon / Pager / RTL / LTR / 图片预加载。
2. **Manga Model 思想**：但重新定义数据模型：
   - 旧：Source → Manga → Chapter → Page
   - 新：Universe → Comic → Volume → Chapter → Page（为漫画宇宙扩展预留）
3. **Cache 系统**：漫画 App 体验 80% 来自缓存。

### 彻底删除旧思想
- 删除 **Source 概念**：用户永远不知道源。内部改叫 **Content Provider**（后台 Provider001/002/003，用户看到"MangaVerse 精选"）。

## 二、内容智能合并系统（差异化核心）

- 一个漫画多个重复 → 做**漫画指纹**（标题/作者/封面/章节数量/hash → 生成 ComicID）。
- 例：A 源《海贼王》+ B 源《One Piece》系统判断为同一漫画并合并。
- 最终：用户只看到一个漫画，内部含最快路线 / 高清路线 / 备用路线，自动切换。

## 三、漫画质量评分系统

每个漫画后台评分：质量分 = 清晰度 + 更新速度 + 章节完整度 + 失败率。首页优先展示高分。

## 四、首页智能推荐（Manga AI Ranking）

- 第一阶段（规则）：用户喜欢末世/复仇/热血 → 推荐同类。
- 第二阶段（服务器 AI）：记录观看时间、停留页数、收藏、跳出章节 → 兴趣模型。

## 五、用户系统

不用 Kotatsu 用户。用 **MangaVerse ID**：一个身份，数据含阅读历史、收藏、广告权益、主题、阅读习惯、推荐模型。登录方式：手机、邮箱、第三方。

## 六、广告系统（Manga Energy）

- 取消"看广告 30 分钟"的廉价感，改为 **Manga Energy**（漫画能量）。
- 观看广告 +30 能量，阅读漫画消耗能量。后续可扩展：签到、任务、会员。

## 七、会员系统（V1 不做，但数据库预留）

- 等级：Free / Plus / Premium。
- 权益：广告减少、高清画质、AI 增强、提前阅读。

## 八、首次启动向导（像 Netflix，不像装软件）

1. 欢迎页：进入你的私人漫画空间。
2. 选择阅读习惯（不是技术配置）：条漫/日漫/韩漫、热血/恋爱 → 用于推荐。
3. 技术设置（图像增强等）隐藏进"高级体验"，不吓普通用户。
4. 服务器初始化：连接 MangaVerse 服务，下载必要配置与漫画路线，不能跳过。完成后写入 `initialized=true`。

## 九、UI/UX 规划

设计语言：**Netflix + Apple**，不模仿 KotoToro。

- 底栏五 tab：首页 / 探索 / 书架 / 任务 / 我的。
- 首页：视觉瀑布，不是列表（继续阅读 / 热门推荐 / 最新更新 / 分类 / 排行榜 / 猜你喜欢）。
- 漫画详情：Netflix 影视详情风格，顶部大封面 + 评分 + 标签 + 简介 + 开始阅读。
- 阅读页：极简，只保留点击区域。默认强制屏幕常亮 + 沉浸模式 + 预加载 10 页。

## 十、设置彻底商业化

不暴露技术。删除：源管理、缓存目录、代理、调试。保留：账号、阅读体验、外观、通知、会员、关于。

## 十一、服务器后台（MangaVerse Admin）

必须规划开发：
- 漫画管理：添加、修改、上下架。
- 路线管理：Provider 状态、速度、失败率。
- 广告：广告平台、奖励配置。
- 用户：封禁、权益、统计。
- 数据：热门漫画、用户增长、阅读时长。

## 十二、AI 编程管理规则（加强版）

AI 不允许一次改 100 个文件。每次任务必须声明：

```
任务编号:
影响模块:
修改文件:
预计修改量:
风险:
测试:
```

- 错误示范："重构主页"。
- 正确示范：`Task-HOME-001`，目标：替换 HomeBanner；允许：HomeScreen.kt / HomeViewModel.kt；禁止：修改 Reader。
- 禁止"扫描整个项目"，采用模块化任务。节省 Token，减少代码污染。

## 十三、代码质量规则

- 禁止重复代码。
- 网络统一：`ApiClient`；数据库统一：`Repository`；状态统一：`ViewModel`。
- 服务器地址配置化（BuildConfig.SERVER_URL），禁止硬编码（尤其禁止 10.0.2.2 等模拟器地址）。
- OkHttpClient 单例复用，禁止每次创建（newSSLContext 问题）。

## 十四、错误处理与环境体系

- 环境：dev / test / prod，服务器地址走 BuildConfig。

## 十五、性能目标

- 启动 < 2 秒；首页 < 1 秒；阅读秒开；缓存自动；APK < 30MB。

## 十六、未来路线

- **V1.0 商业阅读器**：聚合、阅读、账号、广告。
- **V1.5 内容智能化**：推荐、排行、AI 分类。
- **V2.0 生态**：创作者后台、原创漫画、VIP。
- **V3.0 漫画平台**：中国版 Netflix 漫画。

## 附录 A：Phase 1 删除清单（V1.0 保留）

按"稳定 > 功能数量"执行，优先删除无用代码，再开发新功能：

1. **小说相关**：Novel、Book、Text Reader、Novel Source。
2. **视频动漫相关**：Video、Anime、Player、Episode。
3. **原 Kotatsu 用户系统**：Cloud Sync、Account、Backup、External Tracking、AniList、MyAnimeList、Kitsu。
4. **高级设置**：Developer Settings、Advanced、Experimental、Debug、Shortcut、Gesture customization。
5. **本地复杂功能**：CBZ、Local Archive、Import Manga、Export Backup。

保留核心：Home、Search、Detail、Reader、Shelf、TaskCenter、Account、Download、Settings、Network、Ads。
