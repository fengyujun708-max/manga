import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../app/ds.dart';

/// 搜索页
class SearchPage extends StatefulWidget {
  const SearchPage({super.key});
  @override
  State<SearchPage> createState() => _SearchPageState();
}

class _SearchPageState extends State<SearchPage> {
  final _searchController = TextEditingController();
  bool _isSearching = false;
  bool _showResults = false;

  final List<String> _searchHistory = ['海贼王', '咒术回战', '鬼灭之刃', '一拳超人', '进击的巨人'];
  final List<String> _hotSearches = ['葬送的芙莉莲', '怪兽8号', '间谍过家家', '我推的孩子', '蓝色监狱', '电锯人'];
  final List<Map<String, String>> _searchResults = [];

  @override
  void dispose() { _searchController.dispose(); super.dispose(); }

  void _onSearch(String query) {
    if (query.trim().isEmpty) return;
    FocusScope.of(context).unfocus();
    setState(() { _isSearching = true; _showResults = true; });

    Future.delayed(const Duration(milliseconds: 500), () {
      if (!mounted) return;
      setState(() {
        _searchResults.clear();
        for (int i = 0; i < 12; i++) {
          _searchResults.add({'title': '$query 漫画 ${i + 1}', 'author': '作者 ${i + 1}'});
        }
        _isSearching = false;
        if (!_searchHistory.contains(query)) {
          _searchHistory.insert(0, query);
          if (_searchHistory.length > 10) _searchHistory.removeLast();
        }
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: DS.bg,
      appBar: AppBar(
        backgroundColor: DS.bg, elevation: 0, scrolledUnderElevation: 0,
        leading: IconButton(icon: const Icon(Icons.arrow_back_ios_new_rounded, size: 20, color: DS.textPrimary), onPressed: () => Navigator.pop(context)),
        title: Container(
          height: 40,
          padding: const EdgeInsets.symmetric(horizontal: DS.sp12),
          decoration: BoxDecoration(color: DS.surface2, borderRadius: BorderRadius.circular(20)),
          child: Row(children: [
            const Icon(Icons.search_rounded, size: 18, color: DS.textTertiary),
            const SizedBox(width: 6),
            Expanded(child: TextField(
              controller: _searchController,
              autofocus: true,
              style: const TextStyle(color: DS.textPrimary, fontSize: 15),
              decoration: const InputDecoration(
                hintText: '搜索漫画、作者、标签...',
                hintStyle: TextStyle(color: DS.textDisabled, fontSize: 14),
                border: InputBorder.none, isDense: true,
                contentPadding: EdgeInsets.symmetric(vertical: 10)),
              onSubmitted: _onSearch,
              onChanged: (_) => setState(() {}),
            )),
            if (_searchController.text.isNotEmpty)
              GestureDetector(onTap: () { _searchController.clear(); setState(() => _showResults = false); },
                child: const Icon(Icons.close_rounded, size: 16, color: DS.textTertiary)),
          ]),
        ),
        actions: [Padding(
          padding: const EdgeInsets.only(right: DS.sp8),
          child: TextButton(onPressed: () => _onSearch(_searchController.text),
            child: const Text('搜索', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: DS.accent))),
        )],
      ),
      body: _showResults ? _buildResults() : _buildSearchHome(),
    );
  }

  Widget _buildSearchHome() {
    return ListView(
      padding: const EdgeInsets.all(DS.sp16),
      children: [
        if (_searchHistory.isNotEmpty) ...[
          Row(children: [
            const Text('搜索历史', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: DS.textPrimary)),
            const Spacer(),
            GestureDetector(onTap: () => setState(() => _searchHistory.clear()),
              child: const Icon(Icons.delete_outline_rounded, size: 17, color: DS.textDisabled)),
          ]),
          const SizedBox(height: DS.sp12),
          Wrap(spacing: 8, runSpacing: 8, children: _searchHistory.map((h) => _chip(h, icon: Icons.history_rounded,
              onTap: () { _searchController.text = h; _onSearch(h); },
              onRemove: () => setState(() => _searchHistory.remove(h)))).toList()),
          const SizedBox(height: DS.sp24),
        ],
        const Text('热门搜索', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: DS.textPrimary)),
        const SizedBox(height: DS.sp12),
        Wrap(spacing: 8, runSpacing: 8, children: _hotSearches.asMap().entries.map((e) =>
            _hotChip(e.key + 1, e.value, onTap: () { _searchController.text = e.value; _onSearch(e.value); })).toList()),
      ],
    );
  }

  Widget _buildResults() {
    if (_isSearching) {
      return const Center(child: CircularProgressIndicator(color: DS.accent, strokeWidth: 2.5));
    }
    if (_searchResults.isEmpty) {
      return EmptyState(icon: Icons.search_off_rounded, title: '未找到相关内容', subtitle: '试试其他关键词');
    }
    return GridView.builder(
      padding: const EdgeInsets.all(DS.sp16),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 3, childAspectRatio: 0.55, crossAxisSpacing: DS.sp12, mainAxisSpacing: DS.sp16),
      itemCount: _searchResults.length,
      itemBuilder: (_, i) {
        final item = _searchResults[i];
        return ComicCard(cover: '', title: item['title']!, subtitle: item['author']);
      },
    );
  }

  Widget _chip(String label, {IconData? icon, VoidCallback? onTap, VoidCallback? onRemove}) {
    return Container(
      padding: const EdgeInsets.only(left: 10, right: 4),
      decoration: BoxDecoration(color: DS.surface2, borderRadius: BorderRadius.circular(18)),
      child: GestureDetector(onTap: onTap, behavior: HitTestBehavior.opaque,
        child: Row(mainAxisSize: MainAxisSize.min, children: [
          if (icon != null) ...[Icon(icon, size: 13, color: DS.textTertiary), const SizedBox(width: 5)],
          Text(label, style: const TextStyle(color: DS.textSecondary, fontSize: 13)),
          GestureDetector(onTap: onRemove,
            child: Padding(padding: const EdgeInsets.all(5),
              child: Icon(Icons.close_rounded, size: 14, color: DS.textTertiary))),
        ])),
    );
  }

  Widget _hotChip(int index, String label, {VoidCallback? onTap}) {
    final hot = index <= 3;
    return GestureDetector(onTap: onTap,
      child: Container(padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(color: DS.surface2, borderRadius: BorderRadius.circular(DS.rMd)),
        child: Row(mainAxisSize: MainAxisSize.min, children: [
          Text('$index', style: TextStyle(color: hot ? DS.accent : DS.textTertiary, fontWeight: FontWeight.w800, fontSize: 12)),
          const SizedBox(width: 6),
          Text(label, style: const TextStyle(color: DS.textSecondary, fontSize: 13)),
        ])),
    );
  }
}
