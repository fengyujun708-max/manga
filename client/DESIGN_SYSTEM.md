# 漫界 Design System v2.0 — Cinematic Manga OS

## 设计定位
Premium Cinematic Manga Platform — Netflix 沉浸 + Apple 精致 + Pinterest 效率

## 色彩系统 (95% 中性 + 5% 品牌色)

### 基础色板
| Token | Hex | 用途 |
|-------|-----|------|
| `bg` | #08090B | 全局背景 (OLED 友好) |
| `surface1` | #101114 | 一级表面 (卡片/底栏) |
| `surface2` | #16181C | 二级表面 (输入框/hover) |
| `surface3` | #1C1E22 | 三级表面 (弹窗) |
| `overlay` | #FFFFFF0A | 玻璃叠加 (4% 白) |
| `overlayStrong` | #FFFFFF14 | 强玻璃 (8% 白) |

### 文字色阶
| Token | Hex | 用途 |
|-------|-----|------|
| `textPrimary` | #F5F5F5 | 主文字 |
| `textSecondary` | #A1A1AA | 次文字 |
| `textTertiary` | #71717A | 辅助文字 |
| `textDisabled` | #52525B | 禁用 |

### 品牌色 (仅 5% Accent)
| Token | Hex | 用途 |
|-------|-----|------|
| `accent` | #FF3B30 | 品牌红 (按钮/选中/进度) |
| `accentDim` | #FF3B3014 | 品牌红背景 (10%) |
| `success` | #34D399 | 成功 |
| `warning` | #FBBF24 | 警告 |
| `error` | #EF4444 | 错误 |

### 渐变
- `heroScrim`: transparent → bg (底→顶，给 Hero 渐隐到背景)
- `cardScrim`: transparent → surface1 (封面底部渐隐)

## 排版系统

### 字号
| Token | Size | Weight | Letter | 用途 |
|-------|------|--------|--------|------|
| `display` | 34 | w700 | -1.0 | Hero 标题 |
| `headline` | 24 | w700 | -0.5 | 页面标题 |
| `title` | 18 | w600 | -0.3 | 区块标题 |
| `body` | 15 | w400 | 0 | 正文 |
| `caption` | 13 | w400 | 0.2 | 辅助 |
| `micro` | 11 | w600 | 0.5 | 标签/徽章 |

### 字族
- 默认: SF Pro / system (Flutter 默认即可)
- 中文: 系统默认

## 间距系统 (4 的倍数)
| Token | Value |
|-------|-------|
| `xs` | 4 |
| `sm` | 8 |
| `md` | 12 |
| `lg` | 16 |
| `xl` | 20 |
| `2xl` | 24 |
| `3xl` | 32 |
| `4xl` | 48 |

## 圆角系统
| Token | Value | 用途 |
|-------|-------|------|
| `rSm` | 10 | 标签/小按钮 |
| `rMd` | 14 | 输入框/中按钮 |
| `rLg` | 20 | 卡片 |
| `rXl` | 28 | 大卡片/底栏 |
| `rFull` | 999 | 圆形/胶囊 |

## 阴影/高度系统
- 不使用 Material Elevation
- 使用: 纯黑阴影 + 玻璃模糊替代层级
| Level | Shadow | Blur |
|-------|--------|------|
| 0 | 无 | — |
| 1 | black 20% | 8 |
| 2 | black 35% | 20 |
| 3 | black 50% | 40 |

## 玻璃系统 (替代 Elevation)
| Token | Blur | Fill | Border |
|-------|------|------|--------|
| `glass` | 50 | white 4% | white 8% 0.5px |
| `glassStrong` | 50 | white 8% | white 12% 0.5px |
| `glassSheet` | 60 | white 6% | white 10% 0.5px |

## 动效系统
| Category | Duration | Curve | 用途 |
|----------|----------|-------|------|
| Micro | 100-160ms | easeOut | 按压/切换 |
| Standard | 200ms | easeOutCubic | 页面元素入场 |
| Emphasis | 350ms | easeInOutCubic | Hero/转场 |
| Hero | 600-900ms | easeOutBack | 首页 Hero |

## 组件系统
1. `CinematicHero` — 全屏沉浸式 Hero (非 Card)
2. `ComicCard` — 封面+标题+元数据 (长按菜单)
3. `ContinueReadingCard` — 横向阅读进度卡
4. `SectionHeader` — 标题 + "更多" + 动态下划线
5. `GlassBar` — 底部导航 (Blur + 胶囊指示器)
6. `GlassInput` — 毛玻璃输入框
7. `SpringButton` — 弹性按钮 (scale 0.97 + glow)
8. `ShimmerSkeleton` — 骨架屏加载
9. `EmptyState` — 极简空状态
10. `ComicRefCard` — 社区漫画引用卡

## 页面信息架构
1. Home: Hero → 继续阅读 → 热门 → 最新 → 编辑推荐 → 社区动态
2. Discover: 搜索入口 → 趋势 → 分类 → 已安装源
3. Library: 继续阅读 → 收藏 → 下载 → 历史 (支持多选)
4. Community: Feed 流 (帖子+漫画引用卡)
5. Profile: 统计概览 → 最近阅读 → 社区活动 → 设置

## 禁止清单
- 禁止 Material AppBar
- 禁止 Material BottomNavigationBar
- 禁止 Card widget
- 禁止大面积紫色/蓝色
- 禁止 12px 圆角 (已淘汰)
- 禁止静态 Hero (必须是动态的)
- 禁止普通 ComicCard (必须有长按/进度/状态)