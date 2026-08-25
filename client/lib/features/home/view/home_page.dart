import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:get_it/get_it.dart';
import '../../../app/ds.dart';
import '../../../core/network/api_client.dart';
import '../../../plugins/source_data_service.dart';

/// 首页 — 聚合首页（漫界官方 + 已安装源实时聚合）
/// - 官方内容来自服务器元数据（用户后台上传）
/// - 源内容由本地 QuickJS 实时抓取，不落服务器
/// - 标题归一化去重（官方优先）
/// - 下拉刷新，实时更新
class HomePage extends StatefulWidget {
  const HomePage({super.key});
  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  Map<String, dynamic>? _feed; // 官方 /home
  List<Map<String, dynamic>> _srcCards = []; // 源聚合卡片
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  String _norm(String t) => t.toLowerCase().replaceAll(
      RegExp(r'[\s\-_.,:;!@#$%^&*()\[\]{}<>?/\\|`~·「」『』【】"'']+'), '');

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      // 并行：官方内容 + 各源 explore
      final results = await Future.wait([
        _fetchOfficial(),
        _fetchSourceCards(),
      ]);
      if (!mounted) return;
      setState(() {
        _feed = results[0] as Map<String, dynamic>?;
        _srcCards = (results[1] as List<Map<String, dynamic>>?) ?? [];
        _loading = false;
      });
    } catch (e) {
      if (mounted) setState(() { _loading = false; _error = e.toString(); });
    }
  }

  Future<Map<String, dynamic>?> _fetchOfficial() async {
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/home');
      return res.data is Map ? Map<String, dynamic>.from(res.data as Map) : null;
    } catch (_) {
      return null;
    }
  }

  /// 聚合已安装源 explore（本地 QuickJS 实时），标题去重
  Future<List<Map<String, dynamic>>?> _fetchSourceCards() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final raw = prefs.getString('installed_sources') ?? '[]';
      final manifests = (jsonDecode(raw) as List)
          .whereType<Map>()
          .map((e) => Map<String, dynamic>.from(e))
          .where((m) => (m['id']?.toString() ?? '').isNotEmpty)
          .take(8) // 最多 8 个源，控制首页加载时间
          .toList();

      final svc = SourceDataService.instance;
      final seen = <String>{};
      final cards = <Map<String, dynamic>>[];
      for (final m in manifests) {
        final sourceId = m['id']!.toString();
        final sourceName = m['name']?.toString() ?? sourceId;
        try {
          final r = await svc.explore(sourceId).timeout(const Duration(seconds: 12));
          final sections = (r['sections'] as List?) ?? const [];
          for (final sec in sections) {
            if (sec is! Map) continue;
            final items = (sec['items'] as List?) ?? const [];
            for (final item in items) {
              if (item is! Map) continue;
              final title = (item['title'] ?? item['name'] ?? '').toString();
              final cover = (item['cover'] ?? item['coverUrl'] ?? '').toString();
              final id = (item['id'] ?? '').toString();
              if (title.isEmpty || id.isEmpty) continue;
              final key = _norm(title);
              if (seen.contains(key)) continue; // 已存在同标题（官方或其它源）
              seen.add(key);
              cards.add({
                'id': id,
                'title': title,
                'cover': cover,
                'sourceId': sourceId,
                'sourceName': sourceName,
              });
            }
          }
        } catch (_) {} // 单个源失败不影响聚合
      }
      return cards;
    } catch (_) {
      return [];
    }
  }

  /// 官方卡片 + 源卡片合并去重（官方优先）
  List<Map<String, dynamic>> _mergedCards() {
    final seen = <String>{};
    final out = <Map<String, dynamic>>[];
    void add(Map<String, dynamic> c, String idKey) {
      final title = (c['title'] ?? '').toString();
      final key = _norm(title);
      if (key.isEmpty || seen.contains(key)) return;
      seen.add(key);
      out.add(Map<String, dynamic>.from(c)..['_idKey'] = idKey);
    }

    for (final c in _officialCards()) {
      add(c, 'official');
    }
    for (final c in _srcCards) {
      add(c, 'source');
    }
    return out;
  }

  List<Map<String, dynamic>> _officialCards() {
    if (_feed == null || _feed!['official'] == null) return [];
    final section = Map<String, dynamic>.from(_feed!['official'] as Map);
    if (section['cards'] is! List) return [];
    return (section['cards'] as List).map((c) => Map<String, dynamic>.from(c as Map)).toList();
  }

  List<Map<String, dynamic>> _updatesCards() {
    if (_feed == null || _feed!['updates'] == null) return [];
    final section = Map<String, dynamic>.from(_feed!['updates'] as Map);
    if (section['cards'] is! List) return [];
    return (section['cards'] as List).map((c) => Map<String, dynamic>.from(c as Map)).toList();
  }

  void _openCard(Map<String, dynamic> card) {
    final id = (card['id'] ?? '').toString();
    if (id.isEmpty) return;
    if (card['_idKey'] == 'source') {
      final sid = (card['sourceId'] ?? '').toString();
      final sname = Uri.encodeComponent((card['sourceName'] ?? sid).toString());
      GoRouter.of(context).push('/source/$sid/comic/$id?sourceName=$sname');
    } else {
      GoRouter.of(context).push('/official/$id');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: DS.bg,
      body: RefreshIndicator(
        onRefresh: _load,
        color: DS.accent,
        child: _buildBody(),
      ),
    );
  }

  Widget _buildBody() {
    if (_loading && _feed == null && _srcCards.isEmpty) {
      return const Center(child: CircularProgressIndicator(color: DS.accent));
    }
    final hero = _feed?['hero'] != null ? Map<String, dynamic>.from(_feed!['hero'] as Map) : null;
    final merged = _mergedCards();
    final official = _officialCards();
    final updates = _updatesCards();

    return CustomScrollView(
      physics: const AlwaysScrollableScrollPhysics(parent: BouncingScrollPhysics()),
      slivers: [
        SliverToBoxAdapter(child: _buildHero(hero)),
        const SliverToBoxAdapter(child: SizedBox(height: DS.sp20)),

        if (official.isNotEmpty) ...[
          SliverToBoxAdapter(child: _SectionHeader(title: '漫界官方', subtitle: '官方精选 · 实时更新')),
          SliverToBoxAdapter(child: _CardRow(cards: official, onTap: (c) => _openCard(Map<String, dynamic>.from(c)..['_idKey'] = 'official'))),
          const SliverToBoxAdapter(child: SizedBox(height: DS.sp16)),
        ],

        if (updates.isNotEmpty) ...[
          SliverToBoxAdapter(child: _SectionHeader(title: '今日更新')),
          SliverToBoxAdapter(child: _CardRow(cards: updates, onTap: (c) => _openCard(Map<String, dynamic>.from(c)..['_idKey'] = 'official'))),
          const SliverToBoxAdapter(child: SizedBox(height: DS.sp16)),
        ],

        if (merged.isNotEmpty) ...[
          SliverToBoxAdapter(child: _SectionHeader(title: '热门推荐', subtitle: '官方 + 已安装源实时聚合')),
          SliverPadding(
            padding: const EdgeInsets.symmetric(horizontal: DS.sp12),
            sliver: SliverGrid(
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 3, childAspectRatio: 0.6, crossAxisSpacing: DS.sp8, mainAxisSpacing: DS.sp8),
              delegate: SliverChildBuilderDelegate((_, i) {
                final card = merged[i];
                if (i >= 30) return null;
                return _GridCard(card: card, onTap: () => _openCard(card));
              }, childCount: merged.length > 30 ? 30 : merged.length),
            ),
          ),
          const SliverToBoxAdapter(child: SizedBox(height: DS.sp24)),
        ],

        if (merged.isEmpty && official.isEmpty)
          SliverFillRemaining(child: _empty()),

        const SliverToBoxAdapter(child: SizedBox(height: 100)),
      ],
    );
  }

  Widget _empty() {
    if (_error != null) {
      return Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
        Icon(Icons.cloud_off_rounded, size: 44, color: DS.textDisabled),
        const SizedBox(height: 12),
        Text(_error!, style: const TextStyle(color: DS.textTertiary, fontSize: 13)),
        const SizedBox(height: 16),
        FilledButton(onPressed: _load, child: const Text('重试')),
      ]));
    }
    return const Center(child: Text('暂无内容，去发现页安装漫画源', style: TextStyle(color: DS.textTertiary)));
  }

  Widget _buildHero(Map<String, dynamic>? hero) {
    if (hero == null || hero.isEmpty) {
      return Container(
        height: 260,
        margin: const EdgeInsets.fromLTRB(DS.sp12, DS.sp4, DS.sp12, 0),
        decoration: BoxDecoration(color: DS.surface1, borderRadius: BorderRadius.circular(DS.rLg)),
        child: const Center(child: Text('漫界', style: TextStyle(fontSize: 28, fontWeight: FontWeight.w800, color: DS.textPrimary))),
      );
    }
    final cover = hero['cover'] ?? '';
    return GestureDetector(
      onTap: () => GoRouter.of(context).push('/official/${hero['id'] ?? ''}'),
      child: Container(
        height: 320,
        margin: EdgeInsets.fromLTRB(DS.sp12, DS.sp4, DS.sp12, 0),
        decoration: BoxDecoration(borderRadius: BorderRadius.circular(DS.rLg), boxShadow: [BoxShadow(color: Colors.black38, blurRadius: 24, offset: Offset(0, 8))]),
        clipBehavior: Clip.antiAlias,
        child: Stack(fit: StackFit.expand, children: [
          if (cover.isNotEmpty)
            CachedNetworkImage(imageUrl: cover, fit: BoxFit.cover, errorWidget: (_, __, ___) => Container(color: DS.surface2))
          else
            Container(color: DS.surface1),
          Positioned.fill(child: DecoratedBox(decoration: BoxDecoration(
            gradient: LinearGradient(begin: Alignment.topCenter, end: Alignment.bottomCenter, colors: [Colors.transparent, Colors.transparent, Colors.black87]),
          ))),
          Positioned(bottom: DS.sp16, left: DS.sp16, right: DS.sp16, child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              Container(padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4), decoration: BoxDecoration(color: DS.accent, borderRadius: BorderRadius.circular(DS.rSm)),
                child: const Text('漫界官方', style: TextStyle(fontSize: 11, color: Colors.white, fontWeight: FontWeight.w700))),
              const SizedBox(width: 8),
              ...((hero['genres'] as List<dynamic>? ?? []).take(3).map((g) => Padding(
                padding: const EdgeInsets.only(right: 6),
                child: Container(padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3), decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.15), borderRadius: BorderRadius.circular(6)),
                  child: Text(g.toString(), style: const TextStyle(fontSize: 11, color: DS.textSecondary)),
                ),
              ))),
            ]),
            const SizedBox(height: DS.sp8),
            Text(hero['title'] ?? '', style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w800, color: DS.textPrimary)),
            const SizedBox(height: DS.sp4),
            if ((hero['author'] as String?)?.isNotEmpty == true)
              Text(hero['author'], style: const TextStyle(fontSize: 13, color: DS.textSecondary)),
          ])),
        ]),
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  final String title;
  final String? subtitle;
  const _SectionHeader({required this.title, this.subtitle});
  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(DS.sp16, 0, DS.sp16, DS.sp8),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(title, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w700, color: DS.textPrimary)),
        if (subtitle != null && subtitle!.isNotEmpty) ...[
          const SizedBox(height: 2),
          Text(subtitle!, style: const TextStyle(fontSize: 12, color: DS.textTertiary)),
        ],
      ]),
    );
  }
}

