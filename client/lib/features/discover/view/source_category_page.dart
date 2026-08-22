import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import '../../../app/widgets/comic_widgets.dart' hide EmptyState;
import '../../../plugins/source_data_service.dart';
import '../../../app/ds.dart';

/// 源分类页 — 胶囊筛选 + 无限滚动漫画网格
class SourceCategoryPage extends StatefulWidget {
  final String sourceId;
  final String? initialCategory;
  const SourceCategoryPage({super.key, required this.sourceId, this.initialCategory});
  @override
  State<SourceCategoryPage> createState() => _SourceCategoryPageState();
}

class _SourceCategoryPageState extends State<SourceCategoryPage> {
  bool _loading = true;
  String? _error;
  List<Map<String, dynamic>> _parts = [];
  String _activePart = '';
  String _activeCategory = '';
  List<Map<String, dynamic>> _comics = [];
  bool _comicsLoading = false;
  bool _hasMore = true;
  int _page = 1;
  final _scroll = ScrollController();

  @override
  void initState() {
    super.initState();
    _scroll.addListener(_onScroll);
    _loadCategories();
  }

  @override
  void dispose() { _scroll.removeListener(_onScroll); _scroll.dispose(); super.dispose(); }

  void _onScroll() {
    if (_scroll.position.pixels > _scroll.position.maxScrollExtent - 400) {
      if (_hasMore && !_comicsLoading) _loadMore();
    }
  }

  List<String> _activeCategories() {
    for (final p in _parts) {
      if (p['name'] == _activePart) {
        final cats = p['categories'];
        if (cats is List) {
          return cats.map((e) {
            if (e is Map) return e['name']?.toString() ?? '';
            return e.toString();
          }).where((s) => s.isNotEmpty).toList();
        }
      }
    }
    return [];
  }

  Future<void> _loadCategories() async {
    setState(() { _loading = true; _error = null; });
    final parts = await SourceDataService.instance.categories(widget.sourceId);
    if (!mounted) return;
    if (parts.isNotEmpty) {
      setState(() {
        _parts = parts;
        _activePart = parts.first['name']?.toString() ?? '';
        final cats = _activeCategories();
        if (cats.contains(widget.initialCategory)) {
          _activeCategory = widget.initialCategory!;
        } else {
          _activeCategory = cats.isNotEmpty ? cats.first : '';
        }
        _loading = false;
      });
      if (_activeCategory.isNotEmpty) _loadComics(reset: true);
    } else {
      setState(() { _loading = false; _error = '该源暂无分类'; });
    }
  }

  Future<void> _loadComics({bool reset = false}) async {
    if (_activeCategory.isEmpty) return;
    setState(() { _comicsLoading = true; if (reset) { _page = 1; _comics = []; _hasMore = true; } });
    try {
      final result = await SourceDataService.instance.categoryComics(widget.sourceId, _activeCategory, reset ? 1 : _page);
      final list = (result['items'] as List?) ?? [];
      if (!mounted) return;
      setState(() {
        _comics = reset ? list.cast<Map<String, dynamic>>() : [..._comics, ...list.cast<Map<String, dynamic>>()];
        _hasMore = result['hasMore'] == true && list.isNotEmpty;
        if (_hasMore) _page++;
        _comicsLoading = false;
      });
    } catch (e) {
      if (mounted) setState(() { _comicsLoading = false; _hasMore = false; });
    }
  }

  Future<void> _loadMore() async {
    if (_comicsLoading || !_hasMore) return;
    await _loadComics();
  }

  void _selectCategory(String cat) {
    HapticFeedback.selectionClick();
    setState(() { _activeCategory = cat; });
    _loadComics(reset: true);
  }

