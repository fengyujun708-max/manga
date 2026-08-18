/// MangaVerse 发现页 — Netflix 式首页
///
/// 结构：
/// 1. 透明渐变 App Bar（搜索 + 插件切换）
/// 2. Hero 轮播（精选漫画，自动播放）
/// 3. 继续阅读（本地阅读历史，横向滚动）
/// 4. 插件内容区（comic-section-list 渲染为 Netflix 式行）
/// 5. 插件管理入口

import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:mangaverse/config/theme/mangaverse_theme.dart';
import 'package:mangaverse/config/router/router.gr.dart';
import 'package:mangaverse/i18n/strings.g.dart';
import 'package:mangaverse/widgets/mv_hero_carousel.dart';
import 'package:mangaverse/widgets/mv_card.dart';
import 'package:mangaverse/widgets/toast.dart';

import 'package:mangaverse/page/discover/cubit/discover_cubit.dart';
import 'package:mangaverse/page/discover/service/discover_router.dart';
import 'package:mangaverse/page/discover/view/discover_scheme_renderer.dart';
import 'package:mangaverse/page/search/cubit/search_cubit.dart';
import 'package:mangaverse/plugin/plugin_registry_service.dart';
import 'package:mangaverse/object_box/object_box.dart';
import 'package:mangaverse/object_box/objectbox.g.dart';
import 'package:mangaverse/main.dart';
import 'package:mangaverse/type/enum.dart';
import 'package:mangaverse/util/json/json_value.dart';
import 'package:mangaverse/service/recommend_client.dart';
import 'package:mangaverse/widgets/recommend_row.dart';

@RoutePage()
class DiscoverPage extends StatelessWidget {
  const DiscoverPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (_) => DiscoverCubit()..load(),
      child: const _DiscoverView(),
    );
  }
}

class _DiscoverView extends StatefulWidget {
  const _DiscoverView();

  @override
  State<_DiscoverView> createState() => _DiscoverViewState();
}

class _DiscoverViewState extends State<_DiscoverView> {
  late ScrollController _scrollController;
  double _scrollOffset = 0;
  List<RecommendItem> _recommendations = [];
  bool _isLoadingRecs = true;

