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
          title: Text(name, style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: DS.textPrimary)),
          bottom: TabBar(
            controller: _tabCtrl,
            indicatorColor: DS.accent,
            labelColor: DS.textPrimary,
            unselectedLabelColor: DS.textTertiary,
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
      Text(_error!, style: TextStyle(color: DS.textSecondary)),
      SizedBox(height: 16),
      FilledButton(onPressed: _loadExplore, child: Text('重试')),
    ]));
    if (_sections.isEmpty) return Center(child: Text('暂无内容', style: TextStyle(color: DS.textTertiary)));

    return ListView.builder(padding: EdgeInsets.only(bottom: 100), itemCount: _sections.length, itemBuilder: (_, si) {
      final section = _sections[si];
      final items = (section['items'] as List?) ?? [];
      if (items.isEmpty) return SizedBox.shrink();
      final title = (section['title'] ?? '').toString();
      return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Padding(padding: EdgeInsets.fromLTRB(16, 16, 16, 8),
          child: Row(children: [
            Text(title, style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: DS.textPrimary)),
            Spacer(),
            GestureDetector(onTap: () => GoRouter.of(context).push('/source/${widget.sourceId}/category?initial=$title'),
              child: Text('更多', style: TextStyle(fontSize: 13, color: DS.textTertiary))),
          ])),
        SizedBox(height: 200, child: ListView.separated(
          padding: EdgeInsets.symmetric(horizontal: 16), scrollDirection: Axis.horizontal,
          itemCount: items.length, separatorBuilder: (_, __) => SizedBox(width: 12),
          itemBuilder: (_, i) {
            final item = items[i];
            final cover = (item['cover'] ?? item['coverUrl'] ?? '').toString();
            final comicId = (item['id'] ?? '').toString();
            final cTitle = (item['title'] ?? '').toString();
            return GestureDetector(onTap: () => _enterComic(widget.sourceId, comicId),
              child: SizedBox(width: 120, child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Expanded(child: Container(decoration: BoxDecoration(borderRadius: BorderRadius.circular(12), color: DS.surface1),
                  clipBehavior: Clip.antiAlias,
                  child: cover.isNotEmpty ? CachedNetworkImage(imageUrl: cover, fit: BoxFit.cover, width: double.infinity,
                    httpHeaders: {'Referer': 'https://${Uri.parse(cover).host}/'},
                    errorWidget: (_, __, ___) => Container(color: DS.surface2, child: Icon(Icons.menu_book_rounded, color: DS.textDisabled)))
                    : Container(color: DS.surface2))),
                SizedBox(height: 6),
                Text(cTitle, maxLines: 1, overflow: TextOverflow.ellipsis, style: TextStyle(fontSize: 13, fontWeight: FontWeight.w500, color: DS.textPrimary)),
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
