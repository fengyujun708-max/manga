import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../../../plugins/source_data_service.dart';
import '../../../core/services/library_service.dart';
import '../../../app/ds.dart';
import 'source_reader_page.dart';

class SourceComicPage extends StatefulWidget {
  final String sourceId;
  final String comicId;
  const SourceComicPage({super.key, required this.sourceId, required this.comicId});
  @override
  State<SourceComicPage> createState() => _SourceComicPageState();
}

class _SourceComicPageState extends State<SourceComicPage> {
  bool _loading = true;
  String? _error;
  Map<String, dynamic> _info = {};
  List<dynamic> _chapters = [];
  bool _isFavorited = false;

  @override
  void initState() { super.initState(); _load(); _checkFav(); }

  Future<void> _checkFav() async {
    final fav = await LibraryService.instance.isFavorited(widget.sourceId, widget.comicId);
    if (mounted) setState(() => _isFavorited = fav);
  }

  Future<void> _toggleFav() async {
    HapticFeedback.mediumImpact();
    await LibraryService.instance.toggleFavorite({
      'sourceId': widget.sourceId,
      'comicId': widget.comicId,
      'title': _info['title'] ?? '',
      'cover': _info['cover'] ?? '',
    });
    final fav = await LibraryService.instance.isFavorited(widget.sourceId, widget.comicId);
    if (mounted) setState(() => _isFavorited = fav);
  }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    final result = await SourceDataService.instance.comic(widget.sourceId, widget.comicId);
    if (!mounted) return;
    final data = result['detail'] as Map<String, dynamic>? ?? {};
    setState(() {
      _info = data;
      _chapters = (result['chapters'] ?? _extractChapters(data)) as List? ?? [];
      _loading = false;
      if (result['error'] != null) _error = result['error'];
    });
  }

  List<dynamic> _extractChapters(Map<String, dynamic> data) {
    for (final key in ['chapters', 'episodes', 'comics']) {
      if (data[key] is List) return data[key];
    }
    return [];
  }

  String get _title => (_info['title'] ?? _info['name'] ?? widget.comicId).toString();
  String get _cover => (_info['cover'] ?? _info['coverUrl'] ?? '').toString();
  String get _referer {
    final u = Uri.tryParse(_cover);
    return u != null && u.host.isNotEmpty ? 'https://' + u.host + '/' : '';
  }
  List<String> get _genres => (_info['genres'] as List?)?.cast<String>() ?? [];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: DS.bg,
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: DS.accent))
          : _error != null
              ? Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Text(_error!, style: TextStyle(color: DS.textSecondary)),
                  SizedBox(height: 16),
                  FilledButton(onPressed: _load, child: Text('重试')),
                ]))
              : _buildBody(),
    );
  }

  Widget _buildBody() {
    final author = (_info['author'] ?? '').toString();
    final desc = (_info['description'] ?? '').toString();

    return CustomScrollView(
      physics: BouncingScrollPhysics(),
      slivers: [
        SliverAppBar(
          expandedHeight: 280,
          pinned: true,
          backgroundColor: DS.bg,
          leading: GestureDetector(
            onTap: () => context.pop(),
            child: Container(margin: EdgeInsets.all(8),
              decoration: BoxDecoration(color: Colors.black26, shape: BoxShape.circle),
              child: Icon(Icons.arrow_back_ios_new_rounded, size: 18, color: DS.textPrimary)),
          ),
          actions: [
            GestureDetector(onTap: _toggleFav, child: Container(margin: EdgeInsets.all(8),
              decoration: BoxDecoration(color: Colors.black26, shape: BoxShape.circle),
              child: Icon(_isFavorited ? Icons.favorite_rounded : Icons.favorite_border_rounded,
                size: 20, color: _isFavorited ? DS.accent : DS.textPrimary))),
          ],
          flexibleSpace: FlexibleSpaceBar(background: buildHero()),
        ),
        // Start reading button
        SliverToBoxAdapter(child: Padding(padding: EdgeInsets.fromLTRB(16, 12, 16, 8),
          child: _chapters.isNotEmpty
            ? FilledButton.icon(
                onPressed: () => openReader(0),
                icon: Icon(Icons.play_arrow_rounded),
                label: Text('开始阅读', style: TextStyle(fontWeight: FontWeight.w700)),
                style: FilledButton.styleFrom(backgroundColor: DS.accent, padding: EdgeInsets.symmetric(vertical: 14)))
            : SizedBox.shrink())),
        // Description
        if (desc.isNotEmpty)
          SliverToBoxAdapter(child: Padding(padding: EdgeInsets.fromLTRB(16, 0, 16, 12),
            child: Text(desc, maxLines: 5, overflow: TextOverflow.ellipsis,
              style: TextStyle(fontSize: 13, color: DS.textSecondary, height: 1.6)))),
        // Chapter header
        SliverToBoxAdapter(child: Padding(padding: EdgeInsets.fromLTRB(16, 8, 16, 4),
          child: Row(children: [
            Text('章节', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: DS.textPrimary)),
            Spacer(),
            Text('${_chapters.length} 话', style: TextStyle(fontSize: 12, color: DS.textTertiary)),
          ]))),
        // Chapter list
        SliverPadding(padding: EdgeInsets.fromLTRB(16, 0, 16, 100), sliver: SliverList(
          delegate: SliverChildBuilderDelegate((ctx, i) {
            final ch = _chapters[i];
            final chId = (ch is Map ? (ch['id'] ?? ch['episode'] ?? '') : ch).toString();
            final chTitle = (ch is Map ? (ch['title'] ?? ch['name'] ?? '第${i + 1}话') : ch).toString();
            return GestureDetector(
              onTap: () { HapticFeedback.selectionClick(); if (chId.isNotEmpty) openReader(i); },
              child: Container(margin: EdgeInsets.only(bottom: 8), padding: EdgeInsets.symmetric(horizontal: 14, vertical: 13),
                decoration: BoxDecoration(color: i % 2 == 0 ? DS.surface1 : DS.surface2, borderRadius: BorderRadius.circular(DS.rSm)),
                child: Row(children: [
                  Container(width: 28, height: 28, decoration: BoxDecoration(color: DS.accentDim, borderRadius: BorderRadius.circular(8)),
                    child: Center(child: Text('${i + 1}', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: DS.accent)))),
                  SizedBox(width: 12),
                  Expanded(child: Text(chTitle, maxLines: 1, overflow: TextOverflow.ellipsis, style: TextStyle(fontSize: 14, color: DS.textPrimary))),
                  Icon(Icons.chevron_right_rounded, size: 18, color: DS.textTertiary),
                ])));
          }, childCount: _chapters.length))),
      ]);
  }

  Widget buildHero() {
    return Stack(fit: StackFit.expand, children: [
      if (_cover.isNotEmpty)
        CachedNetworkImage(imageUrl: _cover, fit: BoxFit.cover, httpHeaders: {'Referer': _referer},
          errorWidget: (_, __, ___) => Container(color: DS.surface1))
      else
        Container(color: DS.surface1),
      Positioned.fill(child: DecoratedBox(decoration: BoxDecoration(
        gradient: LinearGradient(begin: Alignment.topCenter, end: Alignment.bottomCenter,
          colors: [Colors.transparent, Colors.transparent, DS.bg.withValues(alpha: 0.95)])))),
      Positioned(left: 16, right: 16, bottom: 12, child: Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
        Container(width: 100, height: 140,
          decoration: BoxDecoration(borderRadius: BorderRadius.circular(14), color: DS.surface2, boxShadow: [BoxShadow(color: Colors.black38, blurRadius: 12)]),
          clipBehavior: Clip.antiAlias,
          child: _cover.isNotEmpty
            ? CachedNetworkImage(imageUrl: _cover, fit: BoxFit.cover, httpHeaders: {'Referer': _referer},
                errorWidget: (_, __, ___) => Icon(Icons.menu_book_rounded, size: 40, color: DS.textDisabled))
            : Icon(Icons.menu_book_rounded, size: 40, color: DS.textDisabled)),
        SizedBox(width: 12),
        Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(_title, style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: DS.textPrimary)),
          if ((_info['author'] ?? '').toString().isNotEmpty)
            Padding(padding: EdgeInsets.only(top: 4), child: Text(_info['author'], style: TextStyle(fontSize: 13, color: DS.textSecondary))),
          if (_genres.isNotEmpty)
            Padding(padding: EdgeInsets.only(top: 8), child: Wrap(spacing: 6, runSpacing: 4,
              children: _genres.take(4).map((g) => Container(padding: EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(color: DS.glassFill, borderRadius: BorderRadius.circular(10)),
                child: Text(g, style: TextStyle(fontSize: 11, color: DS.textSecondary)))).toList())),
        ])),
      ])),
    ]);
  }

  void openReader(int index) {
    Navigator.of(context).push(MaterialPageRoute(builder: (_) => SourceReaderPage(
      sourceId: widget.sourceId,
      comicId: widget.comicId,
      comicTitle: _title,
      chapters: _chapters.map((c) => c is Map ? Map<String, dynamic>.from(c) : {'id': c.toString(), 'title': c.toString()}).toList(),
      initialChapter: index,
    )));
  }
}