  @override
  void initState() {
    super.initState();
    _scrollController = ScrollController();
    _scrollController.addListener(() {
      setState(() => _scrollOffset = _scrollController.offset);
    });
    _loadRecommendations();
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _loadRecommendations() async {
    try {
      final items = await RecommendService.getHomeRecommendations(limit: 12);
      if (mounted) setState(() { _recommendations = items; _isLoadingRecs = false; });
    } catch (_) {
      if (mounted) setState(() => _isLoadingRecs = false);
    }
  }

  void _search(BuildContext context) {
    context.pushRoute(
      SearchRoute(
        searchState: SearchStates.initial(),
        aggregateMode: true,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent,
      extendBodyBehindAppBar: true,
      appBar: _buildAppBar(context),
      body: RefreshIndicator(
        color: MangaVerseColors.accent,
        backgroundColor: MangaVerseColors.surface,
        onRefresh: () => context.read<DiscoverCubit>().reload(),
        child: CustomScrollView(
          controller: _scrollController,
          physics: const BouncingScrollPhysics(),
          slivers: [
            // Hero 轮播
            SliverToBoxAdapter(child: _buildHeroCarousel(context)),

            // 推荐阅读
            SliverToBoxAdapter(child: _buildRecommendations()),

            // 继续阅读
            SliverToBoxAdapter(child: _buildContinueReading(context)),

            // 插件内容区
            SliverToBoxAdapter(child: _buildPluginContent(context)),

            // 底部间距
            const SliverToBoxAdapter(
              child: SizedBox(height: 120),
            ),
          ],
        ),
      ),
      floatingActionButtonLocation: FloatingActionButtonLocation.endFloat,
      floatingActionButton: FloatingActionButton(
        backgroundColor: MangaVerseColors.accent,
        foregroundColor: Colors.white,
        elevation: 4,
        tooltip: t.discover.search,
        onPressed: () => _search(context),
        child: const Icon(Icons.search),
      ).animate().scale(duration: 300.ms, curve: Curves.easeOutBack),
    );
  }

  /// 透明渐变 App Bar
  PreferredSizeWidget _buildAppBar(BuildContext context) {
    final opacity = (_scrollOffset / 200).clamp(0.0, 1.0);
    return PreferredSize(
      preferredSize: const Size.fromHeight(kToolbarHeight),
      child: AnimatedContainer(
        duration: 200.ms,
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              Color.lerp(Colors.transparent, Colors.black, opacity)!,
              Colors.transparent,
            ],
          ),
        ),
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Row(
              children: [
                // Logo
                Text(
                  'MANGAVERSE',
                  style: TextStyle(
                    fontSize: 22,
                    fontWeight: FontWeight.w900,
                    color: Color.lerp(
                      Colors.white,
                      MangaVerseColors.accent,
                      0.3,
                    ),
                    letterSpacing: 2,
                  ),
                ).animate().fadeIn(duration: 500.ms),
                const Spacer(),
                // 搜索按钮
                GestureDetector(
                  onTap: () => _search(context),
                  child: Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.1),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(Icons.search, color: Colors.white, size: 22),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  /// Hero 轮播 — 从插件数据提取精选
  Widget _buildRecommendations() {
    return RecommendedRow(
      items: _recommendations,
      title: '为你推荐',
      subtitle: _isLoadingRecs ? '' : '基于阅读偏好',
      isLoading: _isLoadingRecs,
      onTap: (mangaId, source) {
        if (mangaId.isNotEmpty) {
          context.pushRoute(
            ComicInfoRoute(
              comicId: mangaId,
              from: source,
              type: ComicEntryType.comic,
            ),
          );
        }
      },
    );
  }

  Widget _buildHeroCarousel(BuildContext context) {
    return BlocBuilder<DiscoverCubit, DiscoverState>(
      buildWhen: (prev, curr) => prev.plugins != curr.plugins,
      builder: (context, state) {
        // 从活跃插件的 info 中提取 banner/featured 数据
        final heroItems = <MVHeroData>[];

        for (final entry in state.infoStates.entries) {
          final data = entry.value.data;
          if (data == null) continue;

          // 尝试从 sections 中提取 hero 内容
          final sections = asJsonList(data['sections']);
          for (final section in sections) {
            final sectionMap = asJsonMap(section);
            final title = sectionMap['title']?.toString() ?? '';
            final items = asJsonList(sectionMap['items']);
            if (items.isNotEmpty) {
              final firstItem = asJsonMap(items.first);
              final cover = asJsonMap(firstItem['cover']);
              final coverUrl = cover['url']?.toString() ?? '';
              if (coverUrl.isNotEmpty) {
                heroItems.add(MVHeroData(
                  imageUrl: coverUrl,
                  title: title.isNotEmpty
                      ? title
                      : firstItem['title']?.toString() ?? '',
                  description: firstItem['description']?.toString(),
                  badge: '精选',
                  onTap: () => DiscoverRouter.route(
                    context,
                    action: asJsonMap(firstItem['action']),
                    currentFrom: context.read<DiscoverCubit>().currentFrom,
                  ),
                ));
                break;
              }
            }
          }
        }

        // 如果没有插件数据，显示占位
        if (heroItems.isEmpty) {
          return SizedBox(
            height: 280,
            child: Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.auto_awesome,
                      color: MangaVerseColors.accent.withValues(alpha: 0.5),
                      size: 48),
                  const SizedBox(height: 12),
                  Text(
                    state.plugins.isEmpty
                        ? '还没有启用插件\n点击下方按钮安装插件'
                        : '正在加载内容...',
                    style: MangaVerseTypography.mutedText.copyWith(fontSize: 14),
                    textAlign: TextAlign.center,
                  ),
                  if (state.plugins.isEmpty) ...[
                    const SizedBox(height: 16),
                    _buildPluginStoreButton(context),
                  ],
                ],
              ),
            ),
          ).animate().fadeIn(duration: 500.ms);
        }

        return MVHeroCarousel(
          items: heroItems.take(5).toList(),
          height: 320,
        );
      },
    );
  }

  /// 继续阅读 — 从本地历史提取
  Widget _buildContinueReading(BuildContext context) {
    final query = objectbox.unifiedHistoryBox
        .query(UnifiedComicHistory_.deleted.equals(false))
        .order(UnifiedComicHistory_.lastReadAt, flags: Order.descending)
        .build();
    final histories = query.find().take(10).toList();
    query.close();

    if (histories.isEmpty) return const SizedBox.shrink();

    final cards = histories.map((h) {
      return MVCardData(
        imageUrl: h.cover,
        title: h.title,
        subtitle: h.chapterTitle.isNotEmpty ? h.chapterTitle : null,
        badge: '继续',
        onTap: () {
          context.pushRoute(
            ComicInfoRoute(
              comicId: h.comicId,
              from: h.source,
              type: ComicEntryType.normal,
            ),
          );
        },
      );
    }).toList();

    return MVContentRow(
      title: '继续阅读',
      items: cards,
      cardWidth: 130,
      cardHeight: 200,
    ).animate().fadeIn(duration: 500.ms, delay: 200.ms);
  }

