import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:get_it/get_it.dart';
import '../../../app/ds.dart';
import '../../../plugins/source_data_service.dart';
import '../../../core/services/library_service.dart';

/// 源内阅读页 — 本地 QuickJS 引擎加载图片
class SourceReaderPage extends StatefulWidget {
  final String sourceId;
  final String comicId;
  final String comicTitle;
  final List<Map<String, dynamic>> chapters; // [{id, title}]
  final int initialChapter;

  const SourceReaderPage({
    super.key,
    required this.sourceId,
    required this.comicId,
    required this.comicTitle,
    required this.chapters,
    this.initialChapter = 0,
  });

  @override
  State<SourceReaderPage> createState() => _SourceReaderPageState();
}

enum ReaderMode { vertical, rightLeft }

class _SourceReaderPageState extends State<SourceReaderPage> {
  bool _loading = true;
  String? _error;
  List<String> _images = [];
  int _chapterIndex = 0;
  double _brightness = 1.0;
  ReaderMode _mode = ReaderMode.vertical;
  bool _showOverlay = false;
  final _scrollCtrl = ScrollController();
  final _pageCtrl = PageController();

  @override
  void initState() {
    super.initState();
    _chapterIndex = widget.initialChapter;
    _scrollCtrl.addListener(_onScroll);
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
    _load();
    _restoreProgress();
  }

  @override
  void dispose() {
    _scrollCtrl.dispose();
    _pageCtrl.dispose();
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
    super.dispose();
  }

  void _onScroll() {
    // 底部自动加载下一章
    if (_scrollCtrl.position.pixels >= _scrollCtrl.position.maxScrollExtent - 200) {
      _nextChapter();
    }
  }

  Future<void> _restoreProgress() async {
    try {
      final p = await LibraryService.instance.getProgress(widget.sourceId, widget.comicId);
      if (p != null && mounted) setState(() => _chapterIndex = p['chapterIndex'] ?? 0);
    } catch (_) {}
  }

  Future<void> _load() async {
    if (widget.chapters.isEmpty) { setState(() { _loading = false; _error = '无章节'; }); return; }
    setState(() { _loading = true; _error = null; });
    try {
      final svc = SourceDataService.instance;
      final chId = widget.chapters[_chapterIndex]['id'] ?? '';
      final res = await svc.pages(widget.sourceId, widget.comicId, chId.toString());
      final pages = res['pages'] as List? ?? [];
      final images = pages.map((e) => e.toString()).where((e) => e.startsWith('http')).toList();
      if (!mounted) return;
      setState(() { _images = images; _loading = false; });
      LibraryService.instance.recordRead(
        sourceId: widget.sourceId, comicId: widget.comicId, title: widget.comicTitle,
        cover: '', chapterIndex: _chapterIndex, chapterTitle: widget.chapters[_chapterIndex]['title'] ?? '',
      );
    } catch (e) {
      if (!mounted) return;
      setState(() { _loading = false; _error = e.toString(); });
    }
  }

