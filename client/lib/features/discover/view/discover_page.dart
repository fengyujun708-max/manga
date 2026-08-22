import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import '../../../app/ds.dart';
import '../../../plugins/manga_source.dart';

/// 发现页 — 已安装漫画源 + 引导
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
      backgroundColor: DS.bg,
      body: CustomScrollView(
        physics: const BouncingScrollPhysics(),
        slivers: [
          SliverAppBar(
            floating: true, snap: true,
            backgroundColor: Colors.transparent, elevation: 0,
            title: Row(children: [
              const Text('发现', style: DS.headline),
              const Spacer(),
              _iconBtn(Icons.search_rounded, () => GoRouter.of(context).push('/search')),
              const SizedBox(width: 8),
              _iconBtn(Icons.add_rounded, () async {
                await GoRouter.of(context).push('/source-manager');
                _load();
              }),
            ]),
          ),

          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(DS.sp16, DS.sp4, DS.sp16, DS.sp12),
              child: Text('已安装漫画源', style: DS.title),
            ),
          ),

          if (_loading)
            SliverPadding(
              padding: const EdgeInsets.all(DS.sp16),
              sliver: SliverGrid(
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 2, childAspectRatio: 1.05, crossAxisSpacing: DS.sp12, mainAxisSpacing: DS.sp12),
                delegate: SliverChildBuilderDelegate((_, __) => const Shimmer(width: double.infinity, height: 180), childCount: 4),
              ),
            )
          else if (_sources.isEmpty)
            SliverFillRemaining(child: EmptyState(
              icon: Icons.extension_off_rounded, title: '还没有安装漫画源',
              subtitle: '安装后即可浏览海量漫画',
              actionLabel: '去源市场', onAction: () async { await GoRouter.of(context).push('/source-manager'); _load(); },
            ))
          else
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(DS.sp16, 0, DS.sp16, 120),
              sliver: SliverGrid(
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 2, childAspectRatio: 1.05, crossAxisSpacing: DS.sp12, mainAxisSpacing: DS.sp12),
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
      child: Container(width: 38, height: 38, decoration: BoxDecoration(color: DS.glassFill, borderRadius: BorderRadius.circular(12)), child: Icon(icon, size: 20, color: DS.textPrimary)),
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
    _ctrl = AnimationController(duration: DS.durMicro, vsync: this);
    _scale = Tween<double>(begin: 1, end: 0.96).animate(CurvedAnimation(parent: _ctrl, curve: DS.cStd));
  }
  @override
  void dispose() { _ctrl.dispose(); super.dispose(); }

  @override
  Widget build(BuildContext ctx) {
    final m = widget.manifest;
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
              color: DS.surface1,
              borderRadius: BorderRadius.circular(DS.rLg),
              border: Border.all(color: DS.glassBorder, width: 0.5),
            ),
            child: Stack(children: [
              Positioned(right: -20, top: -20, child: Container(width: 72, height: 72, decoration: BoxDecoration(shape: BoxShape.circle, color: DS.accent.withValues(alpha: 0.04)))),
              Padding(
                padding: const EdgeInsets.all(DS.sp16),
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Row(children: [
                    Container(
                      width: 44, height: 44,
                      decoration: BoxDecoration(color: DS.surface2, borderRadius: BorderRadius.circular(12)),
                      child: Center(child: Text(m.name.isNotEmpty ? m.name.characters.first : '?', style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800, color: DS.textPrimary))),
                    ),
                    const Spacer(),
                    _TypeBadge(meta: m.metadata),
                  ]),
                  const Spacer(),
                  Text(m.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: DS.textPrimary)),
                  const SizedBox(height: 4),
                  Text(m.description.isNotEmpty ? m.description : '点击浏览漫画', maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12, color: DS.textTertiary)),
                  const SizedBox(height: 10),
                  Row(children: [
                    Icon(Icons.menu_book_rounded, size: 14, color: DS.textTertiary),
                    const SizedBox(width: 4),
                    Text('v${m.version}', style: const TextStyle(fontSize: 11, color: DS.textTertiary)),
                    const Spacer(),
                    Icon(Icons.arrow_forward_ios_rounded, size: 12, color: DS.accent),
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

class _TypeBadge extends StatelessWidget {
  final Map<String, dynamic> meta;
  const _TypeBadge({required this.meta});
  @override
  Widget build(BuildContext context) {
    final type = meta['type']?.toString() ?? 'manga';
    final isAdult = type == 'hentai';
    final locale = meta['locale']?.toString() ?? '';
    final label = isAdult ? '成人' : (locale == 'zh' ? '中文' : (locale == 'ja' ? '日文' : '海外'));
    final color = isAdult ? DS.accent : DS.textTertiary;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(color: color.withValues(alpha: 0.1), borderRadius: BorderRadius.circular(DS.rSm)),
      child: Text(label, style: TextStyle(fontSize: 9, color: color, fontWeight: FontWeight.w600)),
    );
  }
}