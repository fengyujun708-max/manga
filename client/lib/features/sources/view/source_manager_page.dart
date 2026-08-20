import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../../app/theme/theme.dart';
import '../../../plugins/manga_source.dart' hide SourceManager;
import '../bloc/source_bloc.dart';
import '../bloc/source_event.dart';
import '../bloc/source_state.dart';

/// 漫画源管理页 — 只能从服务器自动下载源，用户不能手动添加
class SourceManagerPage extends StatefulWidget {
  const SourceManagerPage({super.key});

  @override
  State<SourceManagerPage> createState() => _SourceManagerPageState();
}

class _SourceManagerPageState extends State<SourceManagerPage>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    context.read<SourceBloc>().add(SourceLoadRequested());
    context.read<SourceBloc>().add(SourceMarketLoadRequested());
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: CustomScrollView(
        physics: const BouncingScrollPhysics(),
        slivers: [
          SliverAppBar(
            pinned: true, floating: true,
            backgroundColor: AppTheme.surface.withValues(alpha: 0.8),
            title: const Text('漫画源', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 18)),
            bottom: PreferredSize(
              preferredSize: const Size.fromHeight(48),
              child: Container(
                color: AppTheme.background,
                child: TabBar(
                  controller: _tabController,
                  labelColor: AppTheme.primary,
                  unselectedLabelColor: AppTheme.textSecondary,
                  indicatorColor: AppTheme.primary,
                  indicatorSize: TabBarIndicatorSize.label,
                  tabs: const [Tab(text: '已安装'), Tab(text: '源市场')],
                ),
              ),
            ),
            actions: [
              IconButton(
                icon: const Icon(Icons.sync_rounded, color: AppTheme.textPrimary),
                onPressed: () => context.read<SourceBloc>().add(SourceRefreshRequested()),
                tooltip: '检查更新',
              ),
            ],
          ),
          SliverFillRemaining(
            child: TabBarView(
              controller: _tabController,
              children: [
                _InstalledSourcesTab(),
                _SourceMarketTab(),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _InstalledSourcesTab extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return BlocBuilder<SourceBloc, SourceState>(
      builder: (context, state) {
        if (state is SourceLoading) {
          return _buildSkeleton();
        }
        if (state is SourceError) {
          return _buildEmpty('加载失败: ${state.message}');
        }
        if (state is SourceLoaded) {
          final sources = state.sources;
          if (sources.isEmpty) {
            return _buildEmpty('暂无已安装源，去源市场下载');
          }
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: sources.length,
            itemBuilder: (_, i) => _SourceCard(source: sources[i]),
          );
        }
        return const SizedBox();
      },
    );
  }

  Widget _buildSkeleton() {
    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: 4,
      itemBuilder: (_, i) => Padding(
        padding: const EdgeInsets.only(bottom: 12),
        child: ShimmerBox(
          width: double.infinity,
          height: 72,
          radius: BorderRadius.circular(AppTheme.radiusMd),
        ),
      ),
    );
  }

  Widget _buildEmpty(String msg) {
    return Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
      Icon(Icons.source_outlined, size: 48, color: AppTheme.textTertiary),
      const SizedBox(height: 12),
      Text(msg, style: TextStyle(color: AppTheme.textSecondary, fontSize: 14)),
    ]));
  }
}

class _SourceCard extends StatelessWidget {
  final MangaSource source;
  const _SourceCard({required this.source});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(AppTheme.radiusLg),
        border: Border.all(color: AppTheme.glassBorder, width: 0.5),
      ),
      child: Row(children: [
        Container(
          width: 48, height: 48,
          decoration: BoxDecoration(
            color: source.enabled
              ? AppTheme.primary.withValues(alpha: 0.15)
              : AppTheme.surfaceLight,
            borderRadius: BorderRadius.circular(14),
          ),
          child: Center(child: Text(source.manifest.icon.isNotEmpty ? source.manifest.icon : '📚', style: const TextStyle(fontSize: 22))),
        ),
        const SizedBox(width: 12),
        Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Text(source.manifest.name, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15, color: AppTheme.textPrimary)),
            const SizedBox(width: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
              decoration: BoxDecoration(
                color: source.enabled ? AppTheme.success.withValues(alpha: 0.15) : AppTheme.textTertiary.withValues(alpha: 0.15),
                borderRadius: BorderRadius.circular(6),
              ),
              child: Text(source.enabled ? '已启用' : '已禁用',
                style: TextStyle(fontSize: 10, color: source.enabled ? AppTheme.success : AppTheme.textTertiary, fontWeight: FontWeight.w600)),
            ),
            const SizedBox(width: 6),
            Text('v${source.manifest.version}', style: const TextStyle(color: AppTheme.textTertiary, fontSize: 11)),
          ]),
          const SizedBox(height: 4),
          Text(source.manifest.description, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12), maxLines: 1, overflow: TextOverflow.ellipsis),
        ]),
        Switch(
          value: source.enabled,
          onChanged: (v) => context.read<SourceBloc>().add(SourceToggleEnabled(source.id, v)),
          activeColor: AppTheme.primary,
        ),
      ]),
    );
  }
}

