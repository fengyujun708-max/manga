import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:get_it/get_it.dart';
import '../../../app/ds.dart';
import '../../../core/network/api_client.dart';

/// 聚合首页 — 官方内容实时聚合（用户上传漫画）
/// 本地漫画源在「发现」页浏览。官方实时 /v1/home，不缓存。
class HomePage extends StatefulWidget {
  const HomePage({super.key});
  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  Map<String, dynamic>? _feed;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  String _norm(String t) => t.toLowerCase().replaceAll(
      RegExp(r'[\s\-_.,:;!@#$%^&*()\[\]{}<>?/\\|`~·「」『』【】"\'']+'), '');

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/home');
      if (!mounted) return;
      setState(() {
        _feed = res.data is Map ? Map<String, dynamic>.from(res.data as Map) : null;
        _loading = false;
        if (_feed == null) _error = '首页数据格式异常';
      });
    } catch (e) {
      if (!mounted) return;
      final msg = e.toString();
      setState(() {
        _loading = false;
        _error = msg.contains('401')
            ? '登录已过期，请退出重登'
            : msg.contains('SocketException') || msg.contains('connection')
                ? '网络连接失败，请检查网络'
                : '加载失败: ${msg.substring(0, msg.length.clamp(0, 80))}';
      });
    }
  }

  /// 归一化标题去重（官方优先）
  List<Map<String, dynamic>> _dedup(List list) {
    final seen = <String>{};
    final out = <Map<String, dynamic>>[];
    for (final c in (list.whereType<Map>().map((e) => Map<String, dynamic>.from(Map<String, dynamic>.from(e))))) {
      final k = _norm((c['title'] ?? '').toString());
      if (k.isEmpty || seen.contains(k)) continue;
      seen.add(k);
      out.add(c);
    }
    return out;
  }

  List<Map<String, dynamic>> _sectionCards(String key) {
    if (_feed == null || _feed![key] == null) return [];
    final sec = Map<String, dynamic>.from(_feed![key] as Map);
    if (sec['cards'] is! List) return [];
    return sec['cards'].map((c) => Map<String, dynamic>.from(c as Map)).toList();
  }

  void _openOfficial(String id) {
    if (id.isEmpty) return;
    GoRouter.of(context).push('/official/$id');
  }

  String _idOf(Map<String, dynamic> c) => (c['id'] ?? '').toString();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: DS.bg,
      body: RefreshIndicator(
        onRefresh: _load,
        color: DS.accent,
        child: _loading && _feed == null
            ? const Center(child: CircularProgressIndicator(color: DS.accent))
            : _feed == null
                ? _empty()
                : CustomScrollView(
                    physics: const AlwaysScrollableScrollPhysics(parent: BouncingScrollPhysics()),
                    slivers: [
                      SliverToBoxAdapter(child: _buildHero()),
                      const SliverToBoxAdapter(child: SizedBox(height: DS.sp16)),
                      if (_sectionCards('official').isNotEmpty) ..._section('漫界官方', '官方精选 · 实时更新', _sectionCards('official')),
                      if (_sectionCards('updates').isNotEmpty) ..._section('今日更新', null, _sectionCards('updates')),
                      if (_dedup(_sectionCards('official') + _sectionCards('updates')).isNotEmpty)
                        ..._section('热门', null, _dedup(_sectionCards('official') + _sectionCards('updates'))),
                      if (_sectionCards('official').isEmpty)
                        SliverToBoxAdapter(child: _sourceShortcut()),
                      const SliverToBoxAdapter(child: SizedBox(height: 100)),
                    ],
                  ),
      ),
    );
  }

  List<Widget> _section(String title, String? sub, List cards) {
    return [
      SliverToBoxAdapter(child: Padding(padding: EdgeInsets.symmetric(horizontal: DS.sp12, vertical: 6),
        child: Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
          Text(title, style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: DS.textPrimary)),
          if (sub != null) Text(sub, style: TextStyle(fontSize: 12, color: DS.textTertiary)),
        ]))),
      SliverToBoxAdapter(child: _CardRow(cards: cards.map((c) => Map<String, dynamic>.from(c)).toList(), onTap: (c) => _openOfficial(_idOf(c)))),
      const SliverToBoxAdapter(child: SizedBox(height: 12)),
    ];
  }

  Widget _buildHero() {
    final hero = _feed?['hero'];
    if (hero is! Map) return _heroFallback();
    final h = Map<String, dynamic>.from(hero);
    final cover = (h['cover'] ?? '').toString();
    return GestureDetector(
      onTap: () => _openOfficial((h['id'] ?? h['seriesId'] ?? '').toString()),
      child: Container(
        height: 300, margin: EdgeInsets.symmetric(horizontal: DS.sp12),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(DS.rLg),
          boxShadow: const [BoxShadow(color: Colors.black45, blurRadius: 20, offset: Offset(0, 8))],
        ),
        clipBehavior: Clip.antiAlias,
        child: Stack(fit: StackFit.expand, children: [
          if (cover.isNotEmpty)
            CachedNetworkImage(
              imageUrl: cover, fit: BoxFit.cover, placeholder: (_, __) => Container(color: DS.surface2),
              errorWidget: (_, __, ___) => Container(color: DS.surface2),
            )
          else
            Container(color: DS.surface2),
          Positioned(
            bottom: 0, left: 0, right: 0,
            child: Container(
              height: 96, padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
              decoration: const BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [Colors.transparent, Colors.black87],
                ),
              ),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                if ((h['title'] ?? '').toString().isNotEmpty)
                  Text(h['title'], style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w700, color: DS.textPrimary, height: 1.2), maxLines: 1, overflow: TextOverflow.ellipsis),
                if ((h['author'] ?? '').toString().isNotEmpty)
                  Text('作者：${h['author']}', style: TextStyle(fontSize: 12, color: DS.textTertiary), maxLines: 1),
                const Text('点击查看官方详情 →', style: TextStyle(fontSize: 12, color: DS.accent, fontWeight: FontWeight.w600)),
              ])),
          ),
        ]),
      ),
    );
  }

  Widget _heroFallback() {
    return Container(
      height: 260, margin: EdgeInsets.symmetric(horizontal: DS.sp12, vertical: 4),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(DS.rLg),
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF1E1E2F), Color(0xFF2A2A4A)],
        ),
      ),
      child: const Center(child: Text('漫界', style: TextStyle(fontSize: 32, fontWeight: FontWeight.w800, color: DS.accent))),
    );
  }

  Widget _sourceShortcut() {
    return Container(
      margin: EdgeInsets.symmetric(horizontal: DS.sp12, vertical: 8),
      child: Material(
        color: DS.surface1, borderRadius: BorderRadius.circular(DS.rLg),
        child: InkWell(
          onTap: () => GoRouter.of(context).push('/discover'),
          borderRadius: BorderRadius.circular(DS.rLg),
          child: const Padding(
            padding: EdgeInsets.all(16),
            child: Row(children: [
              Icon(Icons.explore_rounded, color: DS.accent, size: 26),
              SizedBox(width: 12),
              Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text('浏览漫画源', style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600, color: DS.textPrimary)),
                Text('在发现页安装浏览上千个漫画源', style: TextStyle(fontSize: 12, color: DS.textTertiary)),
              ])),
              Icon(Icons.chevron_right_rounded, color: DS.textTertiary),
            ]),
          ),
        ),
      ),
    );
  }

  Widget _empty() {
    if (_error != null) {
      return Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
        const Icon(Icons.cloud_off_rounded, size: 44, color: DS.textDisabled),
        const SizedBox(height: 12),
        Text(_error!, style: const TextStyle(color: DS.textTertiary, fontSize: 13)),
        const SizedBox(height: 16),
        FilledButton(onPressed: _load, child: const Text('重试')),
      ]));
    }
    return Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
      const Icon(Icons.auto_awesome_rounded, size: 44, color: DS.textDisabled),
      const SizedBox(height: 12),
      const Text('暂无更新，去「发现」页安装漫画源', style: TextStyle(color: DS.textTertiary)),
    ]));
  }
}

