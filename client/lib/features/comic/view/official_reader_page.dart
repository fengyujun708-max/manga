import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:get_it/get_it.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../../../app/ds.dart';
import '../../../core/network/api_client.dart';
import '../../../core/services/library_service.dart';

/// 官方章节阅读器（Webtoon 等官方源压缩图片直读）
/// 数据：GET /v1/webtoon/episode/:id/content → pages[]
class OfficialReaderPage extends StatefulWidget {
  final String seriesId;
  final String episodeId;
  final String seriesTitle;
  final String episodeTitle;
  const OfficialReaderPage({
    super.key,
    required this.seriesId,
    required this.episodeId,
    required this.seriesTitle,
    this.episodeTitle = '',
  });
  @override
  State<OfficialReaderPage> createState() => _OfficialReaderPageState();
}

class _OfficialReaderPageState extends State<OfficialReaderPage> {
  bool _loading = true;
  bool _ready = false;
  String? _error;
  List<String> _pages = [];
  bool _showOverlay = false;
  int _poll = 0;

  @override
  void initState() {
    super.initState();
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
    _load();
  }

  @override
  void dispose() {
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    super.dispose();
  }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/official/episode/${widget.episodeId}/content');
      final data = res.data is Map ? Map<String, dynamic>.from(res.data as Map) : <String, dynamic>{};
      final images = data['images'];
      if (images is List && images.isNotEmpty) {
        _pages = images.map((e) => e.toString()).where((e) => e.isNotEmpty).toList();
        _ready = true;
        _loading = false;
        LibraryService.instance.recordRead(
          sourceId: 'manjie_official',
          comicId: widget.seriesId,
          title: widget.seriesTitle,
          cover: '',
          chapterIndex: 0,
          chapterTitle: widget.episodeTitle,
        );
      } else {
        if (mounted) setState(() { _loading = false; _error = '本章暂无图片内容'; });
      }
    } catch (e) {
      if (mounted) setState(() { _loading = false; _error = e.toString(); });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(children: [
        _buildContent(),
        ...(_showOverlay ? _buildOverlay() : const <Widget>[]),
        GestureDetector(onTap: () => setState(() => _showOverlay = !_showOverlay), behavior: HitTestBehavior.translucent),
      ]),
    );
  }

  Widget _buildContent() {
    if (_loading) {
      return const Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
        CircularProgressIndicator(color: DS.accent),
        SizedBox(height: 12),
        Text('加载中…', style: TextStyle(color: Colors.white54, fontSize: 12)),
      ]));
    }
    if (_error != null) {
      return Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
        const Icon(Icons.cloud_off_rounded, size: 40, color: Colors.white24),
        const SizedBox(height: 12),
        Text(_error!, style: const TextStyle(color: Colors.white54, fontSize: 13)),
        const SizedBox(height: 16),
        FilledButton(onPressed: _load, child: const Text('重试')),
      ]));
    }
    if (_pages.isEmpty) {
      return const Center(child: Text('本章暂无内容', style: TextStyle(color: Colors.white54)));
    }
    // 竖条滚动看图（Webtoon 官方阅读习惯），支持双指缩放
    return InteractiveViewer(
      maxScale: 4,
      child: SingleChildScrollView(
        scrollDirection: Axis.vertical,
        physics: const BouncingScrollPhysics(),
        child: Column(
          children: _pages.map((url) => CachedNetworkImage(
            imageUrl: url,
            fit: BoxFit.fitWidth,
            placeholder: (_, __) => Container(height: 300, color: const Color(0xFF111111),
              child: const Center(child: CircularProgressIndicator(strokeWidth: 2, color: DS.accent))),
            errorWidget: (_, __, ___) => Container(height: 200, color: const Color(0xFF111111),
              child: const Icon(Icons.broken_image_rounded, color: Colors.white24)),
          )).toList(),
        ),
      ),
    );
  }

  List<Widget> _buildOverlay() => [
    Positioned(top: 0, left: 0, right: 0, child: SafeArea(child: Container(
      padding: const EdgeInsets.symmetric(horizontal: DS.sp12, vertical: DS.sp8),
      decoration: const BoxDecoration(gradient: LinearGradient(colors: [Colors.black87, Colors.transparent])),
      child: Row(children: [
        IconButton(icon: const Icon(Icons.arrow_back_ios_rounded, color: Colors.white), onPressed: () => context.pop()),
        Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(widget.seriesTitle, style: const TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.w600)),
          if (widget.episodeTitle.isNotEmpty)
            Text(widget.episodeTitle, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(color: Colors.white54, fontSize: 12)),
        ])),
      ]),
    ))),
    Positioned(bottom: 0, left: 0, right: 0, child: SafeArea(child: Container(
      padding: const EdgeInsets.all(DS.sp12),
      decoration: const BoxDecoration(gradient: LinearGradient(begin: Alignment.topCenter, colors: [Colors.transparent, Colors.black87])),
      child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
        Text('${_pages.length} 页', style: const TextStyle(color: Colors.white38, fontSize: 12)),
        const SizedBox(width: 16),
        IconButton(icon: const Icon(Icons.refresh_rounded, color: Colors.white70), onPressed: _load),
      ]),
    ))),
  ];
}