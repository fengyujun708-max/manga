# CLEANUP_TRACKING_PLAN：tracker / tracking / scrobbling 解耦删除计划

> 目标：解除依赖 → 替换依赖 → 删除模块。禁止直接暴力删除。
> 原则：先迁移核心引用到 MangaVerse 自身实现，再切断业务依赖，最后删除模块。

## 1. tracker 模块

### 对外功能
- 本地"在追"状态：追踪（TrackEntity）、追踪日志（TrackLogEntity）、每本漫画的追踪进度/评分/状态
- 驱动首页"最近更新"区块、详情页追踪 tab、列表过滤
- 新章节检查 Worker（TrackWorker 调度）

### 核心类
- `tracker/domain/TrackingRepository.kt`（+ TrackFeedCompatibility）
- `tracker/data/TracksDao` 相关（TrackEntity/TrackLogEntity 表）
- `tracker/work/`（TrackWorker 调度）
- `tracker/ui/`（追踪 UI）

### 被谁引用
- HomeViewModel（TrackingRepository：observeUpdatedContent/observeUpdatedContentCount → 首页"更新"区块）
- DetailsViewModel / DetailsScreen / DetailsHeader（追踪 tab）
- HistoryRepository、ContentListMapper、ContentGridModel 等（追踪徽章/进度）
- favourites（BindTrackingToEntitiesUseCase 绑定）
- entitygraph（Entity 关联追踪）
- WorkScheduleManager（TrackerSettings 调度）

### 漫画核心必须保留的引用
- 首页"更新"区块（Home 核心功能，roadmap 保留"首页推荐"）
- 详情页阅读进度展示
- 列表页"在追"徽章（用户自己的进度，非外部追踪）

### 可替换的引用
- 首页"更新"数据源：**依赖反转**——新建 `MangaUpdateRepository`（MangaVerse 自身数据，聚合历史/书签/本地变化），HomeViewModel 改为依赖它，替换 `trackingRepository.observeUpdatedContent*`
- 详情页追踪 tab → 保留本地"在追"状态（自身进度的实现保留在 tracker 数据层），删除外部同步

### 可以删除的代码
- 外部服务同步（MAL/AniList 等经 scrobbling 的推送）
- 新章节检查通知（TrackWorker 若纯外部检查）
- TrackerSettings 中的外部账号管理 UI

### 需要重构的代码
- HomeViewModel：`TrackingRepository` → `MangaUpdateRepository`（依赖反转）
- WorkScheduleManager：移除 trackerScheduler

### 替代方案
- 首页更新区块改为纯本地聚合（历史 + 书签 + 阅读记录），不再依赖追踪服务

### 删除顺序
1. 新建 MangaUpdateRepository，HomeViewModel 切到新数据源
2. 移除 TrackerSettings UI 与外部账号管理
3. 移除 TrackWorker 调度
4. 验证无引用后删除 tracker/ui 与外部相关
5. 保留 tracker 数据层（TrackEntity/TrackLogEntity）或迁移到本地进度表

## 2. tracking 模块

### 对外功能
- 外部追踪站点发现与匹配（TrackingSiteCacheRepository、DefaultTrackingSiteMatcher、AnimeOffline/MALSync 数据）
- 从外部站点把"追踪记录"绑定到本漫（BindTrackingToEntitiesUseCase）
- TrackingDiscoverActivity / TrackingAlternativeTrackersPanel

### 核心类
- `tracking/discovery/data/TrackingSiteCacheRepository.kt`
- `tracking/discovery/data/DefaultTrackingSiteMatcher.kt`
- `tracking/animeoffline/`、`tracking/malsync/`
- `favourites/domain/BindTrackingToEntitiesUseCase.kt`（依附）
- TrackingSiteItemEntity / TrackingSiteLinkEntity 表

### 被谁引用
- entitygraph/data/EntityGraphRepository、EntityOwnershipResolver、EntityGraphMigrationWorker（用 TrackingSiteDao 做实体归并锚点）
- details/ui/DetailsViewModel（追踪站点链接展示）
- favourites 迁移（SourceMigrationViewModel、MergeFavoriteEntitiesUseCase）
- alternatives/domain/MigrateUseCase

### 漫画核心必须保留的引用
- entitygraph 的实体归并锚点（如果依赖 tracking_site 表做去重）

### 可替换的引用
- 外部站点匹配可被 MangaVerse 自身"漫画指纹 ComicID"替换（roadmap Phase 1.5 内容智能化方向）

### 可以删除的代码
- animeoffline / malsync 外部数据源
- TrackingDiscoverActivity / TrackingAlternativeTrackersPanel

