import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:get_it/get_it.dart';
import 'dart:ui';
import '../../../app/theme/theme.dart';
import '../../../core/network/api_client.dart';

/// 发现页 — 漫画源浏览模式（类似 Breeze/Venera）
/// 源列表由服务器自动下发，用户不可手动添加
/// 展示所有可用漫画源 → 点击进入源详情（分类+漫画列表）
class DiscoverPage extends StatefulWidget {
  const DiscoverPage({super.key});

  @override
  State<DiscoverPage> createState() => _DiscoverPageState();
}

class _DiscoverPageState extends State<DiscoverPage> {
  List<dynamic> _sources = [];
  bool _loading = true;
  String? _error;

  // 预置漫画源（服务器源注册表为空时展示）
  final _presetSources = [
    {'id': 'copy', 'name': '拷贝漫画', 'icon': '📖', 'desc': '海量正版漫画，更新速度快', 'color': 0xFF6366F1, 'count': '12000+'},
    {'id': 'baozi', 'name': '包子漫画', 'icon': '🥟', 'desc': '国产漫画聚集地，原创优质', 'color': 0xFFF59E0B, 'count': '8000+'},
    {'id': 'mangaup', 'name': 'MangaUp', 'icon': '🌐', 'desc': '日本漫画在线阅读', 'color': 0xFF22C55E, 'count': '5000+'},
    {'id': 'manhuagui', 'name': '漫画柜', 'icon': '📚', 'desc': '港台漫画资源丰富', 'color': 0xFFEF4444, 'count': '10000+'},
    {'id': 'dmzj', 'name': '动漫之家', 'icon': '🏠', 'desc': '老牌漫画站，分类齐全', 'color': 0xFF8B5CF6, 'count': '30000+'},
    {'id': 'manhuadb', 'name': '漫画DB', 'icon': '💾', 'desc': '高清漫画数据库', 'color': 0xFF06B6D4, 'count': '6000+'},
  ];

  @override
  void initState() {
    super.initState();
    _loadSources();
  }

  Future<void> _loadSources() async {
    setState(() { _loading = true; _error = null; });
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/sources');
      final data = res.data;
      if (data is Map && data['sources'] is List && (data['sources'] as List).isNotEmpty) {
        _sources = data['sources'];
      } else {
        _sources = _presetSources;
      }
    } catch (_) {
      _sources = _presetSources;
    }
    setState(() { _loading = false; });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: CustomScrollView(
        physics: const BouncingScrollPhysics(),
        slivers: [
          // 顶部
          SliverAppBar(
            floating: true, snap: true,
            backgroundColor: Colors.transparent, elevation: 0,
            title: Row(children: [
              ShaderMask(
                shaderCallback: (b) => AppTheme.primaryGradient.createShader(b),
                child: const Text('发现', style: TextStyle(fontWeight: FontWeight.w800, fontSize: 22, color: Colors.white)),
              ),
              const Spacer(),
              _iconBtn(Icons.search_rounded, () => GoRouter.of(context).push('/search')),
              const SizedBox(width: 8),
              _iconBtn(Icons.tune_rounded, () {}),
            ]),
          ),

          // 副标题
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
              child: Text('漫画源', style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w800)),
            ),
          ),

          // 漫画源网格
          if (_loading)
            SliverPadding(
              padding: const EdgeInsets.all(16),
              sliver: SliverGrid(
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 2, childAspectRatio: 1.1, crossAxisSpacing: 14, mainAxisSpacing: 14,
                ),
                delegate: SliverChildBuilderDelegate(
                  (_, i) => ShimmerBox(width: double.infinity, height: 180, radius: BorderRadius.circular(AppTheme.radiusLg)),
                  childCount: 6,
                ),
              ),
            )
          else
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 100),
              sliver: SliverGrid(
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 2, childAspectRatio: 1.05, crossAxisSpacing: 14, mainAxisSpacing: 14,
                ),
                delegate: SliverChildBuilderDelegate(
                  (_, i) {
                    final src = _sources[i];
                    return _SourceCard(
                      name: src['name'] ?? '未知',
                      icon: src['icon'] ?? '📚',
                      desc: src['desc'] ?? src['description'] ?? '',
                      color: Color(src['color'] ?? 0xFF6366F1),
                      count: src['count'] ?? '${src['downloads'] ?? 0}',
                      onTap: () => _enterSource(src),
                      index: i,
                    );
                  },
                  childCount: _sources.length,
                ),
              ),
            ),
        ],
      ),
    );
  }

  void _enterSource(dynamic src) {
    HapticFeedback.lightImpact();
    // 进入源详情页（分类+漫画列表）
    final id = src['id']?.toString() ?? src['name'];
    GoRouter.of(context).push('/discover/source/$id');
  }

  Widget _iconBtn(IconData icon, VoidCallback tap) {
    return GestureDetector(
      onTap: tap,
      child: Container(
        width: 40, height: 40,
        decoration: BoxDecoration(
          color: AppTheme.surfaceLight.withValues(alpha: 0.5),
          borderRadius: BorderRadius.circular(14),
        ),
        child: Icon(icon, size: 20, color: AppTheme.textPrimary),
      ),
    );
  }
}

