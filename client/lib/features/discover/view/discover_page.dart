import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:get_it/get_it.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import 'dart:ui';
import '../../../app/theme/theme.dart';
import '../../../core/network/api_client.dart';
import '../../../plugins/manga_source.dart';

/// 发现页 — 展示用户已安装的漫画源
/// 未安装任何源时引导用户去源市场下载
class DiscoverPage extends StatefulWidget {
  const DiscoverPage({super.key});

  @override
  State<DiscoverPage> createState() => _DiscoverPageState();
}

class _DiscoverPageState extends State<DiscoverPage> {
  List<SourceManifest> _installedSources = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _loadInstalledSources();
  }

  Future<void> _loadInstalledSources() async {
    setState(() => _loading = true);
    try {
      final prefs = await SharedPreferences.getInstance();
      final json = prefs.getString('installed_sources') ?? '[]';
      final List<dynamic> list = jsonDecode(json);
      _installedSources = list
          .map((e) => SourceManifest.fromJson(e as Map<String, dynamic>))
          .toList();
    } catch (_) {
      _installedSources = [];
    }
    setState(() => _loading = false);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: CustomScrollView(
        physics: const BouncingScrollPhysics(),
        slivers: [
          SliverAppBar(
            floating: true,
            snap: true,
            backgroundColor: Colors.transparent,
            elevation: 0,
            title: Row(children: [
              ShaderMask(
                shaderCallback: (b) => AppTheme.primaryGradient.createShader(b),
                child: const Text('发现',
                    style: TextStyle(fontWeight: FontWeight.w800, fontSize: 22, color: Colors.white)),
              ),
              const Spacer(),
              _iconBtn(Icons.search_rounded, () => GoRouter.of(context).push('/search')),
              const SizedBox(width: 8),
              _iconBtn(Icons.extension_rounded, () async {
                await GoRouter.of(context).push('/source-manager');
                _loadInstalledSources(); // 返回后刷新
              }),
            ]),
          ),
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
              child: Text('已安装漫画源',
                  style: Theme.of(context)
                      .textTheme
                      .headlineMedium
                      ?.copyWith(fontWeight: FontWeight.w800)),
            ),
          ),
          if (_loading)
            SliverPadding(
              padding: const EdgeInsets.all(16),
              sliver: SliverGrid(
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 2,
                  childAspectRatio: 1.05,
                  crossAxisSpacing: 14,
                  mainAxisSpacing: 14,
                ),
                delegate: SliverChildBuilderDelegate(
                  (_, i) => ShimmerBox(
                      width: double.infinity,
                      height: 180,
                      radius: BorderRadius.circular(AppTheme.radiusLg)),
                  childCount: 4,
                ),
              ),
            )
          else if (_installedSources.isEmpty)
            SliverFillRemaining(
              child: _buildEmptyState(),
            )
          else
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 100),
              sliver: SliverGrid(
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 2,
                  childAspectRatio: 1.05,
                  crossAxisSpacing: 14,
                  mainAxisSpacing: 14,
                ),
                delegate: SliverChildBuilderDelegate(
                  (_, i) => _SourceCard(
                    manifest: _installedSources[i],
                    onTap: () => _enterSource(_installedSources[i]),
                    index: i,
                  ),
                  childCount: _installedSources.length,
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.extension_off_rounded, size: 64, color: AppTheme.textTertiary),
          const SizedBox(height: 16),
          Text('还没有安装任何漫画源', style: TextStyle(color: AppTheme.textSecondary, fontSize: 16)),
          const SizedBox(height: 8),
          Text('安装漫画源后即可浏览海量漫画', style: TextStyle(color: AppTheme.textTertiary, fontSize: 13)),
          const SizedBox(height: 24),
          ElevatedButton.icon(
            onPressed: () async {
              await GoRouter.of(context).push('/source-market');
              _loadInstalledSources();
            },
            icon: const Icon(Icons.shop_rounded),
            label: const Text('去源市场'),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppTheme.primary,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
            ),
          ),
        ],
      ),
    );
  }

  void _enterSource(SourceManifest source) {
    HapticFeedback.lightImpact();
    GoRouter.of(context).push('/discover/source/${source.id}');
  }

  Widget _iconBtn(IconData icon, VoidCallback tap) {
    return GestureDetector(
      onTap: tap,
      child: Container(
        width: 40,
        height: 40,
        decoration: BoxDecoration(
          color: AppTheme.surfaceLight.withValues(alpha: 0.5),
          borderRadius: BorderRadius.circular(14),
        ),
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
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext ctx) {
    final m = widget.manifest;
    final color = Color(0xFF6366F1);
    return GestureDetector(
      onTapDown: (_) {
        _ctrl.forward();
        HapticFeedback.selectionClick();
      },
      onTapUp: (_) {
        _ctrl.reverse();
        widget.onTap();
      },
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
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [color.withValues(alpha: 0.25), AppTheme.surface],
              ),
              border: Border.all(color: color.withValues(alpha: 0.2), width: 0.5),
              boxShadow: AppTheme.cardShadow,
            ),
            child: Stack(children: [
              Positioned(
                right: -20,
                top: -20,
                child: Container(
                  width: 80,
                  height: 80,
                  decoration: BoxDecoration(shape: BoxShape.circle, color: color.withValues(alpha: 0.08)),
                ),
              ),
              Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      width: 44,
                      height: 44,
                      decoration: BoxDecoration(
                        color: color.withValues(alpha: 0.2),
                        borderRadius: BorderRadius.circular(14),
                      ),
                      child: Center(child: Text(m.icon, style: const TextStyle(fontSize: 22))),
                    ),
                    const Spacer(),
                    Text(m.name,
                        style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: AppTheme.textPrimary),
                        maxLines: 1, overflow: TextOverflow.ellipsis),
                    const SizedBox(height: 4),
                    Text(m.description,
                        style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                        maxLines: 1, overflow: TextOverflow.ellipsis),
                    const SizedBox(height: 8),
                    Row(children: [
                      Icon(Icons.menu_book_rounded, size: 14, color: color.withValues(alpha: 0.7)),
                      const SizedBox(width: 4),
                      Text('v${m.version}',
                          style: TextStyle(fontSize: 11, color: color.withValues(alpha: 0.8), fontWeight: FontWeight.w600)),
                    ]),
                  ],
                ),
              ),
              Positioned(
                right: 12,
                bottom: 12,
                child: Container(
                  width: 28,
                  height: 28,
                  decoration: BoxDecoration(
                    color: color.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Icon(Icons.arrow_forward_ios_rounded, size: 12, color: color),
                ),
              ),
            ]),
          ),
        ),
      ),
    );
  }
}
