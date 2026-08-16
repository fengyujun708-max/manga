# MangaVerse 项目索引

> 用途：Phase 1 清理时定位模块与引用，避免重复扫描。基于 2026-08-13 工作区状态。

## 总体状态

- git HEAD: `181131d`（第一批：ReplaceRule 死代码 + stats 全局统计页）
- 已完成删除：widget（`0b2f263`）、sync（`4c8f0ad`）、ReplaceRule 死代码 + stats 全局统计页（`181131d`）、core/github 更新检查（进行中）
- 编译基线：`./gradlew :app:compileDebugKotlin`（JDK17，`-Xmx3g`，in-process），约 8-11 分钟
- 数据库版本：76（`app/schemas/com.mangaverse.app.core.db.MangaDatabase/76.json`）

## 模块清单（`app/src/main/kotlin/com/mangaverse/app/`）

| 模块 | 规模 | 分类 | 状态 |
|------|------|------|------|
| core/replace | 2 文件 | 死代码 | 已删（181131d） |
| stats/ui 全局统计页 | 5 文件 | 阅读统计 UI | 已删（181131d） |
| core/github | 3 文件（保留 VersionId） | 更新检查产品层 | 已删 AppUpdateRepository+AppVersion |
| browser | 13 文件 | 源加载容错（cloudflare+WebView），部分保留 | P0 只删 AdListUpdateService |
| remotelist | 2 文件 | 漫画线路列表核心 | 保留 |
| stats data/domain | - | 阅读统计数据层（2 表+阅读器/详情/Entity 耦合） | 待评估（保留 for now） |
| shortcuts | - | 快捷入口 | 保留（AppShortcutManager 核心） |
| extensions | 18 文件 | 源加载/源管理核心（含 repo/ 子包，DB 表） | 保留（源系统依赖） |
| alternatives | 8 文件 | 源迁移/去重 | 评估 |
| space | 43 文件 | 空间/导航会话核心（广泛耦合） | 保留（核心导航架构） |
| widget | - | 已删 | 完成 |
| sync | - | 已删 | 完成 |
| tracker / tracking / scrobbling | 3 模块 | 外部追踪（高耦合，需解耦计划） | 见 CLEANUP_TRACKING_PLAN.md |

## 核心保留功能（禁止破坏）

阅读器（reader/）、详情（details/）、章节（reader/chapters）、搜索（search/）、书架（favourites/）、历史（history/）、下载（download/）、缓存（image/local/）、Entity（entitygraph/）、漫画线路（core/parser + parser-api + remotelist）、首页推荐（home/）、本地列表（local/）

## 常用符号定位

- 菜单/设置入口：`settings/SettingsActivity.kt`、`settings/SettingsRootSections.kt`、`settings/SettingsSearchHelper.kt`
- 导航：`core/nav/AppRouter.kt`、`main/ui/compose/AppRoutes.kt`、`main/ui/compose/AppNavGraph.kt`、`main/ui/navigation3/MainNavKey.kt`
- 数据库：`core/db/MangaDatabase.kt`（entity/DAO 注册 + migrations）、`core/db/migrations/`
- DI：`AppModule.kt`（`app/src/main/kotlin/com/mangaverse/app/AppModule.kt`）
- Manifest：`app/src/main/AndroidManifest.xml`
- 依赖：`app/build.gradle` + `gradle/libs.versions.toml`

## 关键引用事实（已验证）

- stats：`ReaderViewModel` 注入 StatsCollector；`DetailsViewModel` 注入 StatsRepository；`DetailsScreen` DetailsAction.OpenStatistics + DetailsStatsSheet；`HistoryScreen` 统计 chip；`EntityGraphRepository` 使用 WorkStatsDao（合并/迁移）；`WorkAggregate.stats` 仅被 StatsRepository 消费；备份在 `BackupRepository` + `StatisticBackup.kt`/`WorkStatisticBackup.kt`
- browser：`AdListUpdateService` 仅 MainActivity 启动 + manifest + SettingsSearchHelper "adblock" 条目；`OpenUrlConfirmActivity` 仅 `LegadoJavaAPI.openUrl()` 调用
- remotelist：`RemoteListViewModel` 被 AppNavGraph ContentListRoute、SearchContentListScreen、ContentListActivity、LocalListViewModel 继承——核心保留
- github（更新检查）：引用面为 settings/about（AboutSettingsRoute/ViewModel/Screen + AppUpdate 系列 + changelog）+ ErrorDetailsActivity + MainViewModel/MainActivity/MainMenuProvider/KototoroApp/KototoroTopBar + AppRouter.openAppUpdate + manifest AppUpdateActivity + menu action_app_update；VersionId.kt 保留（About 页版本显示），AppUpdateRepository.kt + AppVersion.kt 已删
- scrobbling/tracking/tracker 耦合面：见 CLEANUP_TRACKING_PLAN.md

## 编译/验证命令

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew :app:compileDebugKotlin -Dorg.gradle.jvmargs="-Xmx3g" -Pkotlin.compiler.execution.strategy=in-process --console=plain
```

后台终端：`mcaiBuiltin_background_terminal_create`，`memory_percent=60`，`cpu_percent=300`，timeout 25min，`| tail -40`。