/// 漫画源卡片 — 渐变背景 + 图标 + 名称 + 描述 + 书量
class _SourceCard extends StatefulWidget {
  final String name, icon, desc, count;
  final Color color;
  final VoidCallback onTap;
  final int index;

  const _SourceCard({
    required this.name, required this.icon, required this.desc,
    required this.color, required this.count, required this.onTap, required this.index,
  });

  @override
  State<_SourceCard> createState() => _SourceCardState();
}

class _SourceCardState extends State<_SourceCard> with SingleTickerProviderStateMixin {
  late AnimationController _ctrl;
  late Animation<double> _scale;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(duration: AppTheme.durFast, vsync: this);
    _scale = Tween<double>(begin: 1, end: 0.94).animate(CurvedAnimation(parent: _ctrl, curve: AppTheme.smoothOut));
  }

  @override
  void dispose() { _ctrl.dispose(); super.dispose(); }

  @override
  Widget build(BuildContext ctx) {
    return GestureDetector(
      onTapDown: (_) { _ctrl.forward(); HapticFeedback.selectionClick(); },
      onTapUp: (_) { _ctrl.reverse(); widget.onTap(); },
      onTapCancel: () => _ctrl.reverse(),
      child: FadeSlideIn(
        delay: Duration(milliseconds: widget.index * 60),
        child: AnimatedBuilder(
          animation: _scale,
          builder: (ctx, child) => Transform.scale(scale: _scale.value, child: child),
          child: Container(
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(AppTheme.radiusLg),
              gradient: LinearGradient(
                begin: Alignment.topLeft, end: Alignment.bottomRight,
                colors: [widget.color.withValues(alpha: 0.25), AppTheme.surface],
              ),
              border: Border.all(color: widget.color.withValues(alpha: 0.2), width: 0.5),
              boxShadow: AppTheme.cardShadow,
            ),
            child: Stack(children: [
              // 装饰光球
              Positioned(right: -20, top: -20, child: Container(
                width: 80, height: 80,
                decoration: BoxDecoration(shape: BoxShape.circle, color: widget.color.withValues(alpha: 0.08)),
              )),
              Padding(
                padding: const EdgeInsets.all(16),
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  // 图标
                  Container(
                    width: 44, height: 44,
                    decoration: BoxDecoration(
                      color: widget.color.withValues(alpha: 0.2),
                      borderRadius: BorderRadius.circular(14),
                    ),
                    child: Center(child: Text(widget.icon, style: const TextStyle(fontSize: 22))),
                  ),
                  const Spacer(),
                  // 名称
                  Text(widget.name, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: AppTheme.textPrimary), maxLines: 1, overflow: TextOverflow.ellipsis),
                  const SizedBox(height: 4),
                  // 描述
                  Text(widget.desc, style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary), maxLines: 1, overflow: TextOverflow.ellipsis),
                  const SizedBox(height: 8),
                  // 书量
                  Row(children: [
                    Icon(Icons.menu_book_rounded, size: 14, color: widget.color.withValues(alpha: 0.7)),
                    const SizedBox(width: 4),
                    Text('${widget.count} 部', style: TextStyle(fontSize: 11, color: widget.color.withValues(alpha: 0.8), fontWeight: FontWeight.w600)),
                  ]),
                ]),
              ),
              // 进入箭头
              Positioned(right: 12, bottom: 12,
                child: Container(
                  width: 28, height: 28,
                  decoration: BoxDecoration(
                    color: widget.color.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Icon(Icons.arrow_forward_ios_rounded, size: 12, color: widget.color),
                ),
              ),
            ]),
          ),
        ),
      ),
    );
  }
}
