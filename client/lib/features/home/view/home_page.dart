import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../../app/ds.dart';
import '../../core/network/api_client.dart';
import 'package:get_it/get_it.dart';

/// 首页 — Cinematic Immersive
/// 全屏 Hero (动态模糊背景 + 渐变遮罩) + 继续阅读 + 板块横滑
class HomePage extends StatefulWidget {
  const HomePage({super.key});
  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> with TickerProviderStateMixin {
  List<dynamic> _heroSlides = [];
  List<dynamic> _continueReading = [];
  List<dynamic> _popular = [];
  List<dynamic> _latest = [];
  bool _loading = true;
  late PageController _heroCtrl;
  int _heroIndex = 0;
  late AnimationController _entranceCtrl;
  late Animation<double> _entrance;

  @override
  void initState() {
    super.initState();
    _heroCtrl = PageController();
    _entranceCtrl = AnimationController(duration: DS.durHero, vsync: this);
    _entrance = CurvedAnimation(parent: _entranceCtrl, curve: DS.cHero);
    _load();
  }

  @override
  void dispose() { _heroCtrl.dispose(); _entranceCtrl.dispose(); super.dispose(); }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final api = GetIt.instance<ApiClient>();
      // 尝试从服务器获取推荐
      final res = await api.get('/home/recommend');
      final data = res.data;
      if (data is Map) {
        _heroSlides = (data['banners'] as List?) ?? [];
        _continueReading = (data['continueReading'] as List?) ?? [];
        _popular = (data['popular'] as List?) ?? [];
        _latest = (data['latest'] as List?) ?? [];
      }
    } catch (_) {
      // 服务器没数据，用空列表
      _heroSlides = [];
    }
    if (!mounted) return;
    setState(() => _loading = false);
    _entranceCtrl.forward();
    // 自动轮播
    _startAutoSlide();
  }

  void _startAutoSlide() {
    Future.delayed(const Duration(seconds: 5), () {
      if (!mounted || _heroSlides.isEmpty) return;
      _heroIndex = (_heroIndex + 1) % _heroSlides.length;
      _heroCtrl.animateToPage(_heroIndex, duration: DS.durEmphasis, curve: DS.cEmphasis);
      _startAutoSlide();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: DS.bg,
      body: _loading
        ? const Center(child: CircularProgressIndicator(color: DS.accent, strokeWidth: 2))
        : CustomScrollView(
            physics: const BouncingScrollPhysics(),
            slivers: [
              // ── 沉浸式 Hero ──
              SliverToBoxAdapter(
                child: FadeSlideIn(
                  child: _buildHero(),
                ),
              ),

              const SizedBox(height: DS.sp24).asSliver(),

              // ── 继续阅读 ──
              if (_continueReading.isNotEmpty) ...[
                SectionHeader(title: '继续阅读', subtitle: '${_continueReading.length}本'),
                SliverToBoxAdapter(
                  child: SizedBox(
                    height: 110,
                    child: ListView.separated(
                      padding: const EdgeInsets.symmetric(horizontal: DS.sp16),
                      scrollDirection: Axis.horizontal,
                      physics: const BouncingScrollPhysics(),
                      itemCount: _continueReading.length,
                      separatorBuilder: (_, __) => const SizedBox(width: DS.sp12),
                      itemBuilder: (ctx, i) {
                        final item = _continueReading[i] as Map;
                        return SizedBox(
                          width: MediaQuery.of(context).size.width - 64,
                          child: ContinueReadingCard(
                            cover: (item['cover'] ?? '').toString(),
                            title: (item['title'] ?? '').toString(),
                            chapter: (item['chapter'] ?? '继续阅读').toString(),
                            progress: ((item['progress'] ?? 0) as num).toDouble(),
                            onTap: () => GoRouter.of(context).push('/comic/${item['id']}'),
                          ),
                        );
                      },
                    ),
                  ),
                ),
                const SizedBox(height: DS.sp16).asSliver(),
              ],

              // ── 热门 ──
              if (_popular.isNotEmpty) ...[
                SectionHeader(title: '热门作品', onMore: () => GoRouter.of(context).push('/discover')),
                _horizontalComicList(_popular),
              ],

              // ── 最新 ──
              if (_latest.isNotEmpty) ...[
                SectionHeader(title: '最近更新', onMore: () => GoRouter.of(context).push('/discover')),
                _horizontalComicList(_latest),
              ],

              // ── 空状态引导 ──
              if (_heroSlides.isEmpty && _popular.isEmpty && _latest.isEmpty)
                SliverFillRemaining(child: EmptyState(
                  icon: Icons.explore_rounded,
                  title: '探索漫画世界',
                  subtitle: '去发现页安装漫画源，开始阅读',
                  actionLabel: '去发现',
                  onAction: () => GoRouter.of(context).go('/discover'),
                )),

              const SliverToBoxAdapter(child: SizedBox(height: 120)),
            ],
          ),
    );
  }

  /// 全屏沉浸式 Hero
  Widget _buildHero() {
    if (_heroSlides.isEmpty) {
      // 没有推荐数据时，显示一个简约 Hero 引导
      return Container(
        height: MediaQuery.of(context).size.height * 0.5,
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter, end: Alignment.bottomCenter,
            colors: [DS.surface2, DS.bg],
          ),
        ),
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.auto_stories_rounded, size: 56, color: DS.textTertiary),
              const SizedBox(height: DS.sp16),
              Text('漫界', style: DS.display.copyWith(color: DS.textPrimary)),
              const SizedBox(height: 8),
              Text('海量漫画，一触即达', style: DS.bodySec),
            ],
          ),
        ),
      );
    }

    return SizedBox(
      height: MediaQuery.of(context).size.height * 0.62,
      child: Stack(
        children: [
          // ── 背景：PageView 全屏封面 ──
          PageView.builder(
            controller: _heroCtrl,
            itemCount: _heroSlides.length,
            onPageChanged: (i) { HapticFeedback.selectionClick(); setState(() => _heroIndex = i); },
            itemBuilder: (ctx, i) {
              final slide = _heroSlides[i] as Map;
              final cover = (slide['cover'] ?? '').toString();
              return Stack(
                fit: StackFit.expand,
                children: [
                  // 封面图
                  if (cover.isNotEmpty)
                    CachedNetworkImage(imageUrl: cover, fit: BoxFit.cover,
                      errorWidget: (_, __, ___) => Container(color: DS.surface2))
                  else
                    Container(color: DS.surface2),
                  // 模糊层（模拟动态模糊）
                  BackdropFilter(
                    filter: ImageFilter.blur(sigmaX: 0, sigmaY: 0),
                    child: Container(color: Colors.black.withValues(alpha: 0.3)),
                  ),
                  // 渐变遮罩（从透明到背景色）
                  Container(decoration: const BoxDecoration(gradient: DS.heroScrim)),
                  // ── 内容层 ──
                  Padding(
                    padding: const EdgeInsets.fromLTRB(DS.sp20, 0, DS.sp20, 60),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.end,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        // 标签
                        if ((slide['tags'] as List?)?.isNotEmpty == true)
                          Padding(
                            padding: const EdgeInsets.only(bottom: 8),
                            child: Wrap(spacing: 6, children: [
                              for (final tag in (slide['tags'] as List).take(3))
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                  decoration: BoxDecoration(color: DS.glassFillStrong, borderRadius: BorderRadius.circular(DS.rSm)),
                                  child: Text(tag.toString(), style: const TextStyle(fontSize: 11, color: DS.textSecondary)),
                                ),
                            ]),
                          ),
                        // 标题
                        Text((slide['title'] ?? '').toString(),
                          style: DS.display, maxLines: 2, overflow: TextOverflow.ellipsis),
                        if ((slide['description'] ?? '').toString().isNotEmpty) ...[
                          const SizedBox(height: 8),
                          Text((slide['description'] ?? '').toString(),
                            style: DS.bodySec, maxLines: 2, overflow: TextOverflow.ellipsis),
                        ],
                        const SizedBox(height: DS.sp16),
                        // 按钮
                        Row(children: [
                          SpringButton(
                            onPressed: () => GoRouter.of(context).push('/comic/${slide['id']}'),
                            child: const Row(mainAxisSize: MainAxisSize.min, children: [
                              Icon(Icons.play_arrow_rounded, size: 18, color: Colors.white),
                              SizedBox(width: 4),
                              Text('开始阅读'),
                            ]),
                          ),
                          const SizedBox(width: 12),
                          GestureDetector(
                            onTap: () {},
                            child: Glass(
                              radius: DS.rMd,
                              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                              child: const Icon(Icons.bookmark_outline_rounded, size: 18, color: DS.textPrimary),
                            ),
                          ),
                        ]),
                      ],
                    ),
                  ),
                ],
              );
            },
          ),
          // ── 指示器 ──
          Positioned(
            bottom: 20, left: 0, right: 0,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: List.generate(
                _heroSlides.length,
                (i) => AnimatedContainer(
                  duration: DS.durStd,
                  curve: DS.cStd,
                  margin: const EdgeInsets.symmetric(horizontal: 3),
                  width: i == _heroIndex ? 20 : 6,
                  height: 6,
                  decoration: BoxDecoration(
                    color: i == _heroIndex ? DS.accent : DS.textDisabled,
                    borderRadius: BorderRadius.circular(3),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// 横滑漫画列表
  Widget _horizontalComicList(List<dynamic> items) {
    return SliverToBoxAdapter(
      child: SizedBox(
        height: 240,
        child: ListView.separated(
          padding: const EdgeInsets.symmetric(horizontal: DS.sp16),
          scrollDirection: Axis.horizontal,
          physics: const BouncingScrollPhysics(),
          itemCount: items.length,
          separatorBuilder: (_, __) => const SizedBox(width: DS.sp12),
          itemBuilder: (ctx, i) {
            final item = items[i] as Map;
            return ComicCard(
              cover: (item['cover'] ?? '').toString(),
              title: (item['title'] ?? '').toString(),
              subtitle: (item['author'] ?? '').toString(),
              badge: i == 0 ? 'HOT' : null,
              onTap: () => GoRouter.of(context).push('/comic/${item['id']}'),
            );
          },
        ),
      ),
    );
  }
}

// Extension for SizedBox in Slivers
extension on SizedBox {
  Widget asSliver() => SliverToBoxAdapter(child: this);
}