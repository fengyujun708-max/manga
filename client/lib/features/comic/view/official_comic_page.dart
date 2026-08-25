import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:get_it/get_it.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../../../app/ds.dart';
import '../../../app/config/app_config.dart';
import '../../../core/network/api_client.dart';
import '../../../core/services/library_service.dart';
import 'official_reader_page.dart';

/// 官方漫画详情（official_series / Webtoon 等官方源）
/// 数据：GET /v1/official/series/:id（含 episodes）
class OfficialComicPage extends StatefulWidget {
  final String seriesId;
  const OfficialComicPage({super.key, required this.seriesId});
  @override
  State<OfficialComicPage> createState() => _OfficialComicPageState();
}

class _OfficialComicPageState extends State<OfficialComicPage> {
  Map<String, dynamic>? _series;
  List<Map<String, dynamic>> _episodes = [];
  bool _loading = true;
  String? _error;
  bool _descending = true;
  bool _isFav = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/official/series/${widget.seriesId}');
      if (res.data is Map) {
        final data = Map<String, dynamic>.from(res.data as Map);
        final eps = (data['episodes'] as List?)?.map((e) => Map<String, dynamic>.from(e as Map)).toList() ?? [];
        if (mounted) {
          setState(() {
            _series = data;
            _episodes = eps;
            _loading = false;
          });
        }
        _checkFav();
      } else {
        throw Exception('empty');
      }
    } catch (e) {
      if (mounted) setState(() { _loading = false; _error = '加载失败：$e'; });
    }
  }

  Future<void> _checkFav() async {
    final fav = await LibraryService.instance.isFavorited('webtoon', widget.seriesId);
    if (mounted) setState(() => _isFav = fav);
  }

  Future<void> _toggleFav() async {
    HapticFeedback.mediumImpact();
    await LibraryService.instance.toggleFavorite({
      'sourceId': 'webtoon',
      'sourceName': 'Webtoon 官方',
      'comicId': widget.seriesId,
      'title': _series?['title'] ?? '',
      'cover': _series?['coverUrl'] ?? '',
    });
    _checkFav();
  }

  List<Map<String, dynamic>> get _displayEpisodes {
    final list = List<Map<String, dynamic>>.from(_episodes);
    if (_descending) list.sort((a, b) => (b['epNumber'] ?? 0).compareTo(a['epNumber'] ?? 0));
    return list;
  }

  String _cover() {
    final c = _series?['coverUrl']?.toString() ?? '';
    if (c.startsWith('/')) {
      final base = AppConfig.apiBaseUrl.replaceAll(RegExp(r'/v1$'), '');
      return '$base$c';
    }
    return c;
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Scaffold(backgroundColor: DS.bg, body: Center(child: CircularProgressIndicator(color: DS.accent)));
    }
    if (_error != null || _series == null) {
      return Scaffold(
        backgroundColor: DS.bg,
        appBar: AppBar(backgroundColor: DS.bg, elevation: 0, leading: IconButton(icon: const Icon(Icons.arrow_back_ios_new_rounded, size: 20), onPressed: () => context.pop())),
        body: Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
          const Icon(Icons.cloud_off_rounded, size: 44, color: DS.textDisabled),
          const SizedBox(height: 12),
          Text(_error ?? '内容不存在', style: const TextStyle(color: DS.textSecondary)),
          const SizedBox(height: 16),
          FilledButton(onPressed: _load, child: const Text('重试')),
        ])),
      );
    }

    final s = _series!;
    final cover = _cover();
    final genres = (s['genres'] as List?)?.map((e) => e.toString()).toList() ?? <String>[];

    return Scaffold(
      backgroundColor: DS.bg,
      body: CustomScrollView(
        physics: const BouncingScrollPhysics(),
        slivers: [
          SliverAppBar(
            expandedHeight: 320,
            pinned: true,
            backgroundColor: DS.bg,
            elevation: 0,
            leading: _circleBtn(Icons.arrow_back_ios_new_rounded, () => context.pop()),
            actions: [_circleBtn(_isFav ? Icons.favorite_rounded : Icons.favorite_border_rounded, _toggleFav, active: _isFav)],
            flexibleSpace: FlexibleSpaceBar(
              background: Stack(fit: StackFit.expand, children: [
                if (cover.isNotEmpty)
                  CachedNetworkImage(imageUrl: cover, fit: BoxFit.cover, errorWidget: (_, __, ___) => Container(color: DS.surface1))
                else
                  Container(color: DS.surface1),
                const DecoratedBox(decoration: BoxDecoration(gradient: DS.heroScrim)),
                Positioned(left: DS.sp16, right: DS.sp16, bottom: DS.sp16, child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(color: DS.accent, borderRadius: BorderRadius.circular(DS.rSm)),
                    child: const Text('官方', style: TextStyle(fontSize: 10, fontWeight: FontWeight.w700, color: Colors.white)),
                  ),
                  const SizedBox(height: 8),
                  Text(s['title'] ?? '', style: DS.display),
                  const SizedBox(height: 4),
                  Text(s['author'] ?? s['artist'] ?? '未知作者', style: DS.bodySec),
                ])),
              ]),
            ),
          ),
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(DS.sp16, DS.sp8, DS.sp16, 0),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                if (genres.isNotEmpty) ...[
                  Wrap(spacing: 6, runSpacing: 6, children: genres.map((g) => Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                    decoration: BoxDecoration(color: DS.glassFill, borderRadius: BorderRadius.circular(DS.rSm)),
                    child: Text(g, style: const TextStyle(fontSize: 12, color: DS.textSecondary)),
                  )).toList()),
                  const SizedBox(height: DS.sp16),
                ],
                if ((s['description'] ?? '').toString().isNotEmpty) ...[
                  Text((s['description'] ?? '').toString(), style: const TextStyle(fontSize: 13, color: DS.textSecondary, height: 1.6)),
                  const SizedBox(height: DS.sp16),
                ],
                Row(children: [
                  const Text('章节', style: DS.title),
                  const Spacer(),
                  Text('${_episodes.length} 话', style: const TextStyle(fontSize: 12, color: DS.textTertiary)),
                  IconButton(
                    icon: Icon(_descending ? Icons.arrow_downward_rounded : Icons.arrow_upward_rounded, size: 16, color: DS.textTertiary),
                    onPressed: () => setState(() => _descending = !_descending),
                  ),
                ]),
              ]),
            ),
          ),
          if (_episodes.isEmpty)
            const SliverToBoxAdapter(child: SizedBox(height: 200, child: Center(child: Text('暂无章节', style: TextStyle(color: DS.textTertiary)))))
          else
            SliverList.builder(
              itemCount: _displayEpisodes.length,
              itemBuilder: (_, i) {
                final ep = _displayEpisodes[i];
                final isFree = (ep['availability'] ?? 'AVAILABLE').toString() == 'FREE';
                return ListTile(
                  onTap: () => GoRouter.of(context).push(
                    '/official/${widget.seriesId}/reader/${ep['id']}?title=${Uri.encodeComponent(ep['title']?.toString() ?? '')}&series=${Uri.encodeComponent(s['title']?.toString() ?? '')}',
                  ),
                  title: Text(ep['title'] ?? '第 ${ep['epNumber']} 话', maxLines: 1, overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: DS.textPrimary)),
                  subtitle: Text(ep['publishedAt'] != null ? ep['publishedAt'].toString().substring(0, 10) : '', style: const TextStyle(fontSize: 11, color: DS.textTertiary)),
                  trailing: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: isFree ? const Color(0xFF34D399).withValues(alpha: 0.12) : DS.surface2,
                      borderRadius: BorderRadius.circular(999),
                    ),
                    child: Text(isFree ? '免费' : '付费', style: TextStyle(fontSize: 10, fontWeight: FontWeight.w600, color: isFree ? const Color(0xFF34D399) : DS.textTertiary)),
                  ),
                );
              },
            ),
          const SliverToBoxAdapter(child: SizedBox(height: 80)),
        ],
      ),
    );
  }

  Widget _circleBtn(IconData icon, VoidCallback onTap, {bool active = false}) {
    return Padding(padding: const EdgeInsets.all(6), child: GestureDetector(
      onTap: onTap,
      child: Container(
        width: 36,
        height: 36,
        decoration: BoxDecoration(color: Colors.black.withValues(alpha: 0.35), shape: BoxShape.circle),
        child: Icon(icon, size: 18, color: active ? DS.accent : Colors.white),
      ),
    ));
  }
}