import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:get_it/get_it.dart';
import '../../../app/theme/theme.dart';
import '../../../core/network/api_client.dart';

/// 源内漫画详情页 — 从服务器代理执行源 JS 获取真实详情 + 章节
class SourceComicPage extends StatefulWidget {
  final String sourceId;
  final String comicId;
  const SourceComicPage({super.key, required this.sourceId, required this.comicId});

  @override
  State<SourceComicPage> createState() => _SourceComicPageState();
}

class _SourceComicPageState extends State<SourceComicPage> {
  bool _loading = true;
  String? _error;
  Map<String, dynamic> _info = {};
  List<dynamic> _chapters = [];

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/source/${widget.sourceId}/comic/${widget.comicId}');
      final data = res.data;
      if (data is Map) {
        setState(() {
          _info = data;
          _chapters = (data['chapters'] as List?) ?? [];
          _loading = false;
        });
      } else {
        setState(() { _loading = false; _error = '源返回异常'; });
      }
    } catch (e) {
      setState(() { _loading = false; _error = '加载失败: $e'; });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: CustomScrollView(
        physics: const BouncingScrollPhysics(),
        slivers: [
          SliverAppBar(
            pinned: true,
            expandedHeight: 260,
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
            flexibleSpace: FlexibleSpaceBar(
              background: _info['cover']?.toString().isNotEmpty == true
                  ? Image.network(_info['cover'].toString(), fit: BoxFit.cover,
                      errorBuilder: (_, __, ___) => Container(color: AppTheme.surfaceLight))
                  : Container(color: AppTheme.surfaceLight),
            ),
          ),

          if (_loading)
            const SliverFillRemaining(child: Center(child: CircularProgressIndicator(color: AppTheme.primary, strokeWidth: 2)))
          else if (_error != null)
            SliverFillRemaining(
              child: Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                Text(_error!, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
                const SizedBox(height: 16),
                ElevatedButton(onPressed: _load, style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primary),
                  child: const Text('重试', style: TextStyle(color: Colors.white))),
              ])),
            )
          else ...[
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(_info['title']?.toString() ?? '', style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800, color: AppTheme.textPrimary)),
                    const SizedBox(height: 8),
                    if (_info['description']?.toString().isNotEmpty == true)
                      Text(_info['description'].toString(), style: const TextStyle(fontSize: 13, color: AppTheme.textSecondary, height: 1.5)),
                    const SizedBox(height: 12),
                    // 标签
                    if (_info['tags'] is Map)
                      Wrap(
                        spacing: 6,
                        runSpacing: 6,
                        children: (_info['tags'] as Map).entries.expand((e) {
                          final list = e.value is List ? (e.value as List).map((x) => x.toString()).toList() : [];
                          return list.map((v) => Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                            decoration: BoxDecoration(
                              color: AppTheme.primary.withValues(alpha: 0.1),
                              borderRadius: BorderRadius.circular(6),
                            ),
                            child: Text('${e.key}: $v', style: const TextStyle(fontSize: 10, color: AppTheme.primary)),
                          ));
                        }).toList(),
                      ),
                  ],
                ),
              ),
            ),
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
                child: Text('章节列表 (${_chapters.length})', style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 15, color: AppTheme.textPrimary)),
              ),
            ),
            if (_chapters.isEmpty)
              const SliverToBoxAdapter(child: Padding(padding: EdgeInsets.all(20), child: Center(child: Text('暂无章节', style: TextStyle(color: AppTheme.textTertiary)))))
            else
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 100),
                sliver: SliverList.builder(
                  itemCount: _chapters.length,
                  itemBuilder: (_, i) {
                    final ch = _chapters[i];
                    return ListTile(
                      dense: true,
                      leading: const Icon(Icons.menu_book_rounded, size: 16, color: AppTheme.textTertiary),
                      title: Text(ch['title']?.toString() ?? '章节 $i',
                          maxLines: 1, overflow: TextOverflow.ellipsis,
                          style: const TextStyle(fontSize: 13, color: AppTheme.textPrimary)),
                      trailing: Text(ch['group']?.toString() ?? '', style: const TextStyle(fontSize: 10, color: AppTheme.textTertiary)),
                      onTap: () => GoRouter.of(context).push(
                        '/source/${widget.sourceId}/reader/${widget.comicId}/${ch['id']}',
                      ),
                    );
                  },
                ),
              ),
          ],
        ],
      ),
    );
  }
}