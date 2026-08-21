import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import '../../../app/theme/theme.dart';
import '../../../app/widgets/comic_widgets.dart';
import '../../../plugins/manga_source.dart';

/// 发现页 — 已安装漫画源
class DiscoverPage extends StatefulWidget {
  const DiscoverPage({super.key});
  @override
  State<DiscoverPage> createState() => _DiscoverPageState();
}

class _DiscoverPageState extends State<DiscoverPage> {
  List<SourceManifest> _sources = [];
  bool _loading = true;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final prefs = await SharedPreferences.getInstance();
      final json = prefs.getString('installed_sources') ?? '[]';
      _sources = (jsonDecode(json) as List).map((e) => SourceManifest.fromJson(e as Map<String, dynamic>)).toList();
    } catch (_) { _sources = []; }
    setState(() => _loading = false);
  }

  void _enterSource(SourceManifest src) {
    HapticFeedback.lightImpact();
    GoRouter.of(context).push('/discover/source/${src.id}?name=${Uri.encodeComponent(src.name)}');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: CustomScrollView(
        physics: const BouncingScrollPhysics(),
        slivers: [
          // 头部
          SliverAppBar(
            floating: true, snap: true,
            backgroundColor: Colors.transparent,
            elevation: 0,
            title: Row(children: [
              ShaderMask(
                shaderCallback: (b) => AppTheme.primaryGradient.createShader(b),
                child: const Text('发现', style: TextStyle(fontWeight: FontWeight.w800, fontSize: 22, color: Colors.white)),
              ),
              const Spacer(),
              _iconBtn(Icons.search_rounded, () => GoRouter.of(context).push('/search')),
              const SizedBox(width: 8),
              _iconBtn(Icons.add_rounded, () async {
                await GoRouter.of(context).push('/source-manager?tab=market');
                _load();
              }),
            ]),
          ),

          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 4, 16, 12),
              child: Text('已安装漫画源', style: Theme.of(context).textTheme.headlineMedium),
            ),
          ),

          if (_loading)
            SliverPadding(
              padding: const EdgeInsets.all(16),
              sliver: SliverGrid(
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 2, childAspectRatio: 1.1, crossAxisSpacing: 14, mainAxisSpacing: 14),
                delegate: SliverChildBuilderDelegate((_, __) => ShimmerBox(width: double.infinity, height: 180, radius: BorderRadius.circular(AppTheme.radiusLg)), childCount: 4),
              ),
            )
          else if (_sources.isEmpty)
            SliverFillRemaining(child: EmptyState(
              icon: Icons.extension_off_rounded,
              title: '还没有安装漫画源',
              subtitle: '安装漫画源后即可浏览海量漫画',
              actionLabel: '去源市场',
              onAction: () async {
                await GoRouter.of(context).push('/source-manager?tab=market');
                _load();
              },
            ))
          else
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 100),
              sliver: SliverGrid(
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 2, childAspectRatio: 1.05, crossAxisSpacing: 14, mainAxisSpacing: 14),
                delegate: SliverChildBuilderDelegate((_, i) => _SourceCard(manifest: _sources[i], onTap: () => _enterSource(_sources[i]), index: i), childCount: _sources.length),
              ),
            ),
        ],
      ),
    );
  }

  Widget _iconBtn(IconData icon, VoidCallback tap) {
    return GestureDetector(
      onTap: () { HapticFeedback.lightImpact(); tap(); },
      child: Container(
        width: 38, height: 38,
        decoration: BoxDecoration(color: AppTheme.glassFillLight, borderRadius: BorderRadius.circular(12)),
        child: Icon(icon, size: 20, color: AppTheme.textPrimary),
      ),
    );
  }
}

class _SourceCard extends StatefulWidget {
  final SourceManifest manifest;
  final VoidCallback onTap;
  final int index;
  const _SourceCard({required this.manifest, required this.onTap, required this.index});
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
    final m = widget.manifest;
    final meta = m.metadata;
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
                colors: [AppTheme.primary.withValues(alpha: 0.15), AppTheme.surface],
              ),
              border: Border.all(color: AppTheme.glassBorder, width: 0.5),
              boxShadow: AppTheme.softShadow,
            ),
            child: Stack(children: [
              // 装饰圆
              Positioned(right: -20, top: -20, child: Container(width: 72, height: 72, decoration: BoxDecoration(shape: BoxShape.circle, color: AppTheme.primary.withValues(alpha: 0.06)))),
              // 内容
              Padding(
                padding: const EdgeInsets.all(16),
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Row(children: [
                    Container(
                      width: 44, height: 44,
                      decoration: BoxDecoration(
                        gradient: AppTheme.primaryGradient,
                        borderRadius: BorderRadius.circular(12),
                        boxShadow: [BoxShadow(color: AppTheme.primary.withValues(alpha: 0.3), blurRadius: 12)],
                      ),
                      child: Center(child: Text(m.name.isNotEmpty ? m.name.characters.first : '?', style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800, color: Colors.white))),
                    ),
                    const Spacer(),
                    SourceBadge.fromMeta(meta),
                  ]),
                  const Spacer(),
                  Text(m.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: AppTheme.textPrimary)),
                  const SizedBox(height: 4),
                  Text(m.description.isNotEmpty ? m.description : '点击浏览漫画', maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12, color: AppTheme.textTertiary)),
                  const SizedBox(height: 10),
                  Row(children: [
                    Icon(Icons.menu_book_rounded, size: 14, color: AppTheme.textTertiary),
                    const SizedBox(width: 4),
                    Text('v${m.version}', style: const TextStyle(fontSize: 11, color: AppTheme.textTertiary)),
                    const Spacer(),
                    Icon(Icons.arrow_forward_ios_rounded, size: 12, color: AppTheme.primary),
                  ]),
                ]),
              ),
            ]),
          ),
        ),
      ),
    );
  }
}