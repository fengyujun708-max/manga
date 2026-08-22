import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:get_it/get_it.dart';
import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../../../app/ds.dart';
import '../../../plugins/manga_source.dart';
import '../../../plugins/source_installer.dart';
import '../../../core/network/api_client.dart';

/// 源市场 — 分类筛选 + 搜索 + 液态玻璃卡片
class SourceMarketPage extends StatefulWidget {
  const SourceMarketPage({super.key});
  @override
  State<SourceMarketPage> createState() => _SourceMarketPageState();
}

class _SourceMarketPageState extends State<SourceMarketPage> {
  List<SourceManifest> _all = [];
  List<SourceManifest> _filtered = [];
  bool _loading = true;
  String? _error;
  String _search = '';
  final _searchCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() { _searchCtrl.dispose(); super.dispose(); }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/sources');
      _all = ((res.data?['sources'] as List?) ?? []).map((e) => SourceManifest.fromJson(e as Map<String, dynamic>)).toList();
      _applyFilter();
    } catch (e) {
      _error = '无法连接源服务器，请检查网络后重试';
    }
    if (mounted) setState(() => _loading = false);
  }

  void _applyFilter() {
    final q = _search.toLowerCase();
    _filtered = _all.where((s) {
      if (q.isNotEmpty && !s.name.toLowerCase().contains(q) && !s.id.toLowerCase().contains(q)) return false;
      return true;
    }).toList();
    if (mounted) setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: DS.bg,
      body: CustomScrollView(
        physics: const BouncingScrollPhysics(),
        slivers: [
          SliverAppBar(
            pinned: true, backgroundColor: Colors.transparent,
            leading: GestureDetector(
              onTap: () => context.pop(),
              child: Container(margin: const EdgeInsets.all(8), decoration: BoxDecoration(color: DS.glassFill, shape: BoxShape.circle), child: const Icon(Icons.arrow_back_ios_new_rounded, size: 18, color: DS.textPrimary)),
            ),
            title: const Text('源市场', style: DS.headline),
          ),
          // 搜索
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(DS.sp16, DS.sp4, DS.sp16, DS.sp12),
              child: Glass(
                radius: DS.rMd,
                padding: const EdgeInsets.symmetric(horizontal: DS.sp16),
                child: TextField(
                  controller: _searchCtrl,
                  style: const TextStyle(fontSize: 14, color: DS.textPrimary),
                  decoration: const InputDecoration(hintText: '搜索漫画源', hintStyle: TextStyle(color: DS.textTertiary, fontSize: 14), border: InputBorder.none, contentPadding: EdgeInsets.symmetric(vertical: 14), prefixIcon: Icon(Icons.search_rounded, size: 20, color: DS.textTertiary)),
                  onChanged: (v) { _search = v; _applyFilter(); },
                ),
              ),
            ),
          ),
          if (_loading)
            const SliverFillRemaining(child: Center(child: CircularProgressIndicator(color: DS.accent, strokeWidth: 2)))
          else if (_error != null)
            SliverFillRemaining(child: EmptyState(icon: Icons.cloud_off_rounded, title: '加载失败', subtitle: _error, actionLabel: '重试', onAction: _load))
          else if (_filtered.isEmpty)
            const SliverFillRemaining(child: EmptyState(icon: Icons.search_off_rounded, title: '未找到源'))
          else
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(DS.sp16, DS.sp8, DS.sp16, 120),
              sliver: SliverList(
                delegate: SliverChildBuilderDelegate(
                  (ctx, i) => _MarketCard(manifest: _filtered[i], onDone: _load),
                  childCount: _filtered.length,
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _MarketCard extends StatefulWidget {
  final SourceManifest manifest;
  final VoidCallback onDone;
  const _MarketCard({required this.manifest, required this.onDone});
  @override
  State<_MarketCard> createState() => _MarketCardState();
}

class _MarketCardState extends State<_MarketCard> {
  bool _installing = false;
  bool _installed = false;

  @override
  void initState() { super.initState(); _check(); }

  Future<void> _check() async {
    final prefs = await SharedPreferences.getInstance();
    final json = prefs.getString('installed_sources') ?? '[]';
    final ok = (jsonDecode(json) as List).any((e) {
      try { return SourceManifest.fromJson(e as Map<String, dynamic>).id == widget.manifest.id; } catch (_) { return false; }
    });
    if (mounted) setState(() => _installed = ok);
  }

  Future<void> _install() async {
    if (_installed || _installing) return;
    setState(() => _installing = true);
    HapticFeedback.mediumImpact();
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/sources/${widget.manifest.id}');
      if (res.statusCode == 200 && res.data != null) {
        final m = SourceManifest.fromJson(res.data as Map<String, dynamic>);
        final dir = await SourceInstaller.ensureSourceDir();
        final jsOk = dir != null && await SourceInstaller.install(m, dir);
        if (!jsOk) {
          if (mounted) {
            setState(() => _installing = false);
            ScaffoldMessenger.of(context).showSnackBar(SnackBar(
              behavior: SnackBarBehavior.floating,
              backgroundColor: DS.surface1,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(DS.rMd)),
              content: Row(children: [
                const Icon(Icons.wifi_off_rounded, color: DS.warning, size: 18),
                const SizedBox(width: 8),
                const Expanded(child: Text('源脚本下载失败，请检查网络后重试', style: TextStyle(color: DS.textPrimary, fontSize: 13))),
              ]),
            ));
          }
          return;
        }
        final prefs = await SharedPreferences.getInstance();
        final json = prefs.getString('installed_sources') ?? '[]';
        final list = jsonDecode(json) as List;
        if (!list.any((e) { try { return SourceManifest.fromJson(e as Map<String, dynamic>).id == m.id; } catch (_) { return false; } })) {
          list.add(m.toJson());
          await prefs.setString('installed_sources', jsonEncode(list));
        }
        if (mounted) {
          setState(() { _installed = true; _installing = false; });
          HapticFeedback.heavyImpact();
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            behavior: SnackBarBehavior.floating,
            backgroundColor: DS.surface1,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(DS.rMd)),
            content: Row(children: [
              const Icon(Icons.check_circle_rounded, color: DS.success, size: 18),
              const SizedBox(width: 8),
              Expanded(child: Text('「${m.name}」安装成功', style: const TextStyle(color: DS.textPrimary, fontSize: 13))),
            ]),
            action: SnackBarAction(label: '去发现页', textColor: DS.accent, onPressed: () => GoRouter.of(context).go('/discover')),
          ));
        }
      }
    } catch (e) {
      if (mounted) { setState(() => _installing = false); HapticFeedback.heavyImpact(); }
    }
  }

  @override
  Widget build(BuildContext context) {
    final m = widget.manifest;
    return Padding(
      padding: const EdgeInsets.only(bottom: DS.sp12),
      child: Glass(
        radius: DS.rLg,
        padding: const EdgeInsets.all(DS.sp16),
        child: Row(children: [
          Container(
            width: 48, height: 48,
            decoration: BoxDecoration(color: DS.surface2, borderRadius: BorderRadius.circular(14)),
            child: Center(child: Text(m.name.isNotEmpty ? m.name.characters.first : '?', style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w800, color: DS.textPrimary))),
          ),
          const SizedBox(width: DS.sp12),
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(m.name, style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700, color: DS.textPrimary)),
            const SizedBox(height: 4),
            Text('v${m.version} · ${m.author}', style: const TextStyle(fontSize: 11, color: DS.textTertiary)),
          ])),
          if (_installing)
            const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2, color: DS.accent))
          else if (_installed)
            const Icon(Icons.check_circle_rounded, color: DS.success, size: 24)
          else
            GestureDetector(
              onTap: _install,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                decoration: BoxDecoration(color: DS.accent, borderRadius: BorderRadius.circular(DS.rSm)),
                child: const Text('安装', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w700, color: Colors.white)),
              ),
            ),
        ]),
      ),
    );
  }
}