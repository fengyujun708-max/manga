import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:get_it/get_it.dart';
import '../../../app/theme/theme.dart';
import '../../../core/network/api_client.dart';

/// 源内阅读页 — 从服务器代理获取图片 URL 列表，客户端直连源 CDN 加载
class SourceReaderPage extends StatefulWidget {
  final String sourceId;
  final String comicId;
  final String epId;
  const SourceReaderPage({super.key, required this.sourceId, required this.comicId, required this.epId});

  @override
  State<SourceReaderPage> createState() => _SourceReaderPageState();
}

class _SourceReaderPageState extends State<SourceReaderPage> {
  bool _loading = true;
  String? _error;
  List<String> _images = [];
  final _pageCtrl = PageController();

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _pageCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final api = GetIt.instance<ApiClient>();
      final result = await SourceDataService.instance.pages(widget.sourceId, widget.comicId, widget.epId);
      if (result['pages'] is List) {
        final pages = result['pages'] as List;
        setState(() {
          _pages = pages.map((p) {
            if (p is Map) return p['url']?.toString() ?? '';
            return p.toString();
          }).toList();
          _hasMore = (result['next'] ?? '').isNotEmpty;
          _loading = false;
          _images = _pages;
        });
        return;
      }
      final res = await api.get('/source/${widget.sourceId}/pages',
          params: {'comicId': widget.comicId, 'epId': widget.epId});
      final data = res.data;
      if (data is Map && data['images'] is List) {
        setState(() {
          _images = (data['images'] as List).map((e) => e.toString()).where((s) => s.isNotEmpty).toList();
          _loading = false;
        });
      } else {
        setState(() { _loading = false; _error = '章节无图片'; });
      }
    } catch (e) {
      setState(() { _loading = false; _error = '加载失败: $e'; });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        foregroundColor: Colors.white,
        elevation: 0,
        title: Text('阅读中 (${_images.length}页)', style: const TextStyle(fontSize: 14)),
        leading: IconButton(icon: const Icon(Icons.arrow_back_ios_new_rounded), onPressed: () => context.pop()),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: AppTheme.primary, strokeWidth: 2))
          : _error != null
              ? Center(
                  child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                    Text(_error!, style: const TextStyle(color: Colors.white70, fontSize: 13)),
                    const SizedBox(height: 16),
                    ElevatedButton(onPressed: _load, style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primary),
                      child: const Text('重试', style: TextStyle(color: Colors.white))),
                  ]),
                )
              : _images.isEmpty
                  ? const Center(child: Text('无图片', style: TextStyle(color: Colors.white70)))
                  : PageView.builder(
                      controller: _pageCtrl,
                      itemCount: _images.length,
                      itemBuilder: (_, i) => InteractiveViewer(
                        maxScale: 4,
                        child: Center(
                          child: Image.network(
                            _images[i],
                            fit: BoxFit.contain,
                            loadingBuilder: (ctx, child, progress) => progress == null
                                ? child
                                : const Center(child: CircularProgressIndicator(color: AppTheme.primary, strokeWidth: 2)),
                            errorBuilder: (_, __, ___) => const Center(
                              child: Text('图片加载失败', style: TextStyle(color: Colors.white54, fontSize: 12)),
                            ),
                          ),
                        ),
                      ),
                    ),
    );
  }
}