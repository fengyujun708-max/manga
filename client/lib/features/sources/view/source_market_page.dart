import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:get_it/get_it.dart';
import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../../../app/theme/theme.dart';
import '../../../app/widgets/comic_widgets.dart';
import '../../../plugins/manga_source.dart';
import '../../../plugins/local_venera_source.dart';
import '../../../core/network/api_client.dart';
import '../bloc/source_bloc.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

/// 源市场 — 分类筛选 + 搜索 + 液态玻璃卡片
class SourceMarketPage extends StatefulWidget {
  const SourceMarketPage({super.key});
  @override
  State<SourceMarketPage> createState() => _SourceMarketPageState();
}

class _SourceMarketPageState extends State<SourceMarketPage> with SingleTickerProviderStateMixin {
  List<SourceManifest> _allSources = [];
  List<SourceManifest> _filtered = [];
  bool _loading = true;
  String _search = '';
  late TabController _tabCtrl;
  final _searchCtrl = TextEditingController();

  final _tabs = [
    {'key': 'all', 'name': '全部'},
    {'key': 'zh', 'name': '中文'},
    {'key': 'ja', 'name': '日本'},
    {'key': 'intl', 'name': '海外'},
    {'key': 'adult', 'name': '成人'},
  ];

  @override
  void initState() {
    super.initState();
    _tabCtrl = TabController(length: _tabs.length, vsync: this);
    _tabCtrl.addListener(() { if (!_tabCtrl.indexIsChanging) _applyFilter(); });
    _load();
  }

