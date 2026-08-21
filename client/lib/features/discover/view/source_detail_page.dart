import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:get_it/get_it.dart';
import '../../../app/theme/theme.dart';
import '../../../app/widgets/comic_widgets.dart';
import '../../../core/network/api_client.dart';
import '../../../plugins/source_data_service.dart';

/// 漫画源详情页 — 杂志式沉浸布局
/// 板块横滑 + 搜索 + 分类入口 + 液态玻璃
class SourceDetailPage extends StatefulWidget {
  final String sourceId;
  final String sourceName;
  const SourceDetailPage({super.key, required this.sourceId, this.sourceName = ''});
  @override
  State<SourceDetailPage> createState() => _SourceDetailPageState();
}

class _SourceDetailPageState extends State<SourceDetailPage> with TickerProviderStateMixin {
  bool _loading = true;
  String? _error;
  List<dynamic> _sections = [];
  List<dynamic> _searchResults = [];
  bool _searching = false;
  String _searchQuery = '';
  String _mode = '';
  final _searchCtrl = TextEditingController();
  late TabController _tabCtrl;
  bool _showSearch = false;

  @override
  void initState() {
    super.initState();
    _tabCtrl = TabController(length: 2, vsync: this);
    _loadExplore();
  }

  @override
  void dispose() { _searchCtrl.dispose(); _tabCtrl.dispose(); super.dispose(); }

  Future<void> _loadExplore() async {
    setState(() { _loading = true; _error = null; });
    final result = await SourceDataService.instance.explore(widget.sourceId);
    if (!mounted) return;
    setState(() {
      _sections = result['sections'] ?? [];
      _mode = result['mode'] ?? '';
      _loading = false;
      if ((result['error'] ?? '').isNotEmpty) _error = result['error'];
    });
  }

  Future<void> _search(String q) async {
    if (q.trim().isEmpty) { setState(() { _searching = false; _searchResults = []; }); return; }
    setState(() { _searching = true; _searchQuery = q.trim(); });
    final result = await SourceDataService.instance.search(widget.sourceId, q.trim(), 1);
    if (!mounted) return;
    setState(() { _searchResults = result['items'] ?? []; _searching = false; });
  }

  void _enterComic(String id) {
    GoRouter.of(context).push('/source/${widget.sourceId}/comic/$id');
  }