class _SourceMarketTab extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return BlocBuilder<SourceBloc, SourceState>(
      builder: (context, state) {
        if (state is SourceMarketLoading) {
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: 4,
            itemBuilder: (_, i) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: ShimmerBox(
                width: double.infinity,
                height: 88,
                radius: BorderRadius.circular(AppTheme.radiusLg),
              ),
            ),
          );
        }
        if (state is SourceMarketLoaded) {
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: state.sources.length,
            itemBuilder: (_, i) => _MarketCard(source: state.sources[i]),
          );
        }
        return Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
          Icon(Icons.cloud_download_rounded, size: 48, color: AppTheme.textTertiary),
          const SizedBox(height: 12),
          Text('从服务器加载源列表', style: TextStyle(color: AppTheme.textSecondary, fontSize: 14)),
          const SizedBox(height: 16),
          SpringButton(
            width: 180, height: 44,
            onPressed: () => context.read<SourceBloc>().add(SourceMarketLoadRequested()),
            child: const Text('刷新', style: TextStyle(fontSize: 14, color: Colors.white)),
          ),
        ]));
      },
    );
  }
}

class _MarketCard extends StatefulWidget {
  final dynamic source;
  const _MarketCard({required this.source});

  @override
  State<_MarketCard> createState() => _MarketCardState();
}

class _MarketCardState extends State<_MarketCard> with TickerProviderStateMixin {
  bool _installing = false;

  @override
  Widget build(BuildContext context) {
    final s = widget.source;
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(AppTheme.radiusLg),
        border: Border.all(color: AppTheme.glassBorder, width: 0.5),
      ),
      child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Container(
          width: 52, height: 52,
          decoration: BoxDecoration(
            color: AppTheme.primary.withValues(alpha: 0.12),
            borderRadius: BorderRadius.circular(14),
          ),
          child: Center(child: Text(s.icon?.isNotEmpty == true ? s.icon : '📚', style: const TextStyle(fontSize: 24))),
        ),
        const SizedBox(width: 12),
        Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Text(s.name ?? '未知', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15, color: AppTheme.textPrimary)),
            const SizedBox(width: 6),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
              decoration: BoxDecoration(color: AppTheme.primary.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(6)),
              child: Text('v${s.version ?? '1.0'}', style: const TextStyle(color: AppTheme.primary, fontSize: 10)),
            ),
          ]),
          const SizedBox(height: 4),
          Text(s.description ?? '', style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12), maxLines: 2, overflow: TextOverflow.ellipsis),
          const SizedBox(height: 8),
          Row(children: [
            Icon(Icons.star_rounded, size: 14, color: AppTheme.accent),
            const SizedBox(width: 2),
            Text('${s.rating ?? 0}', style: const TextStyle(color: AppTheme.accent, fontSize: 12)),
            const SizedBox(width: 12),
            Icon(Icons.download_rounded, size: 14, color: AppTheme.textTertiary),
            const SizedBox(width: 2),
            Text('${s.downloads ?? 0} 次下载', style: const TextStyle(color: AppTheme.textTertiary, fontSize: 12)),
          ]),
        ]),
        const SizedBox(width: 8),
        SizedBox(
          width: 72, height: 36,
          child: SpringButton(
            width: 72, height: 36,
            onPressed: _installing ? null : () => _install(context, s),
            child: _installing
              ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
              : const Text('下载', style: TextStyle(fontSize: 12, color: Colors.white, fontWeight: FontWeight.w600)),
          ),
        ),
      ]),
    );
  }

  void _install(BuildContext context, dynamic s) async {
    setState(() => _installing = true);
    // 从服务器下载并安装源
    try {
      context.read<SourceBloc>().add(SourceInstallFromServerRequested(s.id));
    } catch (_) {}
    await Future.delayed(const Duration(seconds: 2));
    if (mounted) setState(() => _installing = false);
  }
}
