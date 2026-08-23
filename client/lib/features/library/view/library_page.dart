import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../../../app/ds.dart';
import '../../../core/services/library_service.dart';

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
    Widget body;
    if (_loading) {
      body = Center(child: CircularProgressIndicator(color: DS.accent));
    } else if (_tab == 0) {
      body = _favorites.isEmpty
          ? emptyView(Icons.favorite_border_rounded, '还没有收藏', '浏览漫画时点击收藏即可添加')
          : GridView.builder(
              padding: EdgeInsets.fromLTRB(12, 0, 12, 100),
              gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 3, childAspectRatio: 0.62, crossAxisSpacing: 10, mainAxisSpacing: 10),
              itemCount: _favorites.length,
              itemBuilder: (_, i) => favCard(_favorites[i]));
    } else {
      body = _history.isEmpty
          ? emptyView(Icons.history_rounded, '暂无阅读记录', '开始阅读后这里会显示你的进度')
          : ListView.builder(padding: EdgeInsets.only(bottom: 100), itemCount: _history.length,
              itemBuilder: (_, i) => histTile(_history[i]));
    }

    return Scaffold(
      backgroundColor: DS.bg,
      appBar: AppBar(backgroundColor: Colors.transparent, title: Text('书架', style: DS.headline)),
            body: RefreshIndicator(onRefresh: _load, color: DS.accent, child: Column(children: [
        _buildTabBar(),
        Expanded(child: body),
      ])),
    );
  }

  Widget _buildTabBar() {
    return Padding(padding: EdgeInsets.fromLTRB(16, 4, 16, 12), child: Container(
      padding: EdgeInsets.all(3), decoration: BoxDecoration(color: DS.surface1, borderRadius: BorderRadius.circular(14)),
      child: Row(children: _buildTabItems()),
    ));
  }

  List<Widget> _buildTabItems() {
    final items = <Widget>[];
    for (var i = 0; i < _tabs.length; i++) {
      items.add(Expanded(child: GestureDetector(
        onTap: () { HapticFeedback.selectionClick(); setState(() => _tab = i); },
        child: AnimatedContainer(duration: Duration(milliseconds: 200), padding: EdgeInsets.symmetric(vertical: 9),
          decoration: BoxDecoration(color: _tab == i ? DS.surface3 : Colors.transparent, borderRadius: BorderRadius.circular(10)),
          child: Center(child: Text(_tabs[i], style: TextStyle(fontSize: 13.5, fontWeight: _tab == i ? FontWeight.w700 : FontWeight.w400, color: _tab == i ? DS.textPrimary : DS.textTertiary)))),
      )));
    }
    return items;
  }

  Widget favCard(Map<String, dynamic> item) {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Expanded(child: Container(decoration: BoxDecoration(borderRadius: BorderRadius.circular(14), color: DS.surface1), clipBehavior: Clip.antiAlias,
        child: CachedNetworkImage(imageUrl: item['cover'] ?? '', fit: BoxFit.cover, width: double.infinity,
          placeholder: (_, __) => Container(color: DS.surface2),
          errorWidget: (_, __, ___) => Icon(Icons.menu_book_rounded, size: 32, color: DS.textDisabled)))),
      SizedBox(height: 6),
      Text(item['title'] ?? '', maxLines: 1, overflow: TextOverflow.ellipsis, style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: DS.textPrimary)),
    ]);
  }

  Widget histTile(Map<String, dynamic> h) {
    final date = DateTime.tryParse(h['readAt'] ?? '');
    final timeStr = date != null ? '${date.month.toString().padLeft(2, '0')}/${date.day.toString().padLeft(2, '0')} ${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}' : '';
    return ListTile(contentPadding: EdgeInsets.symmetric(horizontal: 16),
      leading: Container(width: 48, height: 64, decoration: BoxDecoration(borderRadius: BorderRadius.circular(8), color: DS.surface1), clipBehavior: Clip.antiAlias,
        child: CachedNetworkImage(imageUrl: h['cover'] ?? '', fit: BoxFit.cover, errorWidget: (_, __, ___) => Icon(Icons.image, color: DS.textDisabled))),
      title: Text(h['title'] ?? '', maxLines: 1, style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: DS.textPrimary)),
      subtitle: Text('${h['chapterTitle'] ?? ''} · $timeStr', maxLines: 1, style: TextStyle(fontSize: 12, color: DS.textTertiary)));
  }

  Widget emptyView(IconData icon, String title, String subtitle) {
    return Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
      Icon(icon, size: 56, color: DS.textDisabled), SizedBox(height: 12),
      Text(title, style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600, color: DS.textSecondary)),
      SizedBox(height: 4), Text(subtitle, style: TextStyle(fontSize: 13, color: DS.textTertiary))]));
  }
}
