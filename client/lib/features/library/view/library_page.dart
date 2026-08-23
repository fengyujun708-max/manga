import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../../../app/ds.dart';
import '../../../core/services/library_service.dart';

/// 书架页 — 收藏 / 历史
class LibraryPage extends StatefulWidget {
  const LibraryPage({super.key});
  @override
  State<LibraryPage> createState() => _LibraryPageState();
}

class _LibraryPageState extends State<LibraryPage> {
  int _tab = 0;
  bool _loading = true;
  List<Map<String, dynamic>> _favorites = [];
  List<Map<String, dynamic>> _history = [];

  static const _tabs = ['收藏', '历史'];

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    setState(() => _loading = true);
    _favorites = await LibraryService.instance.getFavorites();
    _history = await LibraryService.instance.getHistory();
    if (mounted) setState(() => _loading = false);
  }

  @override
  Widget build(BuildContext context) {
    final children = <Widget>[];
    if (_loading) {
      children.add(SliverPadding(padding: EdgeInsets.all(DS.sp16), sliver: SliverGrid(
        gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 3, childAspectRatio: 0.65, crossAxisSpacing: DS.sp12, mainAxisSpacing: DS.sp12),
        delegate: SliverChildBuilderDelegate((_, __) => Container(decoration: BoxDecoration(color: DS.surface1, borderRadius: BorderRadius.circular(DS.rMd))))),
      ));
    } else if (_tab == 0) {
      if (_favorites.isEmpty) {
        children.add(SliverFillRemaining(child: _empty(Icons.favorite_border_rounded, '还没有收藏')));
      } else {
        children.add(SliverPadding(padding: EdgeInsets.fromLTRB(DS.sp12, 0, DS.sp12, 100), sliver: SliverGrid(
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 3, childAspectRatio: 0.62, crossAxisSpacing: DS.sp10, mainAxisSpacing: DS.sp10),
          delegate: SliverChildBuilderDelegate((_, i) => _favCard(_favorites[i]), childCount: _favorites.length))));
      }
    } else {
      if (_history.isEmpty) {
        children.add(SliverFillRemaining(child: _empty(Icons.history_rounded, '暂无阅读记录')));
      } else {
        children.add(SliverPadding(padding: EdgeInsets.fromLTRB(DS.sp16, 0, DS.sp16, 100), sliver: SliverList(
          delegate: SliverChildBuilderDelegate((_, i) => _histTile(_history[i]), childCount: _history.length))));
      }
    }

    return Scaffold(
      backgroundColor: DS.bg,
      body: RefreshIndicator(onRefresh: _load, color: DS.accent,
        child: CustomScrollView(physics: AlwaysScrollableScrollPhysics(), slivers: [
          SliverAppBar(floating: true, snap: true, backgroundColor: Colors.transparent, title: Text('书架', style: DS.headline)),
          SliverToBoxAdapter(child: Padding(
            padding: EdgeInsets.fromLTRB(DS.sp16, DS.sp4, DS.sp16, DS.sp12),
            child: Container(padding: EdgeInsets.all(3), decoration: BoxDecoration(color: DS.surface1, borderRadius: BorderRadius.circular(DS.rMd)),
              child: Row(children: [
                for (var i = 0; i < _tabs.length; i++)
                  Expanded(child: GestureDetector(
                    onTap: () { HapticFeedback.selectionClick(); setState(() => _tab = i); },
                    child: AnimatedContainer(duration: DS.durStd, padding: EdgeInsets.symmetric(vertical: 9),
                      decoration: BoxDecoration(color: _tab == i ? DS.surface3 : Colors.transparent, borderRadius: BorderRadius.circular(DS.rSm)),
                      child: Center(child: Text(_tabs[i], style: TextStyle(fontSize: 13.5, fontWeight: _tab == i ? FontWeight.w700 : FontWeight.w400, color: _tab == i ? DS.textPrimary : DS.textTertiary))))),
              ]))),
          )),
          ...children,
        ])),
    );
  }

  Widget _favCard(Map<String, dynamic> item) {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Expanded(child: Container(decoration: BoxDecoration(borderRadius: BorderRadius.circular(DS.rMd), color: DS.surface1), clipBehavior: Clip.antiAlias,
        child: CachedNetworkImage(imageUrl: item['cover'] ?? '', fit: BoxFit.cover, width: double.infinity,
          placeholder: (_, __) => Container(color: DS.surface2),
          errorWidget: (_, __, ___) => Icon(Icons.menu_book_rounded, size: 32, color: DS.textDisabled)))),
      SizedBox(height: DS.sp8),
      Text(item['title'] ?? '', maxLines: 1, overflow: TextOverflow.ellipsis, style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: DS.textPrimary)),
    ]);
  }

  Widget _histTile(Map<String, dynamic> h) {
    final date = DateTime.tryParse(h['readAt'] ?? '');
    final timeStr = date != null ? '${date.month.toString().padLeft(2, '0')}/${date.day.toString().padLeft(2, '0')} ${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}' : '';
    return ListTile(contentPadding: EdgeInsets.symmetric(horizontal: 0),
      leading: Container(width: 48, height: 64, decoration: BoxDecoration(borderRadius: BorderRadius.circular(8), color: DS.surface1), clipBehavior: Clip.antiAlias,
        child: CachedNetworkImage(imageUrl: h['cover'] ?? '', fit: BoxFit.cover, errorWidget: (_, __, ___) => Icon(Icons.image, color: DS.textDisabled))),
      title: Text(h['title'] ?? '', maxLines: 1, style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: DS.textPrimary)),
      subtitle: Text('${h['chapterTitle'] ?? ''} · $timeStr', maxLines: 1, style: TextStyle(fontSize: 12, color: DS.textTertiary)));
  }

  Widget _empty(IconData icon, String msg) => Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
    Icon(icon, size: 56, color: DS.textDisabled), SizedBox(height: DS.sp12),
    Text(msg, style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600, color: DS.textSecondary))]));