class _CardRow extends StatelessWidget {
  final List<Map<String, dynamic>> cards;
  final void Function(Map<String, dynamic>) onTap;
  const _CardRow({required this.cards, required this.onTap});

  @override
  Widget build(BuildContext context) {
    if (cards.isEmpty) return const SizedBox.shrink();
    final cs = cards.take(10).toList();
    return SizedBox(
      height: 210,
      child: ListView.separated(
        padding: EdgeInsets.symmetric(horizontal: DS.sp12),
        scrollDirection: Axis.horizontal,
        controller: ScrollController(),
        itemCount: cs.length,
        separatorBuilder: (_, __) => const SizedBox(width: 10),
        itemBuilder: (_, i) => _CardItem(card: cs[i], onTap: onTap),
      ),
    );
  }
}

class _CardItem extends StatelessWidget {
  final Map<String, dynamic> card;
  final void Function(Map<String, dynamic>) onTap;
  const _CardItem({required this.card, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 116,
      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Expanded(
          child: GestureDetector(
            onTap: () => onTap(card),
            child: Container(
              decoration: BoxDecoration(borderRadius: BorderRadius.circular(8), color: DS.surface2),
              clipBehavior: Clip.antiAlias,
              child: CachedNetworkImage(
                imageUrl: (card['cover'] ?? '').toString(),
                fit: BoxFit.cover,
                width: 116,
                placeholder: (_, __) => Container(color: DS.surface2),
                errorWidget: (_, __, ___) => Container(
                  color: DS.surface2,
                  child: const Icon(Icons.broken_image, color: DS.textDisabled, size: 28),
                ),
              ),
            ),
          ),
        ),
        const SizedBox(height: 6),
        Text(
          (card['title'] ?? '').toString(),
          style: const TextStyle(fontSize: 12.5, color: DS.textPrimary, fontWeight: FontWeight.w600),
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
        ),
        if ((card['author'] ?? '').toString().isNotEmpty)
          Text(
            card['author'],
            style: const TextStyle(fontSize: 11, color: DS.textTertiary),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
      ]),
    );
  }
}