  @override
  Widget build(BuildContext context) {
    final name = widget.sourceName.isNotEmpty ? widget.sourceName : widget.sourceId;
    final isSearch = _searching || _searchQuery.isNotEmpty;
    return Scaffold(
      backgroundColor: AppTheme.background,
      body: CustomScrollView(
        physics: const BouncingScrollPhysics(),
        slivers: [
          // ── 沉浸式头部（液态玻璃）──
          SliverAppBar(
            expandedHeight: 100,
            pinned: true,
            stretch: true,
            backgroundColor: Colors.transparent,
            elevation: 0,
            flexibleSpace: FlexibleSpaceBar(
              background: Container(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter, end: Alignment.bottomCenter,
                    colors: [AppTheme.primary.withValues(alpha: 0.15), AppTheme.background],
                  ),
                ),
              ),
            ),
            leading: GestureDetector(
              onTap: () => context.pop(),
              child: Container(
                margin: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: AppTheme.glassFillLight,
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.arrow_back_ios_new_rounded, size: 18, color: AppTheme.textPrimary),
              ),
            ),
            title: Text(name, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: AppTheme.textPrimary)),
            actions: [
              // 模式标签
              if (_mode.isNotEmpty)
                Container(
                  margin: const EdgeInsets.only(right: 8),
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: _mode == 'local' ? AppTheme.success.withValues(alpha: 0.15) : AppTheme.primary.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(_mode == 'local' ? '本地' : '代理',
                      style: TextStyle(fontSize: 10, color: _mode == 'local' ? AppTheme.success : AppTheme.primary)),
                ),
              // 搜索按钮
              GestureDetector(
                onTap: () => setState(() => _showSearch = !_showSearch),
                child: Container(
                  margin: const EdgeInsets.only(right: 12),
                  padding: const EdgeInsets.all(6),
                  decoration: BoxDecoration(color: AppTheme.glassFillLight, shape: BoxShape.circle),
                  child: Icon(_showSearch ? Icons.close_rounded : Icons.search_rounded, size: 20, color: AppTheme.textPrimary),
                ),
              ),
            ],
            bottom: _showSearch ? PreferredSize(
              preferredSize: const Size.fromHeight(60),
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                child: LiquidGlass(
                  radius: BorderRadius.circular(14),
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 0),
                  fillColor: AppTheme.glassFillRegular,
                  child: Row(children: [
                    const Icon(Icons.search_rounded, size: 20, color: AppTheme.textTertiary),
                    const SizedBox(width: 10),
                    Expanded(child: TextField(
                      controller: _searchCtrl,
                      autofocus: true,
                      style: const TextStyle(fontSize: 14, color: AppTheme.textPrimary),
                      decoration: const InputDecoration(hintText: '搜索漫画', hintStyle: TextStyle(color: AppTheme.textTertiary, fontSize: 14), border: InputBorder.none, isDense: true),
                      onSubmitted: _search,
                      textInputAction: TextInputAction.search,
                    )),
                    if (_searching) const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 1.5, color: AppTheme.primary)),
                  ]),
                ),
              ),
            ) : null,
          ),

          // ── 内容区 ──
          if (_loading)
            const SliverFillRemaining(child: LoadingState(text: '加载中...'))
          else if (_error != null && _sections.isEmpty)
            SliverFillRemaining(child: EmptyState(
              icon: Icons.cloud_off_rounded, title: '加载失败', subtitle: _error,
              actionLabel: '重试', onAction: _loadExplore,
            ))
          else if (isSearch && _searchResults.isEmpty)
            SliverFillRemaining(child: EmptyState(icon: Icons.search_off_rounded, title: '未找到漫画', subtitle: '试试其他关键词'))
          else if (isSearch)
            ComicGrid(comics: _searchResults.cast<Map<String, dynamic>>(), onComicTap: _enterComic)
          else ...[
            // 板块列表
            SliverList(delegate: SliverChildBuilderDelegate(
              (ctx, i) {
                if (i >= _sections.length) return null;
                final sec = _sections[i];
                final title = sec['title']?.toString() ?? '板块';
                final items = (sec['items'] as List?) ?? [];
                return Padding(
                  padding: const EdgeInsets.only(bottom: 20),
                  child: HorizontalComicSection(
                    title: title,
                    comics: items.cast<Map<String, dynamic>>(),
                    onMore: () => GoRouter.of(context).push('/source/${widget.sourceId}/category?initial=$title'),
                    onComicTap: _enterComic,
                  ),
                );
              },
              childCount: _sections.length,
            )),

            // 分类入口
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                child: GestureDetector(
                  onTap: () => GoRouter.of(context).push('/source/${widget.sourceId}/category'),
                  child: LiquidGlass(
                    radius: BorderRadius.circular(AppTheme.radiusMd),
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                    fillColor: AppTheme.glassFillLight,
                    child: Row(children: [
                      Container(
                        width: 36, height: 36,
                        decoration: BoxDecoration(gradient: AppTheme.primaryGradient, borderRadius: BorderRadius.circular(10)),
                        child: const Icon(Icons.category_rounded, size: 18, color: Colors.white),
                      ),
                      const SizedBox(width: 12),
                      const Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Text('全部分类', style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600, color: AppTheme.textPrimary)),
                        Text('浏览更多漫画', style: TextStyle(fontSize: 12, color: AppTheme.textTertiary)),
                      ])),
                      const Icon(Icons.chevron_right_rounded, color: AppTheme.textTertiary),
                    ]),
                  ),
                ),
              ),
            ),

            // 底部留白
            const SliverToBoxAdapter(child: SizedBox(height: 100)),
          ],
        ],
      ),
    );
  }
}