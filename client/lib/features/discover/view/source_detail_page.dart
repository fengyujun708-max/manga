import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:get_it/get_it.dart';
import 'dart:convert';
import '../../../app/theme/theme.dart';
import '../../../core/network/api_client.dart';

/// 漫画源详情页 — 从服务器代理执行源 JS，展示源真实内容（板块/搜索/线路）
class SourceDetailPage extends StatefulWidget {
  final String sourceId;
  const SourceDetailPage({super.key, required this.sourceId});

  @override
  State<SourceDetailPage> createState() => _SourceDetailPageState();
}

class _SourceDetailPageState extends State<SourceDetailPage> with TickerProviderStateMixin {
  bool _loading = true;
  bool _routesLoading = false;
  String? _error;
  List<dynamic> _sections = []; // explore 板块
  List<dynamic> _searchResults = [];
  bool _searching = false;
  String _searchQuery = '';

  // 线路
  List<dynamic> _routes = [];
  String _currentRoute = '';

  final _searchCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    _loadExplore();
    _loadRoutes();
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadExplore() async {
    setState(() { _loading = true; _error = null; });
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/source/${widget.sourceId}/explore');
      final data = res.data;
      if (data is Map && data['sections'] is List) {
        setState(() {
          _sections = data['sections'] ?? [];
          _loading = false;
        });
      } else {
        setState(() { _loading = false; _error = '源返回异常'; });
      }
    } catch (e) {
      setState(() { _loading = false; _error = '加载失败: $e'; });
    }
  }

  Future<void> _loadRoutes() async {
    setState(() => _routesLoading = true);
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/source/${widget.sourceId}/routes');
      final data = res.data;
      if (data is Map && data['routes'] is List) {
        setState(() {
          _routes = data['routes'] ?? [];
          final ok = _routes.where((r) => r['ok'] == true).toList();
          if (ok.isNotEmpty) _currentRoute = ok.first['url'] ?? '';
        });
      }
    } catch (_) {}
    setState(() => _routesLoading = false);
  }

  Future<void> _search(String q) async {
    if (q.trim().isEmpty) {
      setState(() { _searching = false; _searchResults = []; });
      return;
    }
    setState(() { _searching = true; _searchQuery = q.trim(); });
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/source/${widget.sourceId}/search', params: {'q': q.trim(), 'page': 1});
      final data = res.data;
      setState(() {
        _searchResults = (data is Map && data['comics'] is List) ? data['comics'] : [];
        _searching = false;
      });
    } catch (e) {
      setState(() { _searching = false; _searchResults = []; });
    }
  }

  void _showRoutePicker() {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppTheme.surface,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Padding(
              padding: EdgeInsets.all(16),
              child: Text('API 线路', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 16, color: AppTheme.textPrimary)),
            ),
            if (_routesLoading)
              const Padding(padding: EdgeInsets.all(20), child: CircularProgressIndicator(strokeWidth: 2, color: AppTheme.primary))
            else
              ..._routes.map((r) {
                final ok = r['ok'] == true;
                final url = r['url']?.toString() ?? '';
                final latency = r['latencyMs'] ?? 0;
                final active = url == _currentRoute;
                return ListTile(
                  leading: Icon(ok ? Icons.check_circle_rounded : Icons.cancel_rounded,
                      color: ok ? AppTheme.success : AppTheme.textTertiary),
                  title: Text(url, style: const TextStyle(fontSize: 13, color: AppTheme.textPrimary), maxLines: 1, overflow: TextOverflow.ellipsis),
                  subtitle: Text(ok ? '延迟 ${latency}ms' : '不可达', style: TextStyle(fontSize: 11, color: ok ? AppTheme.textSecondary : AppTheme.error)),
                  trailing: active ? const Icon(Icons.radio_button_checked_rounded, color: AppTheme.primary, size: 18)
                      : const Icon(Icons.radio_button_off_rounded, color: AppTheme.textTertiary, size: 18),
                  onTap: ok ? () {
                    setState(() => _currentRoute = url);
                    // 保存选择的线路（本地记住，后续请求服务器用）
                    // 此处简单提示，服务器 fallback 自动选择最优
                    Navigator.pop(ctx);
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text('已选择线路: $url'), duration: const Duration(seconds: 1)),
                    );
                  } : null,
                );
              }),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: CustomScrollView(
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
            title: Text(widget.sourceId, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 18, color: AppTheme.textPrimary)),
            actions: [
              // 线路切换
              GestureDetector(
                onTap: _showRoutePicker,
                child: Container(
                  margin: const EdgeInsets.only(right: 12),
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(
                    color: AppTheme.surfaceLight.withValues(alpha: 0.6),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Row(children: [
                    Icon(Icons.swap_vert_rounded, size: 14, color: _currentRoute.isNotEmpty ? AppTheme.primary : AppTheme.textTertiary),
                    const SizedBox(width: 4),
                    Text(_currentRoute.isEmpty ? '线路' : '线路${_routes.length}条',
                        style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary)),
                  ]),
                ),
              ),
            ],
          ),

          // 搜索框
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 14),
                decoration: BoxDecoration(
                  color: AppTheme.surfaceLight.withValues(alpha: 0.5),
                  borderRadius: BorderRadius.circular(14),
                ),
                child: Row(children: [
                  const Icon(Icons.search_rounded, size: 20, color: AppTheme.textTertiary),
                  const SizedBox(width: 10),
                  Expanded(
                    child: TextField(
                      controller: _searchCtrl,
                      style: const TextStyle(fontSize: 14, color: AppTheme.textPrimary),
                      decoration: const InputDecoration(
                        hintText: '搜索漫画',
                        hintStyle: TextStyle(color: AppTheme.textTertiary, fontSize: 14),
                        border: InputBorder.none,
                        isDense: true,
                        contentPadding: EdgeInsets.symmetric(vertical: 12),
                      ),
                      onSubmitted: _search,
                      textInputAction: TextInputAction.search,
                    ),
                  ),
                  if (_searching)
                    GestureDetector(onTap: () { _searchCtrl.clear(); setState(() { _searchResults = []; _searching = false; }); },
                      child: const Icon(Icons.close_rounded, size: 18, color: AppTheme.textTertiary)),
                ]),
              ),
            ),
          ),

          if (_loading)
            const SliverFillRemaining(child: Center(child: CircularProgressIndicator(color: AppTheme.primary, strokeWidth: 2)))
          else if (_error != null && !_searching)
            SliverFillRemaining(
              child: Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                Icon(Icons.cloud_off_rounded, size: 48, color: AppTheme.textTertiary),
                const SizedBox(height: 12),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 40),
                  child: Text(_error!, textAlign: TextAlign.center, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
                ),
                const SizedBox(height: 16),
                ElevatedButton(onPressed: _loadExplore, style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primary),
                  child: const Text('重试', style: TextStyle(color: Colors.white))),
              ])),
            )
          else if (_searching || _searchQuery.isNotEmpty)
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 100),
              sliver: SliverGrid(
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 3, childAspectRatio: 0.62, crossAxisSpacing: 10, mainAxisSpacing: 10),
                delegate: SliverChildBuilderDelegate(
                  (_, i) {
                    final c = _searchResults[i];
                    return _ComicGridItem(
                      title: c['title']?.toString() ?? '未知',
                      subtitle: c['subtitle']?.toString() ?? '',
                      cover: c['cover']?.toString() ?? '',
                      id: c['id']?.toString() ?? '',
                      onTap: () => _openComic(c),
                      index: i,
                    );
                  },
                  childCount: _searchResults.length,
                ),
              ),
            )
          else if (_sections.isEmpty)
            const SliverFillRemaining(child: Center(child: Text('该源暂无内容', style: TextStyle(color: AppTheme.textSecondary))))
          else
            ..._sections.map((sec) => _buildSection(sec)),
        ],
      ),
    );
  }

  Widget _buildSection(dynamic sec) {
    final title = sec['title']?.toString() ?? '';
    final items = (sec['items'] as List?) ?? [];
    if (items.isEmpty) return const SliverToBoxAdapter(child: SizedBox.shrink());
    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.only(top: 8),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Text(title, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16, color: AppTheme.textPrimary)),
            ),
            const SizedBox(height: 10),
            SizedBox(
              height: 168,
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 16),
                itemCount: items.length,
                itemBuilder: (_, i) {
                  final c = items[i];
                  return _HorizontalComicCard(
                    title: c['title']?.toString() ?? '',
                    subtitle: c['subtitle']?.toString() ?? '',
                    cover: c['cover']?.toString() ?? '',
                    onTap: () => _openComic(c),
                    index: i,
                  );
                },
              ),
            ),
            const SizedBox(height: 16),
          ],
        ),
      ),
    );
  }

  void _openComic(dynamic comic) {
    HapticFeedback.lightImpact();
    final id = comic['id']?.toString() ?? '';
    if (id.isEmpty) return;
    GoRouter.of(context).push('/source/${widget.sourceId}/comic/$id');
  }
}