  @override
  void dispose() { _tabCtrl.dispose(); _searchCtrl.dispose(); super.dispose(); }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/sources');
      final data = res.data;
      _allSources = ((data?['sources'] as List?) ?? []).map((e) => SourceManifest.fromJson(e as Map<String, dynamic>)).toList();
      _applyFilter();
    } catch (e) {
      setState(() => _loading = false);
    }
  }

  void _applyFilter() {
    final tab = _tabs[_tabCtrl.index]['key']!;
    final q = _search.toLowerCase();
    _filtered = _allSources.where((s) {
      // 分类筛选
      if (tab != 'all') {
        final locale = s.metadata['locale']?.toString() ?? '';
        final type = s.metadata['type']?.toString() ?? '';
        if (tab == 'zh' && locale != 'zh') return false;
        if (tab == 'ja' && locale != 'ja') return false;
        if (tab == 'intl' && locale != '' && locale != 'en') return false;
        if (tab == 'adult' && type != 'hentai') return false;
      }
      // 搜索
      if (q.isNotEmpty && !s.name.toLowerCase().contains(q) && !s.id.toLowerCase().contains(q)) return false;
      return true;
    }).toList();
    if (mounted) setState(() {});
  }

  Future<bool> _isInstalled(String id) async {
    final prefs = await SharedPreferences.getInstance();
    final json = prefs.getString('installed_sources') ?? '[]';
    return (jsonDecode(json) as List).any((e) => SourceManifest.fromJson(e as Map<String, dynamic>).id == id);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      body: CustomScrollView(
        physics: const BouncingScrollPhysics(),
        slivers: [
          // 头部
          SliverAppBar(
            pinned: true,
            backgroundColor: Colors.transparent,
            leading: GestureDetector(
              onTap: () => context.pop(),
              child: Container(margin: const EdgeInsets.all(8), decoration: BoxDecoration(color: AppTheme.glassFillLight, shape: BoxShape.circle), child: const Icon(Icons.arrow_back_ios_new_rounded, size: 18, color: AppTheme.textPrimary)),
            ),
            title: const Text('源市场', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 18, color: AppTheme.textPrimary)),
          ),

          // 搜索框
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 4, 16, 12),
              child: LiquidGlass(
                radius: BorderRadius.circular(14),
                padding: const EdgeInsets.symmetric(horizontal: 14),
                fillColor: AppTheme.glassFillRegular,
                child: Row(children: [
                  const Icon(Icons.search_rounded, size: 20, color: AppTheme.textTertiary),
                  const SizedBox(width: 10),
                  Expanded(child: TextField(
                    controller: _searchCtrl,
                    style: const TextStyle(fontSize: 14, color: AppTheme.textPrimary),
                    decoration: const InputDecoration(hintText: '搜索漫画源', hintStyle: TextStyle(color: AppTheme.textTertiary, fontSize: 14), border: InputBorder.none, isDense: true, contentPadding: EdgeInsets.symmetric(vertical: 14)),
                    onChanged: (v) { _search = v; _applyFilter(); },
                  )),
                ]),
              ),
            ),
          ),

          // 分类Tab
          SliverToBoxAdapter(
            child: TabBar(
              controller: _tabCtrl,
              isScrollable: true,
              tabAlignment: TabAlignment.start,
              indicatorColor: AppTheme.primary,
              indicatorSize: TabBarIndicatorSize.label,
              labelColor: AppTheme.primary,
              unselectedLabelColor: AppTheme.textTertiary,
              labelStyle: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
              unselectedLabelStyle: const TextStyle(fontSize: 14),
              tabPhysics: const BouncingScrollPhysics(),
              tabs: _tabs.map((t) => Tab(text: t['name'])).toList(),
            ),
          ),

          // 源列表
          if (_loading)
            const SliverFillRemaining(child: LoadingState(text: '加载源市场...'))
          else if (_filtered.isEmpty)
            const SliverFillRemaining(child: EmptyState(icon: Icons.search_off_rounded, title: '未找到源'))
          else
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 100),
              sliver: SliverList(
                delegate: SliverChildBuilderDelegate(
                  (ctx, i) => _MarketSourceCard(manifest: _filtered[i], onInstalled: _load),
                  childCount: _filtered.length,
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _MarketSourceCard extends StatefulWidget {
  final SourceManifest manifest;
  final VoidCallback onInstalled;
  const _MarketSourceCard({required this.manifest, required this.onInstalled});
  @override
  State<_MarketSourceCard> createState() => _MarketSourceCardState();
}

class _MarketSourceCardState extends State<_MarketSourceCard> {
  bool _installing = false;
  bool _installed = false;

  @override
  void initState() { super.initState(); _checkInstalled(); }

  Future<void> _checkInstalled() async {
    final prefs = await SharedPreferences.getInstance();
    final json = prefs.getString('installed_sources') ?? '[]';
    final installed = (jsonDecode(json) as List).any((e) {
      try { return SourceManifest.fromJson(e as Map<String, dynamic>).id == widget.manifest.id; } catch (_) { return false; }
    });
    if (mounted) setState(() => _installed = installed);
  }

  Future<void> _install() async {
    if (_installed || _installing) return;
    setState(() => _installing = true);
    HapticFeedback.mediumImpact();
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/sources/${widget.manifest.id}');
      if (res.statusCode == 200 && res.data != null) {
        final manifest = SourceManifest.fromJson(res.data as Map<String, dynamic>);
        // 下载源 JS 到本地
        final sourceDir = await LocalVeneraInstaller.ensureSourceDir();
        if (sourceDir != null) {
          await LocalVeneraInstaller.install(manifest, sourceDir);
        }
        final prefs = await SharedPreferences.getInstance();
        final json = prefs.getString('installed_sources') ?? '[]';
        final list = jsonDecode(json) as List;
        if (!list.any((e) {
          try { return SourceManifest.fromJson(e as Map<String, dynamic>).id == manifest.id; } catch (_) { return false; }
        })) {
          list.add(manifest.toJson());
          await prefs.setString('installed_sources', jsonEncode(list));
        }
        if (mounted) {
          setState(() { _installed = true; _installing = false; });
          HapticFeedback.heavyImpact();
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            behavior: SnackBarBehavior.floating,
            backgroundColor: AppTheme.surface,
            content: Row(children: [
              const Icon(Icons.check_circle_rounded, color: AppTheme.success, size: 18),
              const SizedBox(width: 8),
              Expanded(child: Text('「${manifest.name}」安装成功', style: const TextStyle(color: AppTheme.textPrimary, fontSize: 13))),
            ]),
            action: SnackBarAction(label: '去发现页', textColor: AppTheme.primary, onPressed: () => GoRouter.of(context).go('/discover')),
          ));
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() => _installing = false);
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          backgroundColor: AppTheme.destructive,
          content: Text('安装失败: $e', style: const TextStyle(color: Colors.white, fontSize: 13)),
        ));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final m = widget.manifest;
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: LiquidGlass(
        radius: BorderRadius.circular(AppTheme.radiusLg),
        padding: const EdgeInsets.all(14),
        fillColor: AppTheme.glassFillRegular,
        child: Row(children: [
          // 图标
          Container(
            width: 48, height: 48,
            decoration: BoxDecoration(
              gradient: AppTheme.primaryGradient,
              borderRadius: BorderRadius.circular(14),
              boxShadow: [BoxShadow(color: AppTheme.primary.withValues(alpha: 0.25), blurRadius: 12)],
            ),
            child: Center(child: Text(m.name.isNotEmpty ? m.name.characters.first : '?', style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w800, color: Colors.white))),
          ),
          const SizedBox(width: 12),
          // 信息
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              Text(m.name, style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700, color: AppTheme.textPrimary)),
              const SizedBox(width: 8),
              SourceBadge.fromMeta(m.metadata),
            ]),
            const SizedBox(height: 4),
            Text('v${m.version} · ${m.author}', style: const TextStyle(fontSize: 11, color: AppTheme.textTertiary)),
          ])),
          // 安装按钮
          _installing
            ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2, color: AppTheme.primary))
            : _installed
              ? const Icon(Icons.check_circle_rounded, color: AppTheme.success, size: 24)
              : GestureDetector(
                  onTap: _install,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                    decoration: BoxDecoration(gradient: AppTheme.primaryGradient, borderRadius: BorderRadius.circular(10), boxShadow: AppTheme.glowShadow),
                    child: const Text('安装', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w700, color: Colors.white)),
                  ),
                ),
        ]),
      ),
    );
  }
}