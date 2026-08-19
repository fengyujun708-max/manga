import 'package:flutter/material.dart';
import '../../app/theme/theme.dart';
import '../../app/components/manjie_card.dart';
import '../../app/components/manjie_comic_card.dart';
import '../../app/components/manjie_section_header.dart';

class SearchPage extends StatefulWidget {
  const SearchPage({super.key});

  @override
  State<SearchPage> createState() => _SearchPageState();
}

class _SearchPageState extends State<SearchPage> {
  final _searchController = TextEditingController();
  bool _isSearching = false;
  bool _showResults = false;

  // 搜索历史
  final List<String> _searchHistory = ['海贼王', '咒术回战', '鬼灭之刃', '一拳超人', '进击的巨人'];

  // 热门搜索
  final List<String> _hotSearches = ['葬送的芙莉莲', '怪兽8号', '间谍过家家', '我推的孩子', '蓝色监狱', '电锯人'];

  // 搜索结果
  final List<Map<String, String>> _searchResults = [];

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _onSearch(String query) {
    if (query.trim().isEmpty) return;

    setState(() {
      _isSearching = true;
      _showResults = true;
    });

    // 模拟搜索
    Future.delayed(const Duration(milliseconds: 500), () {
      setState(() {
        _searchResults.clear();
        for (int i = 0; i < 12; i++) {
          _searchResults.add({
            'title': '$query 漫画 ${i + 1}',
            'author': '作者 ${i + 1}',
            'chapter': '${(i + 1) * 10}',
            'color': '${0xFF6C5CE7 + i * 0x111111}',
          });
        }
        _isSearching = false;
        // 添加到历史
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
      appBar: AppBar(
        title: TextField(
          controller: _searchController,
          autofocus: true,
          decoration: InputDecoration(
            hintText: '搜索漫画、作者、标签...',
            hintStyle: const TextStyle(color: AppTheme.textSecondary),
            border: InputBorder.none,
            suffixIcon: _searchController.text.isNotEmpty
              ? IconButton(
                  icon: const Icon(Icons.clear, size: 20),
                  onPressed: () {
                    _searchController.clear();
                    setState(() => _showResults = false);
                  },
                )
              : null,
          ),
          style: const TextStyle(color: AppTheme.textPrimary, fontSize: 16),
          onSubmitted: _onSearch,
          onChanged: (v) => setState(() {}),
        ),
        actions: [
          TextButton(
            onPressed: () => _onSearch(_searchController.text),
            child: const Text('搜索'),
          ),
        ],
      ),
      body: _showResults ? _buildResults() : _buildSearchHome(),
    );
  }

  Widget _buildSearchHome() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        // 搜索历史
        if (_searchHistory.isNotEmpty) ...[
          ManjieSectionHeader(
            title: '搜索历史',
            onSeeAll: () => _showClearHistoryDialog(),
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8, runSpacing: 8,
            children: _searchHistory.map((h) => _HistoryChip(
              label: h,
              onTap: () {
                _searchController.text = h;
                _onSearch(h);
              },
              onRemove: () => setState(() => _searchHistory.remove(h)),
            )).toList(),
          ),
          const SizedBox(height: 24),
        ],

        // 热门搜索
        ManjieSectionHeader(title: '热门搜索'),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8, runSpacing: 8,
          children: _hotSearches.asMap().entries.map((e) => _HotSearchItem(
            index: e.key + 1,
            label: e.value,
            onTap: () {
              _searchController.text = e.value;
              _onSearch(e.value);
            },
          )).toList(),
        ),
      ],
    );
  }

  Widget _buildResults() {
    if (_isSearching) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_searchResults.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.search_off, size: 64, color: AppTheme.primary.withOpacity(0.4)),
            const SizedBox(height: 16),
            const Text('未找到相关内容', style: TextStyle(color: AppTheme.textSecondary, fontSize: 16)),
            const SizedBox(height: 8),
            const Text('试试其他关键词', style: TextStyle(color: AppTheme.textSecondary, fontSize: 14)),
          ],
        ),
      );
    }

    return GridView.builder(
      padding: const EdgeInsets.all(16),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 3,
        childAspectRatio: 0.65,
        crossAxisSpacing: 8,
        mainAxisSpacing: 8,
      ),
      itemCount: _searchResults.length,
      itemBuilder: (_, i) {
        final item = _searchResults[i];
        return GestureDetector(
          onTap: () {
            // TODO: 跳转漫画详情
          },
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Container(
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(10),
                    gradient: LinearGradient(
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                      colors: [
                        Color(int.parse(item['color'] ?? '0xFF6C5CE7')),
                        Color(int.parse(item['color2'] ?? '0xFF0F3460')),
                      ],
                    ),
                  ),
                  child: Center(
                    child: Text(
                      item['title']?.substring(0, 1) ?? '',
                      style: const TextStyle(fontSize: 32, color: Colors.white38, fontWeight: FontWeight.bold),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 6),
              Text(item['title'] ?? '', style: const TextStyle(color: AppTheme.textPrimary, fontSize: 13),
                maxLines: 1, overflow: TextOverflow.ellipsis),
              Text(item['author'] ?? '', style: const TextStyle(color: AppTheme.textSecondary, fontSize: 11)),
            ],
          ),
        );
      },
    );
  }

  void _showClearHistoryDialog() {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.surface,
        title: const Text('清空搜索历史'),
        content: const Text('确定要清空所有搜索历史吗？'),
        actions: [
          TextButton(onPressed: () => Navigator.of(ctx).pop(), child: const Text('取消')),
          TextButton(
            onPressed: () {
              setState(() => _searchHistory.clear());
              Navigator.of(ctx).pop();
            },
            child: const Text('清空', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }
}

class _HistoryChip extends StatelessWidget {
  final String label;
  final VoidCallback onTap;
  final VoidCallback onRemove;

  const _HistoryChip({required this.label, required this.onTap, required this.onRemove});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.only(left: 12, right: 4, top: 6, bottom: 6),
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: AppTheme.divider),
      ),
      child: GestureDetector(
        onTap: onTap,
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.history, size: 14, color: AppTheme.textSecondary),
            const SizedBox(width: 4),
            Text(label, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 13)),
            const SizedBox(width: 4),
            GestureDetector(
              onTap: onRemove,
              child: const Icon(Icons.close, size: 16, color: AppTheme.textSecondary),
            ),
          ],
        ),
      ),
    );
  }
}

class _HotSearchItem extends StatelessWidget {
  final int index;
  final String label;
  final VoidCallback onTap;

  const _HotSearchItem({required this.index, required this.label, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final color = index <= 3 ? Colors.orange : AppTheme.textSecondary;
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(
          color: AppTheme.surface,
          borderRadius: BorderRadius.circular(8),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text('$index', style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 12)),
            const SizedBox(width: 6),
            Text(label, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 13)),
          ],
        ),
      ),
    );
  }
}