### 需要重构的代码
- EntityGraphRepository 的 TrackingSiteDao 使用点（用本地 EntityRecord 锚点替代，或确认 entitygraph 是否真正依赖该表做归并）

### 替代方案
- 实体归并改为纯本地规则（作者+标题+封面相似度），或保留 tracking_site 表作为内部归并缓存（改名为 entity_alias）

### 删除顺序
1. 确认 entitygraph 对 TrackingSiteDao 的依赖实质
2. 若为归并缓存：重命名保留为内部能力；若可替换：迁移到本地规则
3. 删 tracking/discovery、animeoffline、malsync
4. 删 BindTrackingToEntitiesUseCase、TrackingDiscoverActivity、TrackingAlternativeTrackersPanel

## 3. scrobbling 模块

### 对外功能
- 外部追番追漫服务：AniList、MAL、Kitsu、Bangumi、Shikimori、Simkl、MangaUpdates、Discord（进度/评分推送）
- ScrobblerConfigActivity（服务配置登录）、ScrobblingSelectorSheet（选择推送到哪个服务）、ScrobblingInfoSheet
- ScrobblingDao/ScrobblingEntity 表、ScrobblingBackup 备份模型

### 核心类
- `scrobbling/ScrobblingModule.kt`
- `scrobbling/common/`（Scrobbler、ScrobblerRepositoryMap、ScrobblingDao/Entity、ScrobblerStorage、ScrobblerUserProfileRepository、config/、selector/）
- `scrobbling/{anilist,mal,kitsu,bangumi,shikimori,simkl,mangaupdates,discord}/`

### 被谁引用
- details（ScrobblingInfoSheet、DetailsScreen 追踪 tab、LinkedTrackingItemUiModel）
- history/data/HistoryRepository、list/ui/model/Content*Model（阅读进度推送）
- home/HomeViewModel（推送触发）
- backups（ScrobblingBackup 备份恢复）
- entitygraph/work/EntityGraphMigrationWorker（getScrobblingDao）
- settings（UsersSettingsScreen、DiscordSettingsViewModel、TrackingUserAccountSummaryProvider、SettingsActivity）
- alternatives/domain/MigrateUseCase

### 漫画核心必须保留的引用
- 无。外部追踪推送全部可删（roadmap 明确删除"外部追踪"）

### 可替换的引用
- 阅读进度推送：将来可推送到 MangaVerse API（/api/v1/comics/{id}）而非外部服务
- backups 的 ScrobblingBackup：从备份导出/恢复格式中移除（向后兼容读取可保留）

### 可以删除的代码
- 全部 scrobbling 外部服务实现（anilist/mal/kitsu/bangumi/shikimori/simkl/mangaupdates/discord）
- ScrobblerConfigActivity/Screen/ViewModel、ScrobblingSelectorSheet、ScrobblingInfoSheet
- DiscordRpc、DiscordOAuthPkce、DiscordRepository

### 需要重构的代码
- SettingsActivity/UsersSettingsScreen：移除 scrobbling 入口
- backups：ScrobblingBackup 从备份模型删除（注意向后兼容：旧备份解析需容错跳过）
- DetailsScreen：移除 ScrobblingInfoSheet 入口
- 数据库：ScrobblingEntity 表删除 → Migration（DROP TABLE scrobblings）

### 替代方案
- 无（功能整体下线）；进度云同步归入 MangaVerse API 账号体系（Phase 2）

### 删除顺序
1. 先删 UI 入口（settings/discord、UsersSettings、Details 的 ScrobblingInfoSheet、selector）
2. 删配置 UI（ScrobblerConfig*）
3. 删外部服务实现（各站点 + ScrobblingModule 绑定）
4. 处理数据层：ScrobblingEntity/ScrobblingDao 删除 → Migration76To77（DROP TABLE scrobblings）
5. 备份模型清理 + 向后兼容容错
6. 验证 Home/Details/History/list 无引用

## 全局删除顺序汇总

1. 删除 scrobbling UI 层（settings/discord、UsersSettings、Details sheet、selector）
2. 删除 scrobbling 服务层（各站点实现 + Module 绑定）
3. scrobbling 数据层（表删除 + Migration）
4. tracking：确认 entitygraph 依赖 → 替换 → 删除 discovery/animeoffline/malsync + 依附文件
5. tracker：依赖反转（MangaUpdateRepository）→ 删 UI/Worker → 保留本地数据层
6. 每步之间最小编译验证，遇到数据库迁移/Reader/详情/Home 核心风险暂停报告
