import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:get_it/get_it.dart';
import '../../../app/theme/theme.dart';
import '../../../core/network/api_client.dart';
import '../../../plugins/source_data_service.dart';

/// 源分类页 — 分类结构浏览 + 分类漫画列表（分页）+ 排行榜
class SourceCategoryPage extends StatefulWidget {
  final String sourceId;
  /// 初始分类（从 explore 板块点"更多"进入时指定）
  final String? initialCategory;
  const SourceCategoryPage({super.key, required this.sourceId, this.initialCategory});

  @override
  State<SourceCategoryPage> createState() => _SourceCategoryPageState();
}

class _SourceCategoryPageState extends State<SourceCategoryPage> {
  bool _loading = true;
  String? _error;

  // 分类结构
  List<Map<String, dynamic>> _parts = [];
  String _activePart = '推荐';
  String _activeCategory = '';
  bool _isRanking = false;
  // 排行榜类型
  static const _rankTypes = [
    {'key': 'new', 'name': '新书榜'},
    {'key': 'popular', 'name': '人气榜'},
    {'key': 'end', 'name': '完结榜'},
    {'key': 'recommend', 'name': '推荐榜'},
  ];
  String _rankType = 'new';

  // 漫画列表（分页）
  List<dynamic> _comics = [];
  bool _comicsLoading = false;
  bool _hasMore = true;
  int _page = 1;
  final _scroll = ScrollController();

  List<String> _selectedOptions = [];

  @override
  void initState() {
    super.initState();
    _scroll.addListener(_onScroll);
    _loadCategories();
  }