  /// 插件内容区 — 渲染 scheme 为 Netflix 式行
  Widget _buildPluginContent(BuildContext context) {
    return BlocBuilder<DiscoverCubit, DiscoverState>(
      builder: (context, state) {
        if (state.plugins.isEmpty) {
          return const SizedBox.shrink();
        }

        // 为每个活跃插件渲染内容
        final activePlugins = state.plugins.values
            .where((p) => p.isActive)
            .toList();

        if (activePlugins.isEmpty) {
          return Padding(
            padding: const EdgeInsets.all(20),
            child: Center(
              child: Column(
                children: [
                  Icon(Icons.extension,
                      color: MangaVerseColors.mutedForeground, size: 40),
                  const SizedBox(height: 12),
                  Text(
                    '启用插件开始探索漫画',
                    style: MangaVerseTypography.mutedText,
                  ),
                  const SizedBox(height: 16),
                  _buildPluginStoreButton(context),
                ],
              ),
            ),
          );
        }

        return Column(
          children: activePlugins.map((plugin) {
            final infoState = state.infoStates[plugin.uuid];
            if (infoState == null ||
                infoState.loading ||
                infoState.data == null) {
              return const SizedBox.shrink();
            }

            return _buildPluginScheme(
              context,
              pluginUuid: plugin.uuid,
              data: infoState.data!,
              cubit: context.read<DiscoverCubit>(),
            );
          }).toList(),
        );
      },
    );
  }

  Widget _buildPluginScheme(
    BuildContext context, {
      required String pluginUuid,
      required Map<String, dynamic> data,
      required DiscoverCubit cubit,
    }) {
    final from = cubit.currentFrom;
    final body = asJsonMap(data['body']);
    final scheme = asJsonMap(data['scheme']);

    // 如果有 scheme 结构，用 renderer
    if (scheme.isNotEmpty) {
      final renderer = DiscoverSchemeRenderer();
      final title = renderer.title(scheme, '');

      return Padding(
        padding: const EdgeInsets.only(top: 16),
        child: renderer.buildPage(
          context,
          from: from,
          scheme: scheme,
          data: data,
          onReachBottom: () async {},
          onAction: (action) => DiscoverRouter.route(
            context,
            action: action,
            currentFrom: from,
          ),
          isLoadingMore: false,
          showLoadMoreRetry: false,
          onRetryLoadMore: () {},
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
        ),
      );
    }

    // 回退：直接从 sections 构建内容行
    final sections = asJsonList(data['sections']);
    if (sections.isEmpty) return const SizedBox.shrink();

    return Column(
      children: sections.map((section) {
        final sectionMap = asJsonMap(section);
        final title = sectionMap['title']?.toString() ?? '';
        final items = asJsonList(sectionMap['items']);

        final cards = items.map((item) {
          final itemMap = asJsonMap(item);
          final cover = asJsonMap(itemMap['cover']);
          return MVCardData(
            imageUrl: cover['url']?.toString() ?? '',
            title: itemMap['title']?.toString() ?? '',
            subtitle: itemMap['author']?.toString(),
            onTap: () => DiscoverRouter.route(
              context,
              action: asJsonMap(itemMap['action']),
              currentFrom: from,
            ),
          );
        }).toList();

        return MVContentRow(
          title: title,
          items: cards,
          onSeeAll: () {
            final action = asJsonMap(sectionMap['action']);
            DiscoverRouter.route(
              context,
              action: action,
              currentFrom: from,
            );
          },
        );
      }).toList(),
    );
  }

  Widget _buildPluginStoreButton(BuildContext context) {
    return GestureDetector(
      onTap: () => context.pushRoute(const PluginStoreRoute()),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
        decoration: BoxDecoration(
          gradient: MangaVerseColors.accentGradient,
          borderRadius: BorderRadius.circular(8),
          boxShadow: [
            BoxShadow(
              color: MangaVerseColors.accent.withValues(alpha: 0.3),
              blurRadius: 12,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.store, color: Colors.white, size: 20),
            const SizedBox(width: 8),
            Text(
              '插件商店',
              style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w700,
                color: Colors.white,
              ),
            ),
          ],
        ),
      ),
    ).animate().scale(duration: 300.ms, curve: Curves.easeOutBack);
  }
}
