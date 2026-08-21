import 'package:flutter/material.dart';
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
    final status = (_info['status'] ?? '').toString();

    return Scaffold(
      backgroundColor: AppTheme.background,
      body: _loading
        ? const LoadingState(text: '加载中...')
        : _error != null
          ? EmptyState(icon: Icons.cloud_off_rounded, title: '加载失败', subtitle: _error, actionLabel: '重试', onAction: _load)
          : CustomScrollView(
              physics: const BouncingScrollPhysics(),
              slivers: [
                // Hero 封面 + 模糊背景
                SliverAppBar(
                  expandedHeight: 320,
                  pinned: true,
                  backgroundColor: Colors.transparent,
                  leading: GestureDetector(
                    onTap: () => context.pop(),
                    child: Container(margin: const EdgeInsets.all(8), decoration: BoxDecoration(color: AppTheme.glassFillLight, shape: BoxShape.circle), child: const Icon(Icons.arrow_back_ios_new_rounded, size: 18, color: AppTheme.textPrimary)),
                  ),
                  flexibleSpace: FlexibleSpaceBar(
                    background: Stack(
                      fit: StackFit.expand,
                      children: [
                        // 模糊背景（封面放大）
                        if (cover.isNotEmpty) CachedNetworkImage(imageUrl: cover, fit: BoxFit.cover, errorWidget: (_, __, ___) => Container(color: AppTheme.surface)),
                        // 模糊层
                        BackdropFilter(filter: ImageFilter.blur(sigmaX: 30, sigmaY: 30), child: Container(color: Colors.black.withValues(alpha: 0.6))),
                        // 渐变
                        Container(decoration: const BoxDecoration(gradient: AppTheme.heroScrim)),
                        // 封面 + 标题
                        Padding(
                          padding: const EdgeInsets.fromLTRB(20, 60, 20, 20),
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.end,
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
                                // 封面
                                ClipRRect(
                                  borderRadius: BorderRadius.circular(AppTheme.radiusMd),
                                  child: SizedBox(
                                    width: 120, height: 168,
                                    child: cover.isNotEmpty
                                      ? CachedNetworkImage(imageUrl: cover, fit: BoxFit.cover, errorWidget: (_, __, ___) => Container(color: AppTheme.surface))
                                      : Container(color: AppTheme.surface, child: const Icon(Icons.menu_book_rounded, size: 40, color: AppTheme.textTertiary)),
                                  ),
                                ),
                                const SizedBox(width: 14),
                                Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                  Text(title, maxLines: 3, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800, color: AppTheme.textPrimary, letterSpacing: -0.3)),
                                  if (author.isNotEmpty) ...[const SizedBox(height: 6), Text(author, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 13, color: AppTheme.textSecondary))],
                                  if (status.isNotEmpty) ...[const SizedBox(height: 4), Container(padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2), decoration: BoxDecoration(color: AppTheme.accent.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(6)), child: Text(status, style: const TextStyle(fontSize: 11, color: AppTheme.accent, fontWeight: FontWeight.w500)))],
                                ])),
                              ]),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                ),

                // 简介
                if (desc.isNotEmpty)
                  SliverToBoxAdapter(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: LiquidGlass(
                        radius: BorderRadius.circular(AppTheme.radiusMd),
                        padding: const EdgeInsets.all(14),
                        fillColor: AppTheme.glassFillRegular,
                        child: Text(desc, style: const TextStyle(fontSize: 13, color: AppTheme.textSecondary, height: 1.6)),
                      ),
                    ),
                  ),

                // 章节列表
                SliverToBoxAdapter(
                  child: SectionHeader(title: '章节', subtitle: '${_chapters.length}话'),
                ),
                SliverPadding(
                  padding: const EdgeInsets.fromLTRB(16, 4, 16, 100),
                  sliver: SliverList(
                    delegate: SliverChildBuilderDelegate(
                      (ctx, i) {
                        final ch = _chapters[i];
                        final chId = (ch is Map ? (ch['id'] ?? ch['episode'] ?? '') : ch).toString();
                        final chTitle = (ch is Map ? (ch['title'] ?? ch['name'] ?? ch['episode'] ?? '第${i+1}话') : ch).toString();
                        final isOdd = i % 2 == 0;
                        return GestureDetector(
                          onTap: () {
                            HapticFeedback.selectionClick();
                            GoRouter.of(context).push('/source/${widget.sourceId}/reader/${widget.comicId}/$chId');
                          },
                          child: Container(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
                            decoration: BoxDecoration(
                              color: isOdd ? AppTheme.surface : AppTheme.surfaceLight,
                              borderRadius: BorderRadius.circular(AppTheme.radiusSm),
                            ),
                            child: Row(children: [
                              Container(width: 28, height: 28, decoration: BoxDecoration(color: AppTheme.primary.withValues(alpha: 0.1), borderRadius: BorderRadius.circular(8)), child: Center(child: Text('${i+1}', style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: AppTheme.primary)))),
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
