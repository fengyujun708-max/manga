import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'package:get_it/get_it.dart';
import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../../../plugins/manga_source.dart';
import '../../../plugins/source_installer.dart';
import '../../../core/network/api_client.dart';
import '../bloc/source_bloc.dart';
import '../bloc/source_event.dart';
import '../bloc/source_state.dart';
import '../../../app/ds.dart';

class SourceManagerPage extends StatefulWidget {
  final int initialTab;
  const SourceManagerPage({super.key, this.initialTab = 0});
  @override
  State<SourceManagerPage> createState() => _SourceManagerPageState();
}

class _SourceManagerPageState extends State<SourceManagerPage>
    with TickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this, initialIndex: widget.initialTab);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        children: [
          SafeArea(
            bottom: false,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              child: Row(
                children: [
                  GestureDetector(
                    onTap: () => GoRouter.of(context).pop(),
                    child: Container(
                      width: 40, height: 40,
                      decoration: BoxDecoration(
                        color: DS.surface2.withValues(alpha: 0.5),
                        borderRadius: BorderRadius.circular(14),
                      ),
                      child: const Icon(Icons.arrow_back_ios_new_rounded, size: 18, color: DS.textPrimary),
                    ),
                  ),
                  const SizedBox(width: 12),
                  ShaderMask(
                    shaderCallback: (b) => const LinearGradient(colors: [DS.accent, Color(0xFFD93025)]).createShader(b),
                    child: const Text('漫画源管理',
                        style: TextStyle(fontWeight: FontWeight.w800, fontSize: 20, color: Colors.white)),
                  ),
                  const Spacer(),
                  IconButton(
                    icon: const Icon(Icons.sync_rounded, color: DS.textPrimary),
                    onPressed: () => context.read<SourceBloc>().add(SourceRefreshRequested()),
                  ),
                ],
              ),
            ),
          ),
          Container(
            margin: const EdgeInsets.symmetric(horizontal: 16),
            decoration: BoxDecoration(
              color: DS.surface2.withValues(alpha: 0.4),
              borderRadius: BorderRadius.circular(DS.rMd),
            ),
            child: TabBar(
              controller: _tabController,
              labelColor: DS.accent,
              unselectedLabelColor: DS.textSecondary,
              indicatorColor: DS.accent,
              indicatorSize: TabBarIndicatorSize.label,
              dividerColor: Colors.transparent,
              tabs: const [Tab(text: '已安装'), Tab(text: '源市场')],
            ),
          ),
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: [
                _InstalledTab(),
                _MarketTab(),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _InstalledTab extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return BlocBuilder<SourceBloc, SourceState>(
      builder: (context, state) {
        if (state is SourceLoading) {
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: 4,
            itemBuilder: (_, i) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: ShimmerBox(width: double.infinity, height: 72, radius: BorderRadius.circular(DS.rMd)),
            ),
          );
        }
        if (state is SourceError) {
          return _EmptyState(icon: Icons.error_outline, message: state.message);
        }
        if (state is SourceLoaded) {
          if (state.sources.isEmpty) {
            return _EmptyState(
              icon: Icons.source_outlined,
              message: '暂无已安装源',
              action: () => GoRouter.of(context).push('/source-market'),
              actionLabel: '去源市场',
            );
          }
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: state.sources.length,
            itemBuilder: (_, i) => _InstalledSourceCard(manifest: state.sources[i]),
          );
        }
        return const SizedBox.shrink();
      },
    );
  }
}

class _InstalledSourceCard extends StatelessWidget {
  final SourceManifest manifest;
  const _InstalledSourceCard({required this.manifest});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: DS.surface1,
        borderRadius: BorderRadius.circular(DS.rLg),
        border: Border.all(color: DS.glassBorder, width: 0.5),
      ),
      child: Row(
        children: [
          Container(
            width: 48, height: 48,
            decoration: BoxDecoration(
              color: DS.accent.withValues(alpha: 0.15),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Center(child: Text(manifest.icon, style: const TextStyle(fontSize: 22))),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(manifest.name,
                        style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15, color: DS.textPrimary)),
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                      decoration: BoxDecoration(
                        color: DS.accent.withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text('v${manifest.version}',
                          style: const TextStyle(color: DS.accent, fontSize: 10)),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(manifest.description,
                    style: const TextStyle(color: DS.textSecondary, fontSize: 12),
                    maxLines: 1, overflow: TextOverflow.ellipsis),
              ],
            ),
          ),
          Switch(
            value: true,
            onChanged: (v) {
              // Toggle enabled/disabled
              if (!v) {
                context.read<SourceBloc>().add(SourceDeleteRequested(manifest.id));
              }
            },
            activeColor: DS.accent,
          ),
        ],
      ),
    );
  }
}

class _MarketTab extends StatelessWidget {
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
              child: ShimmerBox(width: double.infinity, height: 88, radius: BorderRadius.circular(DS.rLg)),
            ),
          );
        }
        if (state is SourceMarketLoaded) {
          if (state.sources.isEmpty) {
            return _EmptyState(icon: Icons.cloud_download_rounded, message: '源市场暂时没有可用源');
          }
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: state.sources.length,
            itemBuilder: (_, i) => _MarketSourceCard(manifest: state.sources[i]),
          );
        }
        if (state is SourceMarketError) {
          return _EmptyState(
            icon: Icons.cloud_off_rounded,
            message: state.message,
            action: () => context.read<SourceBloc>().add(SourceMarketLoadRequested()),
            actionLabel: '重试',
          );
        }
        // 首次进入未加载
        return _EmptyState(
          icon: Icons.shop_outlined,
          message: '点击加载源市场',
          action: () => context.read<SourceBloc>().add(SourceMarketLoadRequested()),
          actionLabel: '加载',
        );
      },
    );
  }
}

