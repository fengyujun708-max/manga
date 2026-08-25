import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../../../app/ds.dart';
import '../../../core/network/api_client.dart';
import 'package:get_it/get_it.dart';

/// 首页 — 漫界官方 + 继续阅读 + 热门/更新
/// 数据源：GET /v1/home
class HomePage extends StatefulWidget {
  const HomePage({super.key});
  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> with TickerProviderStateMixin {
  Map<String, dynamic>? _feed;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/home');
      if (res.data is Map) _feed = Map<String, dynamic>.from(res.data as Map);
    } catch (_) {}
    if (!mounted) return;
    setState(() => _loading = false);
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Scaffold(backgroundColor: DS.bg, body: Center(child: CircularProgressIndicator(color: DS.accent)));
    final hero = _feed?['hero'] != null ? Map<String, dynamic>.from(_feed!['hero'] as Map) : null;
    final officialCards = _sectionCards('official');
    final updateCards = _sectionCards('updates');
    final trendingCards = _sectionCards('trending');

    return Scaffold(
      backgroundColor: DS.bg,
      body: CustomScrollView(
        physics: const BouncingScrollPhysics(),
        slivers: [
          // ===== Hero =====
          SliverToBoxAdapter(child: _buildHero(hero)),
          const SliverToBoxAdapter(child: SizedBox(height: DS.sp20)),

          // ===== 漫界官方 =====
          if (officialCards.isNotEmpty) ...[
            SliverToBoxAdapter(
              child: _SectionHeader(title: '漫界官方', subtitle: '精选 · 官方聚合 · 每日更新'),
            ),
            SliverToBoxAdapter(child: _CardRow(cards: officialCards)),
            const SliverToBoxAdapter(child: SizedBox(height: DS.sp16)),
          ],

          // ===== 今日更新 =====
          if (updateCards.isNotEmpty) ...[
            SliverToBoxAdapter(child: _SectionHeader(title: '今日更新')),
            SliverToBoxAdapter(child: _CardRow(cards: updateCards)),
            const SliverToBoxAdapter(child: SizedBox(height: DS.sp16)),
          ],

          // ===== 热门 =====
          if (trendingCards.isNotEmpty) ...[
            SliverToBoxAdapter(child: _SectionHeader(title: '热门漫画')),
            SliverToBoxAdapter(child: _CardRow(cards: trendingCards)),
          ],
        ],
      ),
    );
  }

  List<Map<String, dynamic>> _sectionCards(String key) {
    if (_feed == null || _feed![key] == null) return [];
    final section = Map<String, dynamic>.from(_feed![key] as Map);
    if (section['cards'] is! List) return [];
    return (section['cards'] as List).map((c) => Map<String, dynamic>.from(c as Map)).toList();
  }

  Widget _buildHero(Map<String, dynamic>? hero) {
    if (hero == null || hero.isEmpty) {
      return Container(
        height: 280,
        decoration: BoxDecoration(color: DS.surface1, borderRadius: BorderRadius.vertical(bottom: Radius.circular(DS.rXl))),
        child: Center(child: Text('漫界', style: TextStyle(fontSize: 28, fontWeight: FontWeight.w800, color: DS.textPrimary))),
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
          CachedNetworkImage(imageUrl: cover, fit: BoxFit.cover, httpHeaders: {'Referer': cover}, errorWidget: (_, __, ___) => Container(color: DS.surface2)),
          // 渐变遮罩
          Positioned.fill(child: DecoratedBox(decoration: BoxDecoration(
            gradient: LinearGradient(begin: Alignment.topCenter, end: Alignment.bottomCenter, colors: [Colors.transparent, Colors.transparent, Colors.black87]),
          ))),
          // 内容
          Positioned(bottom: DS.sp16, left: DS.sp16, right: DS.sp16, child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              Container(padding: EdgeInsets.symmetric(horizontal: 10, vertical: 4), decoration: BoxDecoration(color: DS.accent, borderRadius: BorderRadius.circular(DS.rSm)),
                child: Text('漫界官方', style: TextStyle(fontSize: 11, color: Colors.white, fontWeight: FontWeight.w700))),
              SizedBox(width: DS.sp8),
              ...((hero['genres'] as List<dynamic>? ?? []).take(3).map((g) => Padding(
                padding: const EdgeInsets.only(right: 6),
                child: Container(padding: EdgeInsets.symmetric(horizontal: 8, vertical: 3), decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.15), borderRadius: BorderRadius.circular(6)),
                  child: Text(g.toString(), style: TextStyle(fontSize: 11, color: DS.textSecondary)),
                ),
              ))),
            ]),
            SizedBox(height: DS.sp8),
            Text(hero['title'] ?? '', style: TextStyle(fontSize: 22, fontWeight: FontWeight.w800, color: DS.textPrimary)),
            SizedBox(height: DS.sp4),
            if ((hero['author'] as String?)?.isNotEmpty == true)
              Text(hero['author'], style: TextStyle(fontSize: 13, color: DS.textSecondary)),
            SizedBox(height: DS.sp12),
            FilledButton.icon(
              onPressed: () => GoRouter.of(context).push('/search'),
              icon: Icon(Icons.play_arrow_rounded, size: 18),
              label: Text('开始阅读', style: TextStyle(fontWeight: FontWeight.w700)),
              style: FilledButton.styleFrom(backgroundColor: DS.accent, foregroundColor: Colors.white, padding: EdgeInsets.symmetric(horizontal: 20, vertical: 10)),
            ),
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
        Text(title, style: TextStyle(fontSize: 17, fontWeight: FontWeight.w700, color: DS.textPrimary)),
        if (subtitle != null && subtitle!.isNotEmpty) ...[
          SizedBox(height: 2),
          Text(subtitle!, style: TextStyle(fontSize: 12, color: DS.textTertiary)),
        ],
      ]),
    );
  }
}

class _CardRow extends StatelessWidget {
  final List<Map<String, dynamic>> cards;
  const _CardRow({required this.cards});
  @override
  Widget build(BuildContext context) {
    return SizedBox(height: 220, child: ListView.separated(
      padding: EdgeInsets.symmetric(horizontal: DS.sp16),
      scrollDirection: Axis.horizontal, physics: BouncingScrollPhysics(),
      itemCount: cards.length,
      separatorBuilder: (_, __) => SizedBox(width: DS.sp12),
      itemBuilder: (_, i) {
        final card = cards[i];
        final cover = card['cover'] ?? '';
        return GestureDetector(
          onTap: () => GoRouter.of(context).push('/official/${card['id'] ?? ''}'),
          child: SizedBox(width: 130, child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Expanded(
              child: Container(decoration: BoxDecoration(borderRadius: BorderRadius.circular(DS.rMd), color: DS.surface1), clipBehavior: Clip.antiAlias,
                child: cover.isNotEmpty ? CachedNetworkImage(imageUrl: cover, fit: BoxFit.cover, width: double.infinity,
                    placeholder: (_, __) => Container(color: DS.surface2), errorWidget: (_, __, ___) => Container(color: DS.surface2))
                  : Container(color: DS.surface2)),
            ),
            SizedBox(height: DS.sp8),
            Text(card['title'] ?? '', maxLines: 1, overflow: TextOverflow.ellipsis, style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: DS.textPrimary)),
            if (card['tag'] != null) Text(card['tag'], style: TextStyle(fontSize: 11, color: card['tag'] == 'NEW' ? Color(0xFF34D399) : DS.textTertiary)),
          ])),
        );
      },
    ));
  }
}