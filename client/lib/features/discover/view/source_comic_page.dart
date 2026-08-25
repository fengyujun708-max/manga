import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../../../plugins/source_data_service.dart';
import '../../../core/services/library_service.dart';
import '../../../app/ds.dart';
import 'source_reader_page.dart';

/// 漫画详情页 — 电影感 Hero + 真实源数据
class SourceComicPage extends StatefulWidget {
  final String sourceId;
  final String comicId;
  final String sourceName;
  const SourceComicPage({super.key, required this.sourceId, required this.comicId, this.sourceName = ''});
  @override
  State<SourceComicPage> createState() => _SourceComicPageState();
}

class _SourceComicPageState extends State<SourceComicPage> {
  bool _loading = true;
  String? _error;
  Map<String, dynamic> _info = {};
  List<dynamic> _chapters = [];
  bool _isFav = false;
  bool _showFullDesc = false;
  bool _descending = true;

  @override
  void initState() { super.initState(); _load(); _checkFav(); }

  Future<void> _checkFav() async {
    final fav = await LibraryService.instance.isFavorited(widget.sourceId, widget.comicId);
    if (mounted) setState(() => _isFav = fav);
  }

  Future<void> _toggleFav() async {
    HapticFeedback.mediumImpact();
    await LibraryService.instance.toggleFavorite({
      'sourceId': widget.sourceId, 'sourceName': _sourceLabel, 'comicId': widget.comicId,
      'title': _info['title'] ?? '', 'cover': _info['cover'] ?? '',
    });
    final fav = await LibraryService.instance.isFavorited(widget.sourceId, widget.comicId);
    if (mounted) setState(() => _isFav = fav);
  }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final result = await SourceDataService.instance.comic(widget.sourceId, widget.comicId);
      if (!mounted) return;
      // 安全类型转换（不依赖 as，避免 _Map<dynamic,dynamic> 崩溃）
      final detail = result['detail'];
      final data = detail is Map ? Map<String, dynamic>.from(detail) : <String, dynamic>{};
      final chs = result['chapters'];
      setState(() {
        _info = data;
        _chapters = chs is List ? chs : (data['chapters'] is List ? data['chapters'] as List : []);
        _loading = false;
        if (result['error'] != null) _error = result['error'].toString();
      });
    } catch (e) {
      if (!mounted) return;
      setState(() { _loading = false; _error = e.toString().replaceAll('Exception: ', ''); });
    }
  }

  String get _title => (_info['title'] ?? _info['name'] ?? widget.comicId).toString();
  String get _sourceLabel => widget.sourceName.isNotEmpty ? widget.sourceName : widget.sourceId;
  String get _cover => (_info['cover'] ?? _info['coverUrl'] ?? '').toString();
  String get _author => (_info['author'] ?? '').toString();
  String get _desc => (_info['description'] ?? '').toString();
  List<String> get _tags {
    final t = _info['tags'];
    if (t is List) return t.map((e) => e.toString()).toList();
    if (t is String) return t.split(',').map((e) => e.trim()).where((e) => e.isNotEmpty).toList();
    return [];
  }
  String get _referer => SourceDataService.getReferer(_cover, SourceDataService.instance.baseUrl(widget.sourceId));

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: DS.bg,
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: DS.accent, strokeWidth: 2.5))
          : _error != null
              ? _buildError()
              : _buildBody(),
    );
  }

  Widget _buildError() {
    return Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
      Icon(Icons.cloud_off_rounded, size: 48, color: DS.textDisabled),
      const SizedBox(height: 16),
      Padding(padding: const EdgeInsets.symmetric(horizontal: 32),
        child: Text(_error!, textAlign: TextAlign.center, style: const TextStyle(color: DS.textSecondary, fontSize: 14))),
      const SizedBox(height: 16),
      FilledButton(onPressed: _load, child: const Text('重试')),
    ]));
  }

  Widget _buildBody() {
    return CustomScrollView(
      physics: const BouncingScrollPhysics(),
      slivers: [
        // ── 电影感 Hero ──
        SliverAppBar(
          expandedHeight: 340, pinned: true, stretch: true,
          backgroundColor: DS.bg, elevation: 0,
          leading: _circleBtn(Icons.arrow_back_ios_new_rounded, () => context.pop()),
          actions: [
            _circleBtn(Icons.favorite_rounded, _toggleFav, active: _isFav),
          ],
          flexibleSpace: FlexibleSpaceBar(
            background: Stack(fit: StackFit.expand, children: [
              // 封面大图背景
              if (_cover.isNotEmpty)
                CachedNetworkImage(imageUrl: _cover, fit: BoxFit.cover,
                  httpHeaders: {'Referer': _referer},
                  errorWidget: (_, __, ___) => Container(color: DS.surface1))
              else
                Container(color: DS.surface1),
              // 居中半透明书图标
              Center(child: Icon(Icons.menu_book_rounded, size: 140, color: Colors.white.withValues(alpha: 0.04))),
              // 底部渐隐
              const Positioned(left: 0, right: 0, bottom: 0, height: 200,
                child: DecoratedBox(decoration: BoxDecoration(gradient: DS.heroScrim))),
              // 标题信息
              Positioned(left: DS.sp16, right: DS.sp16, bottom: DS.sp16,
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  if (_author.isNotEmpty)
                    Padding(padding: const EdgeInsets.only(bottom: 4),
                      child: Text(_author, style: DS.bodySec)),
                  Text(_title, style: DS.display),
                  const SizedBox(height: 6),
                  Text(_sourceLabel, style: const TextStyle(fontSize: 12, color: DS.textTertiary)),
                ])),
            ]),
          ),
        ),

        // ── 标签 + 操作 + 简介 ──
        SliverToBoxAdapter(child: Padding(
          padding: const EdgeInsets.fromLTRB(DS.sp16, DS.sp12, DS.sp16, 0),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            // 标签
            if (_tags.isNotEmpty)
              Wrap(spacing: 6, runSpacing: 6, children: _tags.take(6).map((t) => Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                decoration: BoxDecoration(color: DS.glassFill, borderRadius: BorderRadius.circular(DS.rSm)),
                child: Text(t, style: const TextStyle(fontSize: 12, color: DS.textSecondary)),
              )).toList()),
            const SizedBox(height: DS.sp16),

            // 操作区
            Row(children: [
              if (_chapters.isNotEmpty)
                Expanded(child: SpringButton(
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  onPressed: () => _openReader(0),
                  child: const Row(mainAxisSize: MainAxisSize.min, children: [
                    Icon(Icons.play_arrow_rounded, size: 22, color: Colors.white),
                    SizedBox(width: 6), Text('开始阅读'),
                  ]),
                ))
              else
                Expanded(child: Container(
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  decoration: BoxDecoration(color: DS.surface2, borderRadius: BorderRadius.circular(DS.rMd)),
                  child: const Center(child: Text('暂无章节', style: TextStyle(color: DS.textTertiary, fontSize: 14))),
                )),
              const SizedBox(width: DS.sp12),
              _roundAction(icon: _isFav ? Icons.favorite_rounded : Icons.favorite_border_rounded, active: _isFav, onTap: _toggleFav),
              const SizedBox(width: DS.sp8),
              _roundAction(icon: Icons.download_outlined, onTap: () {}),
            ]),

            // 简介
            if (_desc.isNotEmpty) ...[
              const SizedBox(height: DS.sp20),
              GestureDetector(
                onTap: () => setState(() => _showFullDesc = !_showFullDesc),
                child: Text(_desc, style: DS.bodySec,
                  maxLines: _showFullDesc ? null : 4,
                  overflow: _showFullDesc ? null : TextOverflow.ellipsis),
              ),
              if (!_showFullDesc && _desc.length > 100)
                GestureDetector(onTap: () => setState(() => _showFullDesc = true),
                  child: const Padding(padding: EdgeInsets.only(top: 6),
                    child: Text('展开全部', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: DS.accent)))),
            ],
          ]),
        )),

        // ── 章节列表 ──
        if (_chapters.isNotEmpty) ...[
          SliverToBoxAdapter(child: Padding(
            padding: const EdgeInsets.fromLTRB(DS.sp16, DS.sp24, DS.sp16, DS.sp12),
            child: Row(children: [
              const Text('章节', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: DS.textPrimary)),
              const SizedBox(width: 8),
              Text('${_chapters.length} 话', style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: DS.accent)),
              const Spacer(),
              GestureDetector(
                onTap: () { HapticFeedback.selectionClick(); setState(() => _descending = !_descending); },
                child: Row(children: [
                  Text(_descending ? '倒序' : '正序', style: const TextStyle(fontSize: 13, color: DS.textTertiary)),
                  Icon(_descending ? Icons.arrow_downward_rounded : Icons.arrow_upward_rounded, size: 15, color: DS.textTertiary),
                ]),
              ),
            ]),
          )),
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(DS.sp16, 0, DS.sp16, 100),
            sliver: SliverGrid(
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 2, childAspectRatio: 3.4, crossAxisSpacing: DS.sp8, mainAxisSpacing: DS.sp8),
              delegate: SliverChildBuilderDelegate((_, i) {
                final idx = _descending ? i : _chapters.length - 1 - i;
                final ch = _chapters[idx];
                final chId = (ch is Map ? (ch['id'] ?? ch['episode'] ?? '') : ch).toString();
                final chTitle = (ch is Map ? (ch['title'] ?? ch['name'] ?? '第${idx + 1}话') : ch).toString();
                return GestureDetector(
                  onTap: () { HapticFeedback.selectionClick(); if (chId.isNotEmpty) _openReader(idx); },
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: DS.sp12),
                    decoration: BoxDecoration(
                      color: i == 0 ? DS.surface2 : DS.surface1,
                      borderRadius: BorderRadius.circular(DS.rMd),
                      border: Border.all(color: i == 0 ? DS.accent.withValues(alpha: 0.4) : Colors.transparent, width: 1),
                    ),
                    child: Row(children: [
                      Expanded(child: Text(chTitle, maxLines: 1, overflow: TextOverflow.ellipsis,
                        style: TextStyle(fontSize: 13, fontWeight: FontWeight.w500, color: i == 0 ? DS.textPrimary : DS.textSecondary))),
                      Icon(Icons.chevron_right_rounded, size: 16, color: DS.textTertiary),
                    ]),
                  ),
                );
              }, childCount: _chapters.length),
            ),
          ),
        ] else ...[
          SliverToBoxAdapter(child: Padding(
            padding: const EdgeInsets.all(DS.sp32),
            child: Center(child: Column(children: [
              const Icon(Icons.inbox_rounded, size: 40, color: DS.textDisabled),
              const SizedBox(height: 12),
              const Text('暂无章节', style: TextStyle(color: DS.textTertiary, fontSize: 14)),
              const Text('该源可能需要登录或 VIP', style: TextStyle(color: DS.textDisabled, fontSize: 12)),
            ])),
          )),
        ],
      ],
    );
  }

  void _openReader(int index) {
    Navigator.of(context).push(MaterialPageRoute(builder: (_) => SourceReaderPage(
      sourceId: widget.sourceId, comicId: widget.comicId, comicTitle: _title,
      chapters: _chapters.map((c) => c is Map ? Map<String, dynamic>.from(c) : {'id': c.toString(), 'title': c.toString()}).toList(),
      initialChapter: index,
    )));
  }

  Widget _roundAction({required IconData icon, bool active = false, VoidCallback? onTap}) {
    return GestureDetector(
      onTap: onTap,
      child: Container(width: 50, height: 50,
        decoration: BoxDecoration(
          color: active ? DS.accent.withValues(alpha: 0.15) : DS.surface2,
          borderRadius: BorderRadius.circular(DS.rMd),
          border: Border.all(color: active ? DS.accent.withValues(alpha: 0.4) : Colors.transparent, width: 1),
        ),
        child: Icon(icon, size: 22, color: active ? DS.accent : DS.textSecondary)),
    );
  }

  Widget _circleBtn(IconData icon, VoidCallback onTap, {bool active = false}) {
    return Padding(padding: const EdgeInsets.all(6), child: GestureDetector(
      onTap: onTap,
      child: Container(width: 36, height: 36,
        decoration: BoxDecoration(color: Colors.black.withValues(alpha: 0.35), shape: BoxShape.circle),
        child: Icon(icon, size: 18, color: active ? DS.accent : Colors.white)),
    ));
  }
}