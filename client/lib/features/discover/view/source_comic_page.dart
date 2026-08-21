import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'dart:ui';
import 'package:get_it/get_it.dart';
import '../../../app/theme/theme.dart';
import '../../../core/network/api_client.dart';
import '../../../plugins/source_data_service.dart';
import '../../../app/widgets/comic_widgets.dart';

/// 源内漫画详情页 — 从服务器代理执行源 JS 获取真实详情 + 章节
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

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    final result = await SourceDataService.instance.comic(widget.sourceId, widget.comicId);
    if (!mounted) return;
    final data = result['detail'] as Map<String, dynamic>? ?? {};
    setState(() {
      _info = data;
      final rawChapters = result['chapters'] ?? _extractChapters(data);
      _chapters = (rawChapters as List?) ?? [];
      _loading = false;
      if (result['error'] != null) _error = result['error'];
    });
  }

  List<dynamic> _extractChapters(Map<String, dynamic> data) {
    for (final key in ['chapters', 'episodes', 'comics']) {
      final v = data[key];
      if (v is List) return v;
    }
    return [];
  }

  @override
  Widget build(BuildContext context) {
    final cover = (_info['cover'] ?? _info['coverUrl'] ?? '').toString();
    final title = (_info['title'] ?? _info['name'] ?? '').toString();
    final author = (_info['author'] ?? _info['subTitle'] ?? '').toString();
    final desc = (_info['description'] ?? '').toString();

    return Scaffold(
      backgroundColor: AppTheme.background,
      body: _loading
        ? const Center(child: CircularProgressIndicator(color: AppTheme.primary))
        : _error != null
          ? Center(child: Text(_error!, style: const TextStyle(color: AppTheme.textSecondary)))
          : CustomScrollView(
              physics: const BouncingScrollPhysics(),
              slivers: [
                SliverAppBar(
                  expandedHeight: 320,
                  pinned: true,
                  backgroundColor: Colors.transparent,
                  leading: GestureDetector(
                    onTap: () => context.pop(),
                    child: Container(margin: const EdgeInsets.all(8), decoration: const BoxDecoration(color: Color(0x14FFFFFF), shape: BoxShape.circle), child: const Icon(Icons.arrow_back_ios_new_rounded, size: 18, color: AppTheme.textPrimary)),
                  ),
                  flexibleSpace: FlexibleSpaceBar(background: Container(color: AppTheme.surface)),
                ),
                if (desc.isNotEmpty)
                  SliverToBoxAdapter(child: Padding(padding: const EdgeInsets.all(16), child: Text(desc, style: const TextStyle(fontSize: 13, color: AppTheme.textSecondary, height: 1.6)))),
                SliverToBoxAdapter(child: Padding(padding: const EdgeInsets.fromLTRB(16, 8, 16, 4), child: Text('章节', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: AppTheme.textPrimary)))),
                SliverPadding(
                  padding: const EdgeInsets.fromLTRB(16, 4, 16, 100),
                  sliver: SliverList(
                    delegate: SliverChildBuilderDelegate(
                      (ctx, i) {
                        final ch = _chapters[i];
                        final chId = (ch is Map ? (ch['id'] ?? ch['episode'] ?? '') : ch).toString();
                        final chTitle = (ch is Map ? (ch['title'] ?? ch['name'] ?? ch['episode'] ?? '第${i+1}话') : ch).toString();
                        return GestureDetector(
                          onTap: () {
                            HapticFeedback.selectionClick();
                            GoRouter.of(context).push('/source/${widget.sourceId}/reader/${widget.comicId}/$chId');
                          },
                          child: Container(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
                            decoration: BoxDecoration(
                              color: i % 2 == 0 ? AppTheme.surface : AppTheme.surfaceLight,
                              borderRadius: BorderRadius.circular(AppTheme.radiusSm),
                            ),
                            child: Row(children: [
                              Container(width: 28, height: 28, decoration: BoxDecoration(color: AppTheme.primary.withValues(alpha: 0.1), borderRadius: BorderRadius.circular(8)), child: Center(child: Text('${i + 1}', style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: AppTheme.primary)))),
                              const SizedBox(width: 12),
                              Expanded(child: Text(chTitle, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 14, color: AppTheme.textPrimary))),
                              const Icon(Icons.chevron_right_rounded, size: 18, color: AppTheme.textTertiary),
                            ]),
                          ),
                        );
                      },
                      childCount: _chapters.length,
                    ),
                  ),
                ),
              ],
            ),
    );
  }
}