  void _prevChapter() { if (_chapterIndex > 0) { setState(() { _chapterIndex--; _images.clear(); }); _load(); } }
  void _nextChapter() { if (_chapterIndex < widget.chapters.length - 1) { setState(() { _chapterIndex++; _images.clear(); }); _load(); } }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(children: [
        // ===== 内容区 =====
        _buildContent(),
        // ===== 顶部/底部控制条 =====
        if (_showOverlay) ..._buildOverlay(),
        // ===== 点击区域 =====
        GestureDetector(onTap: () => setState(() => _showOverlay = !_showOverlay), behavior: HitTestBehavior.translucent),
      ]),
    );
  }

  Widget _buildContent() {
    if (_loading) return const Center(child: CircularProgressIndicator(color: DS.accent));
    if (_error != null) return Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
      Text('加载失败', style: TextStyle(color: Colors.white70)),
      SizedBox(height: 8),
      Text(_error!, style: TextStyle(color: Colors.white38, fontSize: 12)),
      SizedBox(height: 16),
      FilledButton(onPressed: _load, child: Text('重试')),
    ]));
    if (_images.isEmpty) return Center(child: Text('本章无内容', style: TextStyle(color: Colors.white54)));

    return _mode == ReaderMode.vertical ? _buildVertical() : _buildPageMode();
  }

  Widget _buildVertical() {
    return ListView.builder(
      controller: _scrollCtrl,
      itemCount: _images.length + 2, // 头尾章节导航
      itemBuilder: (_, i) {
        if (i == 0) return _chapterHeader();
        if (i == _images.length + 1) return _chapterFooter();
        return CachedNetworkImage(
          imageUrl: _images[i - 1],
          fit: BoxFit.fitWidth,
          httpHeaders: {'Referer': Uri.tryParse(_images[i - 1])?.host != null ? 'https://${Uri.parse(_images[i - 1]).host}/' : ''},
          placeholder: (_, __) => Container(height: 400, color: DS.surface1, child: Center(child: CircularProgressIndicator(strokeWidth: 2, color: DS.accent))),
          errorWidget: (_, __, ___) => Container(height: 200, color: DS.surface1, child: Icon(Icons.broken_image_rounded, color: Colors.white24)),
        );
      },
    );
  }

  Widget _buildPageMode() {
    return PageView.builder(
      controller: _pageCtrl,
      reverse: true,
      itemCount: _images.length,
      itemBuilder: (_, i) => InteractiveViewer(maxScale: 3.0, child: Center(
        child: CachedNetworkImage(imageUrl: _images[i], fit: BoxFit.contain,
          httpHeaders: {'Referer': 'https://${Uri.parse(_images[i]).host}/'},
          placeholder: (_, __) => CircularProgressIndicator(strokeWidth: 2, color: DS.accent),
          errorWidget: (_, __, ___) => Icon(Icons.broken_image_rounded, color: Colors.white24),
        ),
      )),
    );
  }

  Widget _chapterHeader() => Container(padding: EdgeInsets.all(DS.sp16), color: DS.surface1,
    child: Text(widget.chapters[_chapterIndex]['title'] ?? '', style: TextStyle(color: Colors.white70, fontSize: 14)));

  Widget _chapterFooter() => Padding(padding: EdgeInsets.all(DS.sp32),
    child: Center(child: _chapterIndex < widget.chapters.length - 1
      ? FilledButton(onPressed: _nextChapter, child: Text('下一章'))
      : Text('已读完', style: TextStyle(color: Colors.white38))));

  List<Widget> _buildOverlay() => [
    // 顶部
    Positioned(top: 0, left: 0, right: 0, child: SafeArea(child: Container(
      padding: EdgeInsets.symmetric(horizontal: DS.sp12, vertical: DS.sp8),
      decoration: BoxDecoration(gradient: LinearGradient(colors: [Colors.black87, Colors.transparent])),
      child: Row(children: [
        IconButton(icon: Icon(Icons.arrow_back_ios_rounded, color: Colors.white), onPressed: () => context.pop()),
        Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(widget.comicTitle, style: TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.w600)),
          if (widget.chapters.isNotEmpty)
            Text(widget.chapters[_chapterIndex]['title'] ?? '', style: TextStyle(color: Colors.white54, fontSize: 12)),
        ])),
      ]),
    ))),
    // 底部
    Positioned(bottom: 0, left: 0, right: 0, child: SafeArea(child: Container(
      padding: EdgeInsets.all(DS.sp12),
      decoration: BoxDecoration(gradient: LinearGradient(begin: Alignment.topCenter, colors: [Colors.transparent, Colors.black87])),
      child: Column(children: [
        // 章节进度
        Row(children: [
          IconButton(icon: Icon(Icons.skip_previous_rounded, color: _chapterIndex > 0 ? Colors.white : Colors.white24), onPressed: _prevChapter),
          Expanded(child: Slider(value: _chapterIndex.toDouble(), min: 0, max: (widget.chapters.length - 1).toDouble(), divisions: widget.chapters.length,
            activeColor: DS.accent, label: '${_chapterIndex + 1}/${widget.chapters.length}',
            onChanged: (v) => setState(() => _chapterIndex = v.round()))),
          IconButton(icon: Icon(Icons.skip_next_rounded, color: _chapterIndex < widget.chapters.length - 1 ? Colors.white : Colors.white24), onPressed: _nextChapter),
        ]),
        // 工具栏
        Row(mainAxisAlignment: MainAxisAlignment.spaceEvenly, children: [
          IconButton(icon: Icon(_mode == ReaderMode.vertical ? Icons.swap_vert_rounded : Icons.swipe_left_rounded, color: Colors.white),
            onPressed: () => setState(() => _mode = _mode == ReaderMode.vertical ? ReaderMode.rightLeft : ReaderMode.vertical)),
          IconButton(icon: Icon(Icons.brightness_6_rounded, color: Colors.white),
            onPressed: () => showModalBottomSheet(context: context, backgroundColor: DS.surface2, builder: (_) => _brightnessSheet())),
        ]),
      ]),
    ))),
  ];

  Widget _brightnessSheet() => SafeArea(child: Padding(padding: EdgeInsets.all(DS.sp20), child: Column(mainAxisSize: MainAxisSize.min, children: [
    Text('亮度', style: TextStyle(color: DS.textPrimary, fontWeight: FontWeight.w600)),
    Slider(value: _brightness, min: 0.2, max: 1.0, activeColor: DS.accent,
      onChanged: (v) { setState(() => _brightness = v); SystemChrome.setSystemUIOverlayStyle(SystemUiOverlayStyle()); }),
  ])));
}