class _CardRow extends StatelessWidget {
  final List<Map<String, dynamic>> cards;
  final ValueChanged<Map<String, dynamic>> onTap;
  const _CardRow({required this.cards, required this.onTap});
  @override
  Widget build(BuildContext context) {
    return SizedBox(height: 220, child: ListView.separated(
      padding: const EdgeInsets.symmetric(horizontal: DS.sp16),
      scrollDirection: Axis.horizontal, physics: const BouncingScrollPhysics(),
      itemCount: cards.length,
      separatorBuilder: (_, __) => const SizedBox(width: DS.sp12),
      itemBuilder: (_, i) {
        final card = cards[i];
        final cover = card['cover'] ?? '';
        return GestureDetector(
          onTap: () => onTap(card),
          child: SizedBox(width: 130, child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Expanded(
              child: Container(decoration: BoxDecoration(borderRadius: BorderRadius.circular(DS.rMd), color: DS.surface1), clipBehavior: Clip.antiAlias,
                child: cover.toString().isNotEmpty ? CachedNetworkImage(imageUrl: cover, fit: BoxFit.cover, width: double.infinity,
                    placeholder: (_, __) => Container(color: DS.surface2), errorWidget: (_, __, ___) => Container(color: DS.surface2))
                  : Container(color: DS.surface2)),
            ),
            const SizedBox(height: DS.sp8),
            Text(card['title'] ?? '', maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: DS.textPrimary)),
            if (card['sourceName'] != null)
              Text(card['sourceName'], maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 10, color: DS.textTertiary)),
          ])),
        );
      },
    ));
  }
}

class _GridCard extends StatelessWidget {
  final Map<String, dynamic> card;
  final VoidCallback onTap;
  const _GridCard({required this.card, required this.onTap});
  @override
  Widget build(BuildContext context) {
    final cover = card['cover'] ?? '';
    return GestureDetector(
      onTap: onTap,
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Expanded(child: Container(
          decoration: BoxDecoration(borderRadius: BorderRadius.circular(DS.rMd), color: DS.surface1),
          clipBehavior: Clip.antiAlias,
          child: cover.toString().isNotEmpty
              ? CachedNetworkImage(imageUrl: cover, fit: BoxFit.cover, width: double.infinity,
                  placeholder: (_, __) => Container(color: DS.surface2), errorWidget: (_, __, ___) => Container(color: DS.surface2))
              : Container(color: DS.surface2),
        )),
        const SizedBox(height: 6),
        Text(card['title'] ?? '', maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.w600, color: DS.textPrimary)),
        Text('${card['sourceName'] ?? ''}', maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 10, color: DS.textTertiary)),
      ]),
    );
  }
}