  @override
  void dispose() {
    _scroll.removeListener(_onScroll);
    _scroll.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scroll.position.pixels > _scroll.position.maxScrollExtent - 300) {
      if (_hasMore && !_comicsLoading) _loadMore();
    }
  }

  Future<void> _loadCategories() async {
    setState(() { _loading = true; _error = null; });
    try {
      final parts = await SourceDataService.instance.categories(widget.sourceId);
      if (parts.isNotEmpty) {
        setState(() {
          _parts = parts;
          if (parts.isNotEmpty) {
            _activePart = parts.first['name']?.toString() ?? '分类';
            final cats = (_activeCategories());
            // 初始分类：优先 widget.initialCategory，否则第一个
            if (cats.contains(widget.initialCategory)) {
              _activeCategory = widget.initialCategory!;
              _isRanking = _activeCategory == '排行榜';
            } else {
              _activeCategory = cats.isNotEmpty ? cats.first : '';
            }
          }
          _loading = false;
        });
        if (_activeCategory.isNotEmpty) await _loadComics(reset: true);
      } else {
        setState(() { _loading = false; _error = '该源暂无分类'; });
      }
    } catch (e) {
      setState(() { _loading = false; _error = '加载失败: $e'; });
    }
  }

  List<String> _activeCategories() {
    for (final p in _parts) {
      if (p['name'] == _activePart) {
        return (p['categories'] as List?)?.map((e) => e.toString()).toList() ?? [];
      }
    }
    return [];
  }

  Future<void> _loadComics({bool reset = false}) async {
    if (_activeCategory.isEmpty) return;
    setState(() { _comicsLoading = true; if (reset) _page = 1; });
    try {
      final q = _isRanking ? _rankType : _activeCategory;
      final result = await SourceDataService.instance.categoryComics(
          widget.sourceId, q, reset ? 1 : _page,
          '',
          _selectedOptions,
          _isRanking);
      final list = (result['items'] as List?) ?? [];
      setState(() {
        _comics = reset ? list : [..._comics, ...list];
        _hasMore = result['hasMore'] == true;
        if (_hasMore) _page++;
        _comicsLoading = false;
      });
    } catch (e) {
      setState(() { _comicsLoading = false; _hasMore = false; });
    }
  }

  Future<void> _loadMore() async {
    if (_comicsLoading || !_hasMore) return;
    await _loadComics();
  }

  void _selectCategory(String cat) {
    HapticFeedback.selectionClick();
    setState(() {
      _activeCategory = cat;
      _isRanking = cat == '排行榜';
      _comics = [];
      _hasMore = true;
    });
    _loadComics(reset: true);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: CustomScrollView(
        controller: _scroll,
        physics: const BouncingScrollPhysics(),
        slivers: [
          SliverAppBar(
            pinned: true,
            backgroundColor: AppTheme.surface.withValues(alpha: 0.9),
            elevation: 0,
            leading: GestureDetector(
              onTap: () => context.pop(),
              child: Container(
                margin: const EdgeInsets.all(8),
                decoration: BoxDecoration(color: AppTheme.surfaceLight.withValues(alpha: 0.6), borderRadius: BorderRadius.circular(12)),
                child: const Icon(Icons.arrow_back_ios_new_rounded, size: 18, color: AppTheme.textPrimary),
              ),
            ),
            title: Text('${widget.sourceId} · 分类',
                style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 17, color: AppTheme.textPrimary)),
          ),

          if (_loading)
            const SliverFillRemaining(child: Center(child: CircularProgressIndicator(color: AppTheme.primary, strokeWidth: 2)))
          else if (_error != null)
            SliverFillRemaining(
              child: Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                Text(_error!, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
                const SizedBox(height: 16),
                ElevatedButton(onPressed: _loadCategories, style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primary),
                  child: const Text('重试', style: TextStyle(color: Colors.white))),
              ])),
            )
          else ...[
            // 分类分组切换
            if (_parts.length > 1)
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  child: Row(children: [
                    ..._parts.map((p) {
                      final active = p['name'] == _activePart;
                      return GestureDetector(
                        onTap: () {
                          setState(() {
                            _activePart = p['name']?.toString() ?? '';
                            final cats = _activeCategories();
                            _activeCategory = cats.isNotEmpty ? cats.first : '';
                            _comics = [];
                            _hasMore = true;
                          });
                          if (_activeCategory.isNotEmpty) _loadComics(reset: true);
                        },
                        child: Container(
                          margin: const EdgeInsets.only(right: 8),
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 7),
                          decoration: BoxDecoration(
                            color: active ? AppTheme.primary : AppTheme.surfaceLight.withValues(alpha: 0.5),
                            borderRadius: BorderRadius.circular(20),
                          ),
                          child: Text(p['name']?.toString() ?? '',
                              style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600,
                                  color: active ? Colors.white : AppTheme.textSecondary)),
                        ),
                      );
                    }),
                  ]),
                ),
              ),
            // 分类标签
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
                child: Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: _activeCategories().map((cat) {
                    final active = cat == _activeCategory;
                    return GestureDetector(
                      onTap: () => _selectCategory(cat),
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                        decoration: BoxDecoration(
                          color: active
                              ? AppTheme.primary.withValues(alpha: 0.15)
                              : AppTheme.surfaceLight.withValues(alpha: 0.5),
                          borderRadius: BorderRadius.circular(16),
                          border: active ? Border.all(color: AppTheme.primary, width: 1) : null,
                        ),
                        child: Text(cat,
                            style: TextStyle(fontSize: 12,
                                color: active ? AppTheme.primary : AppTheme.textSecondary,
                                fontWeight: active ? FontWeight.w700 : FontWeight.w500)),
                      ),
                    );
                  }).toList(),
                ),
              ),
            ),
            // 排行榜类型（当分类为"排行榜"时）
            if (_isRanking)
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
                  child: Row(children: _rankTypes.map((t) {
                    final active = t['key'] == _rankType;
                    return GestureDetector(
                      onTap: () {
                        setState(() { _rankType = t['key']!; _comics = []; _hasMore = true; });
                        _loadComics(reset: true);
                      },
                      child: Container(
                        margin: const EdgeInsets.only(right: 8),
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
                        decoration: BoxDecoration(
                          color: active ? AppTheme.accent.withValues(alpha: 0.15) : AppTheme.surfaceLight.withValues(alpha: 0.5),
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: Text(t['name']!,
                            style: TextStyle(fontSize: 11,
                                color: active ? AppTheme.accent : AppTheme.textSecondary,
                                fontWeight: active ? FontWeight.w700 : FontWeight.w500)),
                      ),
                    );
                  }).toList()),
                ),
              ),
            // 漫画网格
            if (_comics.isEmpty && _comicsLoading)
              const SliverFillRemaining(child: Center(child: CircularProgressIndicator(color: AppTheme.primary, strokeWidth: 2)))
            else if (_comics.isEmpty)
              const SliverPadding(
                padding: EdgeInsets.all(40),
                sliver: SliverToBoxAdapter(
                  child: Center(child: Text('该分类暂无漫画', style: TextStyle(color: AppTheme.textTertiary, fontSize: 13))),
                ),
              )
            else
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 100),
                sliver: SliverGrid(
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 3, childAspectRatio: 0.62, crossAxisSpacing: 10, mainAxisSpacing: 10),
                  delegate: SliverChildBuilderDelegate(
                    (_, i) {
                      final c = _comics[i];
                      return _CatComicItem(
                        title: c['title']?.toString() ?? '',
                        subtitle: c['subtitle']?.toString() ?? '',
                        cover: c['cover']?.toString() ?? '',
                        onTap: () {
                          HapticFeedback.lightImpact();
                          final id = c['id']?.toString() ?? '';
                          if (id.isNotEmpty) {
                            GoRouter.of(context).push('/source/${widget.sourceId}/comic/$id');
                          }
                        },
                      );
                    },
                    childCount: _comics.length,
                  ),
                ),
              ),
            if (_comicsLoading && _comics.isNotEmpty)
              const SliverToBoxAdapter(
                child: Padding(padding: EdgeInsets.all(12), child: Center(
                  child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: AppTheme.primary)),
                )),
              ),
          ],
        ],
      ),
    );
  }
}

class _CatComicItem extends StatelessWidget {
  final String title, subtitle, cover;
  final VoidCallback onTap;
  const _CatComicItem({required this.title, required this.subtitle, required this.cover, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        decoration: BoxDecoration(
          color: AppTheme.surface,
          borderRadius: BorderRadius.circular(AppTheme.radiusMd),
          border: Border.all(color: AppTheme.glassBorder, width: 0.5),
        ),
        clipBehavior: Clip.antiAlias,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Container(
                width: double.infinity,
                color: AppTheme.surfaceLight,
                child: cover.isNotEmpty
                    ? Image.network(cover, fit: BoxFit.cover,
                        errorBuilder: (_, __, ___) =>
                            const Icon(Icons.menu_book_rounded, size: 30, color: AppTheme.textTertiary))
                    : const Icon(Icons.menu_book_rounded, size: 30, color: AppTheme.textTertiary),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(5),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, maxLines: 1, overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: AppTheme.textPrimary)),
                  if (subtitle.isNotEmpty) ...[
                    const SizedBox(height: 2),
                    Text(subtitle, maxLines: 1, overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontSize: 9, color: AppTheme.textSecondary)),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}