  void _enterComic(String id) {
    GoRouter.of(context).push('/source/${widget.sourceId}/comic/$id');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: DS.bg,
      body: CustomScrollView(
        controller: _scroll,
        physics: const BouncingScrollPhysics(),
        slivers: [
          // 头部
          SliverAppBar(
            pinned: true,
            backgroundColor: Colors.transparent,
            leading: GestureDetector(
              onTap: () => context.pop(),
              child: Container(margin: const EdgeInsets.all(8), decoration: BoxDecoration(color: DS.glassFillStrong, shape: BoxShape.circle), child: const Icon(Icons.arrow_back_ios_new_rounded, size: 18, color: DS.textPrimary)),
            ),
            title: const Text('分类浏览', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w700, color: DS.textPrimary)),
          ),

          if (_loading)
            const SliverFillRemaining(child: LoadingState(text: '加载分类...'))
          else if (_error != null)
            SliverFillRemaining(child: EmptyState(icon: Icons.category_outlined, title: _error!))
          else ...[
            // 分类分区选择（横向）
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
                child: SizedBox(
                  height: 36,
                  child: ListView.separated(
                    scrollDirection: Axis.horizontal,
                    physics: const BouncingScrollPhysics(),
                    itemCount: _parts.length,
                    separatorBuilder: (_, __) => const SizedBox(width: 8),
                    itemBuilder: (ctx, i) {
                      final p = _parts[i];
                      final name = p['name']?.toString() ?? '';
                      final active = name == _activePart;
                      return GestureDetector(
                        onTap: () { HapticFeedback.selectionClick(); setState(() { _activePart = name; _activeCategory = _activeCategories().firstOrNull ?? ''; }); _loadComics(reset: true); },
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                          decoration: BoxDecoration(
                            color: active ? DS.accent.withValues(alpha: 0.15) : DS.surface1,
                            borderRadius: BorderRadius.circular(10),
                            border: Border.all(color: active ? DS.accent.withValues(alpha: 0.3) : DS.glassBorder, width: 0.5),
                          ),
                          child: Text(name, style: TextStyle(fontSize: 13, fontWeight: active ? FontWeight.w600 : FontWeight.w400, color: active ? DS.accent : DS.textSecondary)),
                        ),
                      );
                    },
                  ),
                ),
              ),
            ),

            // 分类标签（横向滚动胶囊）
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                child: SizedBox(
                  height: 32,
                  child: ListView.separated(
                    scrollDirection: Axis.horizontal,
                    physics: const BouncingScrollPhysics(),
                    itemCount: _activeCategories().length,
                    separatorBuilder: (_, __) => const SizedBox(width: 8),
                    itemBuilder: (ctx, i) {
                      final cat = _activeCategories()[i];
                      final active = cat == _activeCategory;
                      return GestureDetector(
                        onTap: () => _selectCategory(cat),
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                          decoration: BoxDecoration(
                            gradient: active ? const LinearGradient(colors: [DS.accent, Color(0xFFD93025)]) : null,
                            color: active ? null : DS.surface1,
                            borderRadius: BorderRadius.circular(16),
                            border: active ? null : Border.all(color: DS.glassBorder, width: 0.5),
                          ),
                          child: Text(cat, style: TextStyle(fontSize: 12, fontWeight: active ? FontWeight.w600 : FontWeight.w400, color: active ? Colors.white : DS.textSecondary)),
                        ),
                      );
                    },
                  ),
                ),
              ),
            ),

            // 漫画网格
            ComicGrid(
              comics: _comics,
              crossAxisCount: 3,
              onComicTap: _enterComic,
              footer: _comicsLoading
                ? const Padding(padding: EdgeInsets.all(20), child: Center(child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: DS.accent))))
                : _hasMore ? null : const Padding(padding: EdgeInsets.all(20), child: Center(child: Text('没有更多了', style: TextStyle(fontSize: 12, color: DS.textTertiary)))),
            ),

            if (_comics.isEmpty && !_comicsLoading)
              const SliverToBoxAdapter(child: SizedBox(height: 200, child: EmptyState(icon: Icons.inbox_rounded, title: '暂无漫画'))),

            const SliverToBoxAdapter(child: SizedBox(height: 100)),
          ],
        ],
      ),
    );
  }
}