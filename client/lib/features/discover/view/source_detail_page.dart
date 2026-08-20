import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:get_it/get_it.dart';
import '../../../app/theme/theme.dart';
import '../../../core/network/api_client.dart';

/// 漫画源详情页 — 展示该源的分类和漫画列表
class SourceDetailPage extends StatefulWidget {
  final String sourceId;
  const SourceDetailPage({super.key, required this.sourceId});

  @override
  State<SourceDetailPage> createState() => _SourceDetailPageState();
}

class _SourceDetailPageState extends State<SourceDetailPage> with TickerProviderStateMixin {
  String _selectedCategory = '全部';
  String _sortBy = 'latest';
  bool _loading = true;
  List<dynamic> _categories = [];
  List<dynamic> _comics = [];

  // 预置分类
  final _presetCategories = [
    {'id': 'all', 'name': '全部'},
    {'id': 'action', 'name': '热血'},
    {'id': 'romance', 'name': '恋爱'},
    {'id': 'comedy', 'name': '搞笑'},
    {'id': 'fantasy', 'name': '奇幻'},
    {'id': 'sci-fi', 'name': '科幻'},
    {'id': 'horror', 'name': '恐怖'},
    {'id': 'school', 'name': '校园'},
    {'id': 'mystery', 'name': '悬疑'},
    {'id': 'adventure', 'name': '冒险'},
  ];

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() { _loading = true; });
    try {
      final api = GetIt.instance<ApiClient>();
      // 尝试从服务器获取分类
      final catRes = await api.get('/comic/categories');
      if (catRes.data is List) {
        _categories = catRes.data;
      } else {
        _categories = _presetCategories;
      }
      // 获取漫画列表
      final comicRes = await api.get('/comic/discover', params: {'category': _selectedCategory, 'sort': _sortBy, 'limit': 30});
      if (comicRes.data is Map) {
        _comics = comicRes.data['items'] ?? [];
      }
    } catch (_) {
      _categories = _presetCategories;
      _comics = [];
    }
    setState(() { _loading = false; });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: CustomScrollView(
        physics: const BouncingScrollPhysics(),
        slivers: [
          // 顶部
          SliverAppBar(
            pinned: true, floating: true, snap: true,
            backgroundColor: AppTheme.surface.withValues(alpha: 0.8),
            elevation: 0,
            leading: GestureDetector(
              onTap: () => context.pop(),
              child: Container(
                margin: const EdgeInsets.all(8),
                decoration: BoxDecoration(color: AppTheme.surfaceLight.withValues(alpha: 0.6), borderRadius: BorderRadius.circular(12)),
                child: const Icon(Icons.arrow_back_ios_new_rounded, size: 18, color: AppTheme.textPrimary),
              ),
            ),
            title: Text(widget.sourceId, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 18, color: AppTheme.textPrimary)),
          ),

          // 分类标签栏
          SliverPersistentHeader(
            pinned: true,
            delegate: _CategoryBarDelegate(
              categories: _categories.map((c) => c['name']?.toString() ?? '').toList(),
              selected: _selectedCategory,
              onSelect: (name) {
                HapticFeedback.selectionClick();
                setState(() => _selectedCategory = name);
                _loadData();
              },
            ),
          ),

          // 排序栏
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
              child: Row(children: [
                _sortChip('最新', 'latest'),
                const SizedBox(width: 8),
                _sortChip('最热', 'popular'),
                const SizedBox(width: 8),
                _sortChip('评分', 'rating'),
                const Spacer(),
                Text('${_comics.length} 部', style: const TextStyle(color: AppTheme.textTertiary, fontSize: 12)),
              ]),
            ),
          ),

          // 漫画网格
          if (_loading)
            const SliverFillRemaining(child: Center(child: CircularProgressIndicator(color: AppTheme.primary, strokeWidth: 2)))
          else if (_comics.isEmpty)
            SliverFillRemaining(
              child: Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                Icon(Icons.inbox_rounded, size: 48, color: AppTheme.textTertiary),
                const SizedBox(height: 12),
                Text('暂无漫画', style: TextStyle(color: AppTheme.textSecondary, fontSize: 14)),
              ])),
            )
          else
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 100),
              sliver: SliverGrid(
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 3, childAspectRatio: 0.62, crossAxisSpacing: 10, mainAxisSpacing: 10,
                ),
                delegate: SliverChildBuilderDelegate(
                  (_, i) {
                    final comic = _comics[i];
                    return _ComicGridItem(
                      title: comic['title'] ?? '未知',
                      author: comic['author'] ?? '',
                      rating: (comic['rating'] as num?)?.toDouble() ?? 0,
                      chapterCount: comic['chapterCount'] ?? 0,
                      onTap: () => GoRouter.of(context).push('/comic/${comic['id']}'),
                      index: i,
                    );
                  },
                  childCount: _comics.length,
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _sortChip(String label, String value) {
    final active = _sortBy == value;
    return GestureDetector(
      onTap: () { HapticFeedback.selectionClick(); setState(() => _sortBy = value); _loadData(); },
      child: AnimatedContainer(
        duration: AppTheme.durNormal,
        curve: AppTheme.smoothOut,
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: active ? AppTheme.primary.withValues(alpha: 0.12) : Colors.transparent,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Text(label, style: TextStyle(
          color: active ? AppTheme.primary : AppTheme.textSecondary,
          fontSize: 13, fontWeight: active ? FontWeight.w600 : FontWeight.w400,
        )),
      ),
    );
  }
}

