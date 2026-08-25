import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../../../app/ds.dart';
import '../../../plugins/source_data_service.dart';
import '../../../plugins/source_sdk.dart';
import '../../../plugins/source_routes_service.dart';

/// 源详情页 — Tab 分离：首页(Explore) / 分类(Category) / 设置
class SourceDetailPage extends StatefulWidget {
  final String sourceId;
  final String sourceName;
  const SourceDetailPage({super.key, required this.sourceId, this.sourceName = ''});
  @override
  State<SourceDetailPage> createState() => _SourceDetailPageState();
}

class _SourceDetailPageState extends State<SourceDetailPage> with SingleTickerProviderStateMixin {
  late TabController _tabCtrl;
  bool _loadingExplore = true;
  List<Map<String, dynamic>> _sections = [];
  List<Map<String, dynamic>> _categories = [];
  String? _error;

  // 线路检测
  List<RouteProbe> _routes = [];
  bool _routeLoading = false;
  String? _selectedRoute;

  @override
  void initState() {
    super.initState();
    _tabCtrl = TabController(length: 3, vsync: this);
    _preloadAndLoad();
  }

  Future<void> _preloadAndLoad() async {
    await SourceDataService.instance.loadLocal(widget.sourceId);
    if (!mounted) return;
    // 加载当前选中线路
    _selectedRoute = SourceRoutesService.instance.getSelectedRoute(widget.sourceId);
    await _loadExplore();
    if (!mounted) return;
    _loadCategories();
    // 后台自动检测最优线路（不阻塞 UI）
    _autoDetectRoute();
  }

  /// 自动检测最优线路
  Future<void> _autoDetectRoute() async {
    try {
      final best = await SourceRoutesService.instance.autoSelect(widget.sourceId);
      if (best != null && mounted) {
        setState(() => _selectedRoute = best.host);
      }
    } catch (_) {}
  }

  /// 手动测速所有线路
  Future<void> _probeRoutes() async {
    if (_routeLoading) return;
    setState(() => _routeLoading = true);
    try {
      _routes = await SourceRoutesService.instance.probeRoutes(widget.sourceId);
      if (mounted) setState(() => _routeLoading = false);
    } catch (_) {
      if (mounted) setState(() => _routeLoading = false);
    }
  }

  /// 手动切换线路
  Future<void> _selectRoute(String host) async {
    HapticFeedback.selectionClick();
    if (widget.sourceId == 'jm') {
      final hosts = await SourceRoutesService.instance.extractHosts(widget.sourceId);
      final index = hosts.indexOf(host);
      if (index >= 0) {
        await SourceRoutesService.instance.selectRoute(widget.sourceId, 'apiDomain', index + 1);
      }
    } else {
      await SourceRoutesService.instance.selectRoute(widget.sourceId, 'base_url', host);
      await SourceRoutesService.instance.selectRoute(widget.sourceId, 'domains', host);
    }
    setState(() => _selectedRoute = host);
    // 重新加载源（清除缓存使新线路生效）
    SourceDataService.instance.clearCache();
    await SourceDataService.instance.loadLocal(widget.sourceId);
    _loadExplore();
  }

  @override
  void dispose() { _tabCtrl.dispose(); super.dispose(); }

