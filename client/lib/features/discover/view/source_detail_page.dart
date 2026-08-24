import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../../../app/ds.dart';
import '../../../plugins/source_data_service.dart';
import '../../../plugins/source_sdk.dart';

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

  @override
  void initState() {
    super.initState();
    _tabCtrl = TabController(length: 2, vsync: this);
    _loadExplore();
    _loadCategories();
    // 预加载源到 QuickJS（修复首次点击不加载）
    SourceDataService.instance.loadLocal(widget.sourceId);
  }

  @override
  void dispose() { _tabCtrl.dispose(); super.dispose(); }

  Future<void> _loadExplore() async {
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
    GoRouter.of(context).push('/source/$sourceId/comic/$comicId');
  }

  @override
  Widget build(BuildContext context) {
    final name = widget.sourceName.isNotEmpty ? widget.sourceName : widget.sourceId;
    return DefaultTabController(
      length: 2,
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
              onPressed: () => GoRouter.of(context).push('/search?sourceId=${widget.sourceId}&name=${Uri.encodeComponent(name)}')),
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
            tabs: [Tab(text: '首页'), Tab(text: '分类')],
          ),
        ),
        body: TabBarView(controller: _tabCtrl, children: [
          _buildExplore(),
          _buildCategoryGrid(),
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
    final u = Uri.tryParse(_fixUrl(url));
    if (u != null && u.host.isNotEmpty) return {'Referer': 'https://' + u.host + '/'};
    return {};
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
            GestureDetector(onTap: () => GoRouter.of(context).push('/source/${widget.sourceId}/category?initial=${Uri.encodeComponent(title)}'),
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
                    httpHeaders: {'Referer': 'https://${Uri.parse(cover).host}/'},
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
              onPressed: () => GoRouter.of(context).push('/source/${widget.sourceId}/category?initial=${Uri.encodeComponent(cname)}&param=${Uri.encodeComponent(cparam)}'));
          }).toList())),
      ]);
    });
  }
}
