import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../app/theme/theme.dart';
import '../../../app/components/manjie_chip.dart';
import '../../../app/components/manjie_shimmer.dart';

class DiscoverPage extends StatefulWidget {
  const DiscoverPage({super.key});

  @override
  State<DiscoverPage> createState() => _DiscoverPageState();
}

class _DiscoverPageState extends State<DiscoverPage> {
  String _selectedCategory = '全部';
  String _sortBy = 'latest';
  bool _loading = false;

  final List<String> _categories = ['全部', '热血', '恋爱', '悬疑', '奇幻', '科幻', '搞笑', '恐怖', '校园', '后宫', '冒险', '战斗', '萌系'];
  final List<String> _sortOptions = [
    {'label': '最新', 'value': 'latest'},
    {'label': '最热', 'value': 'popular'},
    {'label': '评分', 'value': 'rating'},
  ].map((e) => e['value']!).toList();
  final List<String> _sortLabels = ['最新', '最热', '评分'];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('发现'),
        actions: [
          IconButton(
            icon: const Icon(Icons.search),
            onPressed: () => context.push('/search'),
          ),
        ],
      ),
      body: Column(
        children: [
          // 分类标签
          Container(
            height: 44,
            child: ListView.builder(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 16),
              itemCount: _categories.length,
              itemBuilder: (_, i) => Padding(
                padding: const EdgeInsets.only(right: 8),
                child: ManjieChip(
                  label: _categories[i],
                  selected: _selectedCategory == _categories[i],
                  onTap: () => setState(() => _selectedCategory = _categories[i]),
                ),
              ),
            ),
          ),

          // 排序栏
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Row(
              children: [
                ...List.generate(_sortOptions.length, (i) {
                  final isSelected = _sortBy == _sortOptions[i];
                  return Padding(
                    padding: const EdgeInsets.only(right: 12),
                    child: GestureDetector(
                      onTap: () => setState(() => _sortBy = _sortOptions[i]),
                      child: Row(
                        children: [
                          Icon(
                            isSelected ? Icons.radio_button_checked : Icons.radio_button_unchecked,
                            size: 16,
                            color: isSelected ? AppTheme.primary : AppTheme.textSecondary,
                          ),
                          const SizedBox(width: 4),
                          Text(_sortLabels[i], style: TextStyle(
                            color: isSelected ? AppTheme.primary : AppTheme.textSecondary,
                            fontSize: 13,
                          )),
                        ],
                      ),
                    ),
                  );
                }),
                const Spacer(),
                Text('共 120 部', style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
              ],
            ),
          ),

          const Divider(color: AppTheme.divider, height: 1),

          // 漫画网格
          Expanded(
            child: _loading
              ? const ManjieGridShimmer()
              : GridView.builder(
                  padding: const EdgeInsets.all(16),
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 3,
                    childAspectRatio: 0.65,
                    crossAxisSpacing: 8,
                    mainAxisSpacing: 8,
                  ),
                  itemCount: 30,
                  itemBuilder: (_, i) => GestureDetector(
                    onTap: () => context.push('/comic/${i + 1}'),
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
                                  Colors.primaries[i % Colors.primaries.length],
                                  Colors.primaries[(i + 3) % Colors.primaries.length].withOpacity(0.7),
                                ],
                              ),
                            ),
                            child: Center(
                              child: Text('${i + 1}', style: const TextStyle(fontSize: 28, color: Colors.white24)),
                            ),
                          ),
                        ),
                        const SizedBox(height: 6),
                        Text('漫画 ${i + 1}', style: const TextStyle(color: AppTheme.textPrimary, fontSize: 13), maxLines: 1, overflow: TextOverflow.ellipsis),
                        Text('更新至 100 话', style: const TextStyle(color: AppTheme.textSecondary, fontSize: 11)),
                      ],
                    ),
                  ),
                ),
          ),
        ],
      ),
    );
  }
}