// 分类标签栏（pinned）
class _CategoryBarDelegate extends SliverPersistentHeaderDelegate {
  final List<String> categories;
  final String selected;
  final Function(String) onSelect;
  final double height = 48;

  _CategoryBarDelegate({required this.categories, required this.selected, required this.onSelect});

  @override
  double get minExtent => height;
  @override
  double get maxExtent => height;

  @override
  Widget build(BuildContext context, double shrinkOffset, bool overlaps) {
    return Container(
      color: AppTheme.background,
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        physics: const BouncingScrollPhysics(),
        itemCount: categories.length,
        itemBuilder: (_, i) {
          final cat = categories[i];
          final active = selected == cat;
          return GestureDetector(
            onTap: () => onSelect(cat),
            child: AnimatedContainer(
              duration: AppTheme.durNormal, curve: AppTheme.smoothOut,
              margin: const EdgeInsets.only(right: 8),
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
              decoration: BoxDecoration(
                color: active ? AppTheme.primary.withValues(alpha: 0.15) : AppTheme.surfaceLight.withValues(alpha: 0.4),
                borderRadius: BorderRadius.circular(18),
                border: active ? Border.all(color: AppTheme.primary.withValues(alpha: 0.3), width: 0.5) : null,
              ),
              child: Center(child: Text(cat, style: TextStyle(
                color: active ? AppTheme.primary : AppTheme.textSecondary,
                fontSize: 13, fontWeight: active ? FontWeight.w600 : FontWeight.w400,
              ))),
            ),
          );
        },
      ),
    );
  }

  @override
  bool shouldRebuild(_CategoryBarDelegate old) => selected != old.selected || categories != old.categories;
}

// 网格漫画卡片
class _ComicGridItem extends StatefulWidget {
  final String title, author; final double rating; final int chapterCount; final VoidCallback onTap; final int index;
  const _ComicGridItem({required this.title, required this.author, this.rating = 0, this.chapterCount = 0, required this.onTap, required this.index});

  @override
  State<_ComicGridItem> createState() => _ComicGridItemState();
}

class _ComicGridItemState extends State<_ComicGridItem> with SingleTickerProviderStateMixin {
  late AnimationController _ctrl;
  late Animation<double> _scale;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(duration: AppTheme.durFast, vsync: this);
    _scale = Tween<double>(begin: 1, end: 0.94).animate(CurvedAnimation(parent: _ctrl, curve: AppTheme.smoothOut));
  }

  @override
  void dispose() { _ctrl.dispose(); super.dispose(); }

  @override
  Widget build(BuildContext ctx) {
    return GestureDetector(
      onTapDown: (_) { _ctrl.forward(); HapticFeedback.selectionClick(); },
      onTapUp: (_) { _ctrl.reverse(); widget.onTap(); },
      onTapCancel: () => _ctrl.reverse(),
      child: FadeSlideIn(
        delay: Duration(milliseconds: widget.index * 50),
        child: AnimatedBuilder(
          animation: _scale,
          builder: (ctx, child) => Transform.scale(scale: _scale.value, child: child),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Expanded(
              child: Container(
                decoration: BoxDecoration(
                  gradient: AppTheme.cardGradient,
                  borderRadius: BorderRadius.circular(AppTheme.radiusMd),
                  boxShadow: AppTheme.softShadow,
                ),
                child: Stack(children: [
                  Center(child: Icon(Icons.menu_book_rounded, size: 32, color: AppTheme.textTertiary)),
                  if (widget.rating > 0)
                    Positioned(top: 6, right: 6,
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 2),
                        decoration: BoxDecoration(color: Colors.black.withValues(alpha: 0.55), borderRadius: BorderRadius.circular(8)),
                        child: Row(mainAxisSize: MainAxisSize.min, children: [
                          const Icon(Icons.star_rounded, size: 11, color: AppTheme.accent),
                          const SizedBox(width: 2),
                          Text(widget.rating.toStringAsFixed(1), style: const TextStyle(fontSize: 10, color: Colors.white, fontWeight: FontWeight.w600)),
                        ]),
                      )),
                  Positioned(bottom: 0, left: 0, right: 0, height: 50,
                    child: Container(
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.only(bottomLeft: Radius.circular(AppTheme.radiusMd), bottomRight: Radius.circular(AppTheme.radiusMd)),
                        gradient: LinearGradient(begin: Alignment.topCenter, end: Alignment.bottomCenter, colors: [Colors.transparent, Colors.black.withValues(alpha: 0.4)]),
                      ),
                    )),
                ]),
              ),
            ),
            const SizedBox(height: 6),
            Text(widget.title, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: AppTheme.textPrimary), maxLines: 1, overflow: TextOverflow.ellipsis),
            const SizedBox(height: 2),
            Row(children: [
              Expanded(child: Text(widget.author, style: const TextStyle(fontSize: 10, color: AppTheme.textSecondary), maxLines: 1, overflow: TextOverflow.ellipsis)),
              if (widget.chapterCount > 0)
                Text('${widget.chapterCount}话', style: const TextStyle(fontSize: 9, color: AppTheme.textTertiary)),
            ]),
          ]),
        ),
      ),
    );
  }
}