class _MarketSourceCard extends StatefulWidget {
  final SourceManifest manifest;
  const _MarketSourceCard({required this.manifest});
  @override
  State<_MarketSourceCard> createState() => _MarketSourceCardState();
}

class _MarketSourceCardState extends State<_MarketSourceCard> {
  bool _installing = false;
  bool _installed = false;

  @override
  void initState() {
    super.initState();
    _checkInstalled();
  }

  Future<void> _checkInstalled() async {
    final prefs = await SharedPreferences.getInstance();
    final json = prefs.getString('installed_sources') ?? '[]';
    final list = jsonDecode(json) as List;
    final installed = list.any((e) {
      try {
        return SourceManifest.fromJson(e as Map<String, dynamic>).id == widget.manifest.id;
      } catch (_) { return false; }
    });
    if (mounted) setState(() => _installed = installed);
  }

  Future<void> _install() async {
    if (_installed || _installing) return;
    setState(() => _installing = true);
    try {
      final api = GetIt.instance<ApiClient>();
      // 从服务器获取源详情并存入本地
      final res = await api.get('/sources/${widget.manifest.id}');
      if (res.statusCode == 200 && res.data != null) {
        final manifest = SourceManifest.fromJson(res.data as Map<String, dynamic>);
        // 下载源 JS 到本地（Venera 本地执行模式）
        final sourceDir = await SourceInstaller.ensureSourceDir();
        var jsInstalled = false;
        if (sourceDir != null) {
          jsInstalled = await SourceInstaller.install(manifest, sourceDir);
        }
        // 写入 SharedPreferences（去重追加）
        final prefs = await SharedPreferences.getInstance();
        final json = prefs.getString('installed_sources') ?? '[]';
        final list = jsonDecode(json) as List;
        // 去重检查
        if (!list.any((e) {
          try { return SourceManifest.fromJson(e as Map<String, dynamic>).id == manifest.id; }
          catch (_) { return false; }
        })) {
          list.add(manifest.toJson());
          await prefs.setString('installed_sources', jsonEncode(list));
        }
        if (!mounted) return;
        setState(() { _installed = true; _installing = false; });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            behavior: SnackBarBehavior.floating,
            backgroundColor: DS.surface1,
            content: Row(children: [
              const Icon(Icons.check_circle_rounded, color: DS.success, size: 18),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                    jsInstalled ? '「${manifest.name}」安装成功' : '「${manifest.name}」已加入列表（JS 下载失败，走服务器代理）',
                    style: const TextStyle(color: DS.textPrimary, fontSize: 13)),
              ),
            ]),
            action: SnackBarAction(
              label: '去发现页',
              textColor: DS.accent,
              onPressed: () => GoRouter.of(context).go('/discover'),
            ),
          ),
        );
      }
    } catch (e) {
      if (!mounted) return;
      setState(() => _installing = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          behavior: SnackBarBehavior.floating,
          backgroundColor: DS.error,
          content: Text('安装失败: $e', style: const TextStyle(color: Colors.white, fontSize: 13)),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final m = widget.manifest;
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: DS.surface1,
        borderRadius: BorderRadius.circular(DS.rLg),
        border: Border.all(color: DS.glassBorder, width: 0.5),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 52, height: 52,
            decoration: BoxDecoration(
              color: DS.accent.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Center(child: Text(m.icon, style: const TextStyle(fontSize: 24))),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(m.name,
                        style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15, color: DS.textPrimary)),
                    const SizedBox(width: 6),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: DS.accent.withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text('v${m.version}',
                          style: const TextStyle(color: DS.accent, fontSize: 10)),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(m.description,
                    style: const TextStyle(color: DS.textSecondary, fontSize: 12),
                    maxLines: 2, overflow: TextOverflow.ellipsis),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Icon(Icons.star_rounded, size: 14, color: DS.accent),
                    const SizedBox(width: 2),
                    Text(m.rating.toStringAsFixed(1),
                        style: const TextStyle(color: DS.accent, fontSize: 12)),
                    const SizedBox(width: 12),
                    Icon(Icons.download_rounded, size: 14, color: DS.textTertiary),
                    const SizedBox(width: 2),
                    Text('${m.downloads} 次下载',
                        style: const TextStyle(color: DS.textTertiary, fontSize: 12)),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          SizedBox(
            width: 72, height: 36,
            child: _installed
                ? Container(
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: DS.success.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: const Text('已安装', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: DS.success)),
                  )
                : ElevatedButton(
                    onPressed: _installing ? null : _install,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: DS.accent,
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                      padding: EdgeInsets.zero,
                    ),
                    child: _installing
                        ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                        : const Text('安装', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
                  ),
          ),
        ],
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  final IconData icon;
  final String message;
  final VoidCallback? action;
  final String? actionLabel;

  const _EmptyState({required this.icon, required this.message, this.action, this.actionLabel});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, size: 48, color: DS.textTertiary),
          const SizedBox(height: 12),
          Text(message, style: TextStyle(color: DS.textSecondary, fontSize: 14)),
          if (action != null && actionLabel != null) ...[
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: action,
              style: ElevatedButton.styleFrom(
                backgroundColor: DS.accent,
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
              child: Text(actionLabel!),
            ),
          ],
        ],
      ),
    );
  }
}