  Future<void> _loadExplore() async {
    if (!mounted) return;
    setState(() => _loadingExplore = true);
    try {
      final result = await SourceDataService.instance.explore(widget.sourceId);
      if (!mounted) return;
      setState(() {
        _sections = (result['sections'] as List?)?.map((e) => Map<String, dynamic>.from(e as Map)).toList() ?? [];
        _loadingExplore = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() { _loadingExplore = false; _error = e.toString(); });
    }
  }

  Future<void> _loadCategories() async {
    try {
      final cats = await SourceDataService.instance.categories(widget.sourceId);
      if (!mounted) return;
      setState(() => _categories = cats);
    } catch (_) {}
  }

  void _enterComic(String sourceId, String comicId) {
    if (comicId.isEmpty || sourceId.isEmpty) return;
    final name = widget.sourceName.isNotEmpty ? widget.sourceName : widget.sourceId;
    GoRouter.of(context).push('/source/$sourceId/comic/$comicId?sourceName=${Uri.encodeComponent(name)}');
  }

  @override
  Widget build(BuildContext context) {
    final name = widget.sourceName.isNotEmpty ? widget.sourceName : widget.sourceId;
    return DefaultTabController(
      length: 3,
      child: Scaffold(
        backgroundColor: DS.bg,
        appBar: AppBar(
          backgroundColor: DS.bg,
          elevation: 0, scrolledUnderElevation: 0,
          titleSpacing: 0,
          title: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisSize: MainAxisSize.min, children: [
            Text('$name', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: DS.textPrimary, letterSpacing: -0.3)),
            Text(widget.sourceId, style: TextStyle(fontSize: 11, color: DS.textTertiary, letterSpacing: 0.3)),
          ]),
          actions: [
            IconButton(icon: Icon(Icons.search_rounded, color: DS.textPrimary),
              onPressed: () => GoRouter.of(context).push('/search?sourceId=${widget.sourceId}&name=${Uri.encodeComponent(name)}&scope=source')),
          ],
          bottom: TabBar(
            controller: _tabCtrl,
            indicatorColor: DS.accent,
            indicatorSize: TabBarIndicatorSize.label,
            indicatorWeight: 3,
            labelColor: DS.textPrimary,
            unselectedLabelColor: DS.textTertiary,
            labelStyle: TextStyle(fontSize: 14, fontWeight: FontWeight.w700),
            unselectedLabelStyle: TextStyle(fontSize: 14, fontWeight: FontWeight.w500),
            tabs: [Tab(text: '首页'), Tab(text: '分类'), Tab(text: '线路')],
          ),
        ),
        body: TabBarView(controller: _tabCtrl, children: [
          _buildExplore(),
          _buildCategoryGrid(),
          _buildRoutes(),
        ]),
      ),
    );
  }


  String _fixUrl(String url) {
    if (url.startsWith('//')) return 'https:' + url;
    if (url.startsWith('/')) return 'https://' + widget.sourceId + url;
    return url;
  }

  Map<String, String> _imgHeaders(String url) {
    final base = SourceDataService.instance.baseUrl(widget.sourceId);
    return {'Referer': SourceDataService.getReferer(url, base)};
  }

  String _categoryLink(String title) {
    final normalized = title.trim();
    if (normalized.contains('排行') || normalized == '热门' || normalized == '本周' || normalized == '本月' || normalized == '今日') {
      return '/source/${widget.sourceId}/category?initial=${Uri.encodeComponent('排行')}&param=${Uri.encodeComponent('ranking')}';
    }
    return '/source/${widget.sourceId}/category?initial=${Uri.encodeComponent(normalized)}';
  }

  // ===== 首页（explore 板块）=====
  Widget _buildExplore() {
    if (_loadingExplore) return Center(child: CircularProgressIndicator(color: DS.accent));
    if (_error != null) return Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
      Icon(Icons.cloud_off_rounded, size: 44, color: DS.textDisabled),
      SizedBox(height: 16),
      Padding(padding: EdgeInsets.symmetric(horizontal: 32),
        child: Text(_error!, textAlign: TextAlign.center, style: TextStyle(color: DS.textSecondary, fontSize: 13))),
      SizedBox(height: 16),
      FilledButton(onPressed: _loadExplore, child: Text('重试')),
    ]));
    if (_sections.isEmpty) return Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
      Icon(Icons.menu_book_rounded, size: 44, color: DS.textDisabled),
      SizedBox(height: 12),
      Text('暂无内容', style: TextStyle(color: DS.textTertiary)),
      SizedBox(height: 8),
      Text('可能是源站无响应，试试开 VPN', style: TextStyle(color: DS.textDisabled, fontSize: 12)),
    ]));

    return ListView.builder(padding: EdgeInsets.only(top: 8, bottom: 100), itemCount: _sections.length, itemBuilder: (_, si) {
      final section = _sections[si];
      final items = (section['items'] as List?) ?? [];
      if (items.isEmpty) return SizedBox.shrink();
      final title = (section['title'] ?? '').toString();
      return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Padding(padding: EdgeInsets.fromLTRB(16, 20, 16, 12),
          child: Row(children: [
            Container(width: 4, height: 18, decoration: BoxDecoration(color: DS.accent, borderRadius: BorderRadius.circular(2))),
            SizedBox(width: 8),
            Expanded(child: Text(title, style: TextStyle(fontSize: 17, fontWeight: FontWeight.w800, color: DS.textPrimary))),
            GestureDetector(onTap: () => GoRouter.of(context).push(_categoryLink(title)),
              child: Row(children: [
                Text('查看全部', style: TextStyle(fontSize: 12, color: DS.textTertiary)),
                Icon(Icons.chevron_right_rounded, size: 16, color: DS.textTertiary),
              ])),
          ])),
        SizedBox(height: 250, child: ListView.separated(
          padding: EdgeInsets.symmetric(horizontal: 16), scrollDirection: Axis.horizontal,
          itemCount: items.length, separatorBuilder: (_, __) => SizedBox(width: 12),
          itemBuilder: (_, i) {
            final item = items[i];
            final cover = (item['cover'] ?? item['coverUrl'] ?? '').toString();
            final comicId = (item['id'] ?? '').toString();
            final cTitle = (item['title'] ?? '').toString();
            return GestureDetector(onTap: () => _enterComic(widget.sourceId, comicId),
              child: SizedBox(width: 140, child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Expanded(child: Container(
                  decoration: BoxDecoration(borderRadius: BorderRadius.circular(14), color: DS.surface2,
                    boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.35), blurRadius: 10, offset: Offset(0, 4))]),
                  clipBehavior: Clip.antiAlias,
                  child: cover.isNotEmpty ? CachedNetworkImage(imageUrl: cover, fit: BoxFit.cover, width: double.infinity,
                    httpHeaders: {'Referer': SourceDataService.getReferer(cover, SourceDataService.instance.baseUrl(widget.sourceId))},
                    errorWidget: (_, __, ___) => Container(color: DS.surface2, child: Icon(Icons.menu_book_rounded, color: DS.textDisabled)))
                    : Container(color: DS.surface2, child: Icon(Icons.menu_book_rounded, color: DS.textDisabled)),
                )),
                SizedBox(height: 8),
                Text(cTitle, maxLines: 2, overflow: TextOverflow.ellipsis, style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: DS.textPrimary, height: 1.3)),
              ])));
          })),
      ]);
    });
  }

  // ===== 分类页 =====
  Widget _buildCategoryGrid() {
    if (_categories.isEmpty) return Center(child: Text('暂无分类', style: TextStyle(color: DS.textTertiary)));
    return ListView.builder(padding: EdgeInsets.only(bottom: 100), itemCount: _categories.length, itemBuilder: (_, pi) {
      final part = _categories[pi];
      final catName = (part['name'] ?? '').toString();
      final cats = (part['categories'] as List?) ?? [];
      if (cats.isEmpty) return SizedBox.shrink();
      return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Padding(padding: EdgeInsets.fromLTRB(16, 16, 16, 8),
          child: Text(catName, style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600, color: DS.textSecondary))),
        Padding(padding: EdgeInsets.symmetric(horizontal: 12), child: Wrap(spacing: 8, runSpacing: 8,
          children: cats.map((c) {
            final cname = (c is Map ? (c['name'] ?? '') : c).toString();
            final cparam = (c is Map ? (c['param'] ?? '') : '').toString();
            return ActionChip(label: Text(cname, style: TextStyle(fontSize: 13, color: DS.textPrimary)),
              backgroundColor: DS.surface2,
              onPressed: () => GoRouter.of(context).push('/source/${widget.sourceId}/category?sourceName=${Uri.encodeComponent(widget.sourceName.isNotEmpty ? widget.sourceName : widget.sourceId)}&initial=${Uri.encodeComponent(cname)}&param=${Uri.encodeComponent(cparam)}'));
          }).toList())),
      ]);
    });
  }

  // ===== 线路 Tab =====
  Widget _buildRoutes() {
    if (_routeLoading) {
      return Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
        const CircularProgressIndicator(color: DS.accent, strokeWidth: 2.5),
        const SizedBox(height: 16),
        const Text('正在测速所有线路...', style: TextStyle(color: DS.textTertiary, fontSize: 13)),
      ]));
    }

    // 首次进入自动测速
    if (_routes.isEmpty) {
      // 延迟触发避免和 explore 竞争
      Future.microtask(() => _probeRoutes());
      return Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
        Icon(Icons.dns_rounded, size: 40, color: DS.textDisabled),
        const SizedBox(height: 12),
        const Text('点击下方按钮检测线路', style: TextStyle(color: DS.textTertiary, fontSize: 14)),
        const SizedBox(height: 16),
        FilledButton.icon(
          onPressed: _probeRoutes,
          icon: const Icon(Icons.speed_rounded, size: 18),
          label: const Text('检测线路'),
        ),
      ]));
    }

    return ListView(
      padding: const EdgeInsets.all(DS.sp16),
      children: [
        // 自动选择按钮
        Padding(
          padding: const EdgeInsets.only(bottom: DS.sp16),
          child: Row(children: [
            Expanded(child: FilledButton.icon(
              onPressed: () async {
                HapticFeedback.mediumImpact();
                await _autoDetectRoute();
                _probeRoutes();
              },
              icon: const Icon(Icons.auto_awesome_rounded, size: 18),
              label: const Text('自动选择最优'),
              style: FilledButton.styleFrom(backgroundColor: DS.accent),
            )),
            const SizedBox(width: DS.sp8),
            OutlinedButton.icon(
              onPressed: _probeRoutes,
              icon: const Icon(Icons.refresh_rounded, size: 18),
              label: const Text('重新测速'),
            ),
          ]),
        ),

        // 当前线路
        if (_selectedRoute != null)
          Container(
            padding: const EdgeInsets.all(DS.sp12),
            margin: const EdgeInsets.only(bottom: DS.sp12),
            decoration: BoxDecoration(
              color: DS.accent.withValues(alpha: 0.08),
              borderRadius: BorderRadius.circular(DS.rMd),
              border: Border.all(color: DS.accent.withValues(alpha: 0.3), width: 0.5),
            ),
            child: Row(children: [
              const Icon(Icons.check_circle_rounded, size: 18, color: DS.success),
              const SizedBox(width: 8),
              const Text('当前线路', style: TextStyle(fontSize: 13, color: DS.textTertiary)),
              const SizedBox(width: 8),
              Expanded(child: Text(_selectedRoute!, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: DS.textPrimary))),
            ]),
          ),

        // 线路列表
        ...(_routes.map((r) {
          final selected = _selectedRoute == r.host;
          final latency = r.latencyMs;
          final color = latency == null
            ? DS.textDisabled
            : (latency < 300 ? DS.success : (latency < 800 ? DS.warning : DS.error));
          return GestureDetector(
            onTap: () => _selectRoute(r.host),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: DS.sp16, vertical: 14),
              margin: const EdgeInsets.only(bottom: DS.sp8),
              decoration: BoxDecoration(
                color: selected ? DS.accent.withValues(alpha: 0.08) : DS.surface1,
                borderRadius: BorderRadius.circular(DS.rMd),
                border: Border.all(color: selected ? DS.accent.withValues(alpha: 0.4) : Colors.transparent, width: 1),
              ),
              child: Row(children: [
                Expanded(child: Text(r.host, style: TextStyle(fontSize: 14, fontWeight: selected ? FontWeight.w600 : FontWeight.w400, color: DS.textPrimary))),
                if (latency != null)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(color: color.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(6)),
                    child: Text('${latency}ms', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: color)),
                  )
                else
                  const Text('不可达', style: TextStyle(fontSize: 12, color: DS.textDisabled)),
                const SizedBox(width: 8),
                if (selected)
                  const Icon(Icons.check_rounded, size: 18, color: DS.accent)
                else
                  Icon(Icons.radio_button_unchecked_rounded, size: 18, color: DS.textTertiary),
              ]),
            ),
          );
        }).toList()),

        const SizedBox(height: DS.sp20),
        // 说明
        Container(
          padding: const EdgeInsets.all(DS.sp12),
          decoration: BoxDecoration(color: DS.surface1, borderRadius: BorderRadius.circular(DS.rMd)),
          child: const Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Icon(Icons.info_outline_rounded, size: 16, color: DS.textTertiary),
            SizedBox(width: 8),
            Expanded(child: Text('线路检测从源 JS 提取所有 API 域名，并发测速（HEAD 请求，5秒超时）。选择线路后源会使用该域名加载内容。建议选择延迟最低的线路。海外源（如 jm）可能需要 VPN。',
              style: TextStyle(fontSize: 12, color: DS.textTertiary, height: 1.5))),
          ]),
        ),
      ],
    );
  }
}