/// 横向漫画卡片（explore 板块）
class _HorizontalComicCard extends StatefulWidget {
  final String title, subtitle, cover;
  final VoidCallback onTap;
  final int index;
  const _HorizontalComicCard({required this.title, required this.subtitle, required this.cover, required this.onTap, required this.index});
  @override
  State<_HorizontalComicCard> createState() => _HorizontalComicCardState();
}

class _HorizontalComicCardState extends State<_HorizontalComicCard> {
  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: widget.onTap,
      child: Container(
        width: 110,
        margin: const EdgeInsets.only(right: 10),
        decoration: BoxDecoration(
          color: AppTheme.surface,
          borderRadius: BorderRadius.circular(AppTheme.radiusMd),
          border: Border.all(color: AppTheme.glassBorder, width: 0.5),
        ),
        clipBehavior: Clip.antiAlias,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              height: 130,
              width: double.infinity,
              color: AppTheme.surfaceLight,
              child: widget.cover.isNotEmpty
                  ? Image.network(widget.cover, fit: BoxFit.cover,
                      errorBuilder: (_, __, ___) => _coverPlaceholder())
                  : _coverPlaceholder(),
            ),
            Padding(
              padding: const EdgeInsets.all(6),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(widget.title, maxLines: 1, overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: AppTheme.textPrimary)),
                  if (widget.subtitle.isNotEmpty) ...[
                    const SizedBox(height: 2),
                    Text(widget.subtitle, maxLines: 1, overflow: TextOverflow.ellipsis,
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

  Widget _coverPlaceholder() => Center(
        child: Icon(Icons.menu_book_rounded, size: 28, color: AppTheme.textTertiary),
      );
}

/// 网格漫画卡片（搜索结果）
class _ComicGridItem extends StatelessWidget {
  final String title, subtitle, cover, id;
  final VoidCallback onTap;
  final int index;
  const _ComicGridItem({required this.title, required this.subtitle, required this.cover, required this.id, required this.onTap, required this.index});

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