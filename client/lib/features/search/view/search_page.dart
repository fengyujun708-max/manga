import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:get_it/get_it.dart';
import '../../../app/ds.dart';
import '../../../core/network/api_client.dart';
import '../../../plugins/source_data_service.dart';

/// 搜索页
/// - 默认：只搜漫界官方源
/// - discover 入口：搜已安装的其他源并去重
/// - 源内搜索：传 sourceId 即只搜该源
class SearchPage extends StatefulWidget {
  final String? sourceId;
  final String sourceName;
  final String scope;
  const SearchPage({super.key, this.sourceId, this.sourceName = '', this.scope = 'official'});
  @override
  State<SearchPage> createState() => _SearchPageState();
}

class _SearchPageState extends State<SearchPage> {
  final _searchController = TextEditingController();
  bool _isSearching = false;
  bool _showResults = false;
  String? _error;
  final List<String> _searchHistory = [];
  List<_SearchGroup> _groups = [];

  bool get _inSource => widget.sourceId != null && widget.sourceId!.isNotEmpty;
  bool get _sourceScope => !_inSource && (widget.scope == 'sources' || widget.scope == 'source');

  @override
  void initState() {
    super.initState();
    _loadHistory();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _loadHistory() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final h = prefs.getString('search_history') ?? '[]';
      final list = (jsonDecode(h) as List).map((e) => e.toString()).toList();
      if (mounted) setState(() {
        _searchHistory
          ..clear()
          ..addAll(list);
      });
    } catch (_) {}
  }

  Future<void> _saveHistory(String q) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      _searchHistory.remove(q);
      _searchHistory.insert(0, q);
      if (_searchHistory.length > 10) _searchHistory.removeLast();
      await prefs.setString('search_history', jsonEncode(_searchHistory));
    } catch (_) {}
  }

  Future<void> _onSearch(String query) async {
    final q = query.trim();
    if (q.isEmpty) return;
    FocusScope.of(context).unfocus();
    setState(() {
      _isSearching = true;
      _showResults = true;
      _error = null;
      _groups = [];
    });
    await _saveHistory(q);

    try {
      final hits = _inSource
          ? await _searchSingleSource(widget.sourceId!, widget.sourceName, q)
          : (_sourceScope ? await _searchOtherSources(q) : await _searchOfficial(q));
      if (!mounted) return;
      setState(() {
        _groups = _groupHits(hits);
        _isSearching = false;
        if (_groups.isEmpty) _error = '未找到相关内容';
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _isSearching = false;
        _error = e.toString();
      });
    }
  }

  Future<List<_SearchHit>> _searchOfficial(String q) async {
    final api = GetIt.instance<ApiClient>();
    final res = await api.get('/comic/search', params: {'q': q, 'page': 1, 'limit': 30});
    final data = res.data is Map ? Map<String, dynamic>.from(res.data as Map) : <String, dynamic>{};
    final items = (data['items'] as List?) ?? const [];
    return items.whereType<Map>().map((e) {
      final m = Map<String, dynamic>.from(e);
      return _SearchHit(
        sourceId: 'official',
        sourceName: '漫界官方',
        comicId: m['id']?.toString() ?? '',
        title: m['title']?.toString() ?? '',
        cover: m['coverUrl']?.toString() ?? m['cover']?.toString() ?? '',
        author: m['author']?.toString() ?? '',
        description: m['description']?.toString() ?? '',
        official: true,
      );
    }).where((e) => e.title.isNotEmpty && e.comicId.isNotEmpty).toList();
  }

  Future<List<_SearchHit>> _searchSingleSource(String sourceId, String sourceName, String q) async {
    final result = await SourceDataService.instance.search(sourceId, q, 1);
    final items = (result['items'] as List?) ?? const [];
    return items.whereType<Map>().map((e) {
      final m = Map<String, dynamic>.from(e);
      return _SearchHit(
        sourceId: sourceId,
        sourceName: sourceName.isNotEmpty ? sourceName : sourceId,
        comicId: m['id']?.toString() ?? '',
        title: m['title']?.toString() ?? '',
        cover: m['cover']?.toString() ?? m['coverUrl']?.toString() ?? '',
        author: m['author']?.toString() ?? '',
        description: m['description']?.toString() ?? '',
      );
    }).where((e) => e.title.isNotEmpty && e.comicId.isNotEmpty).toList();
  }

  Future<List<_SearchHit>> _searchOtherSources(String q) async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString('installed_sources') ?? '[]';
    final manifests = (jsonDecode(raw) as List)
        .whereType<Map>()
        .map((e) => Map<String, dynamic>.from(e))
        .map((e) => {
              'id': e['id']?.toString() ?? '',
              'name': e['name']?.toString() ?? '',
            })
        .where((e) => e['id'] != null && e['id'].toString().isNotEmpty && e['id'] != 'official')
        .toList();

    final futures = manifests.map((m) async {
      final sourceId = m['id']!.toString();
      final sourceName = m['name']!.toString();
      final r = await SourceDataService.instance.search(sourceId, q, 1);
      final items = (r['items'] as List?) ?? const [];
      return items.whereType<Map>().map((e) {
        final item = Map<String, dynamic>.from(e);
        return _SearchHit(
          sourceId: sourceId,
          sourceName: sourceName.isNotEmpty ? sourceName : sourceId,
          comicId: item['id']?.toString() ?? '',
          title: item['title']?.toString() ?? '',
          cover: item['cover']?.toString() ?? item['coverUrl']?.toString() ?? '',
          author: item['author']?.toString() ?? '',
          description: item['description']?.toString() ?? '',
        );
      }).where((e) => e.title.isNotEmpty && e.comicId.isNotEmpty).toList();
    }).toList();

    final nested = await Future.wait(futures);
    return nested.expand((e) => e).toList();
  }

  List<_SearchGroup> _groupHits(List<_SearchHit> hits) {
    final map = <String, _SearchGroup>{};
    for (final hit in hits) {
      final key = _normalizeKey(hit.title);
      final group = map.putIfAbsent(key, () => _SearchGroup(title: hit.title, cover: hit.cover, author: hit.author, description: hit.description));
      group.hits.add(hit);
      if (group.cover.isEmpty && hit.cover.isNotEmpty) group.cover = hit.cover;
      if (group.author.isEmpty && hit.author.isNotEmpty) group.author = hit.author;
      if (group.description.isEmpty && hit.description.isNotEmpty) group.description = hit.description;
    }
    return map.values.toList()
      ..sort((a, b) => a.title.compareTo(b.title));
  }

  String _normalizeKey(String title) {
    return title
        .toLowerCase()
        .replaceAll(RegExp(r'[\s\-_.,:;!@#\$%\^&\*\(\)\[\]\{\}<>?/\\|`~]+'), '');
  }

  Future<void> _openGroup(_SearchGroup group) async {
    if (group.hits.isEmpty) return;
    if (group.hits.length == 1) {
      _openHit(group.hits.first);
      return;
    }
    final chosen = await showModalBottomSheet<_SearchHit>(
      context: context,
      backgroundColor: DS.surface1,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(18))),
      builder: (_) => SafeArea(
        child: ListView(
          shrinkWrap: true,
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
          children: [
            Text(group.title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: DS.textPrimary)),
            const SizedBox(height: 6),
            Text('选择漫画源', style: const TextStyle(fontSize: 12, color: DS.textTertiary)),
            const SizedBox(height: 12),
            ...group.hits.map((hit) => ListTile(
              contentPadding: EdgeInsets.zero,
              leading: ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: Container(
                  width: 42,
                  height: 56,
                  color: DS.surface2,
                  child: hit.cover.isNotEmpty
                      ? Image.network(hit.cover, fit: BoxFit.cover, errorBuilder: (_, __, ___) => const Icon(Icons.menu_book_rounded, color: DS.textDisabled))
                      : const Icon(Icons.menu_book_rounded, color: DS.textDisabled),
                ),
              ),
              title: Text(hit.sourceName, style: const TextStyle(fontSize: 14, color: DS.textPrimary, fontWeight: FontWeight.w600)),
              subtitle: Text(hit.author.isNotEmpty ? hit.author : hit.comicId, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12, color: DS.textTertiary)),
              trailing: const Icon(Icons.chevron_right_rounded, color: DS.textTertiary),
              onTap: () => Navigator.pop(context, hit),
            )),
          ],
        ),
      ),
    );
    if (chosen != null && mounted) {
      _openHit(chosen);
    }
  }

  void _openHit(_SearchHit hit) {
    HapticFeedback.lightImpact();
    if (hit.official) {
      GoRouter.of(context).push('/official/${hit.comicId}');
      return;
    }
    GoRouter.of(context).push('/source/${hit.sourceId}/comic/${hit.comicId}?sourceName=${Uri.encodeComponent(hit.sourceName)}');
  }

  @override
  Widget build(BuildContext context) {
    final title = _inSource ? (widget.sourceName.isNotEmpty ? widget.sourceName : '源内搜索') : (_sourceScope ? '发现搜索' : '搜索');
    final hint = _inSource
        ? '搜索 $title 内的漫画…'
        : (_sourceScope ? '搜索已安装源…' : '搜索漫界官方漫画…');

    return Scaffold(
      backgroundColor: DS.bg,
      appBar: AppBar(
        backgroundColor: DS.bg,
        elevation: 0,
        scrolledUnderElevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new_rounded, size: 20, color: DS.textPrimary),
          onPressed: () => Navigator.pop(context),
        ),
        title: Container(
          height: 40,
          padding: const EdgeInsets.symmetric(horizontal: DS.sp12),
          decoration: BoxDecoration(color: DS.surface2, borderRadius: BorderRadius.circular(20)),
          child: Row(children: [
            const Icon(Icons.search_rounded, size: 18, color: DS.textTertiary),
            const SizedBox(width: 6),
            Expanded(
              child: TextField(
                controller: _searchController,
                autofocus: true,
                style: const TextStyle(color: DS.textPrimary, fontSize: 15),
                decoration: InputDecoration(
                  hintText: hint,
                  hintStyle: const TextStyle(color: DS.textDisabled, fontSize: 14),
                  border: InputBorder.none,
                  isDense: true,
                  contentPadding: const EdgeInsets.symmetric(vertical: 10),
                ),
                onSubmitted: _onSearch,
                onChanged: (_) => setState(() {}),
              ),
            ),
            if (_searchController.text.isNotEmpty)
              GestureDetector(
                onTap: () {
                  _searchController.clear();
                  setState(() => _showResults = false);
                },
                child: const Icon(Icons.close_rounded, size: 16, color: DS.textTertiary),
              ),
          ]),
        ),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: DS.sp8),
            child: TextButton(
              onPressed: () => _onSearch(_searchController.text),
              child: const Text('搜索', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: DS.accent)),
            ),
          ),
        ],
      ),
      body: _showResults ? _buildResults() : _buildSearchHome(),
    );
  }

  Widget _buildSearchHome() {
    return ListView(
      padding: const EdgeInsets.all(DS.sp16),
      children: [
        Padding(
          padding: const EdgeInsets.only(bottom: DS.sp8),
          child: Text(
            _inSource
                ? '当前为源内搜索'
                : (_sourceScope ? '当前为发现搜索：已安装源' : '当前为官方搜索：优先漫界官方源'),
            style: const TextStyle(fontSize: 13, color: DS.textTertiary),
          ),
        ),
        if (_searchHistory.isNotEmpty) ...[
          Row(
            children: [
              const Text('搜索历史', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: DS.textPrimary)),
              const Spacer(),
              GestureDetector(
                onTap: () async {
                  final prefs = await SharedPreferences.getInstance();
                  await prefs.remove('search_history');
                  setState(() => _searchHistory.clear());
                },
                child: const Icon(Icons.delete_outline_rounded, size: 17, color: DS.textDisabled),
              ),
            ],
          ),
          const SizedBox(height: DS.sp12),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: _searchHistory.map((h) => _chip(h, onTap: () {
              _searchController.text = h;
              _onSearch(h);
            })).toList(),
          ),
        ],
      ],
    );
  }

  Widget _buildResults() {
    if (_isSearching) {
      return const Center(child: CircularProgressIndicator(color: DS.accent, strokeWidth: 2.5));
    }
    if (_error != null && _groups.isEmpty) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.cloud_off_rounded, size: 40, color: DS.textDisabled),
            const SizedBox(height: DS.sp12),
            Text(_error!, style: const TextStyle(color: DS.textSecondary, fontSize: 13), textAlign: TextAlign.center),
            const SizedBox(height: DS.sp16),
            FilledButton(onPressed: () => _onSearch(_searchController.text), child: const Text('重试')),
          ],
        ),
      );
    }
    if (_groups.isEmpty) {
      return const Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.search_off_rounded, size: 40, color: DS.textDisabled),
            SizedBox(height: DS.sp12),
            Text('未找到相关内容', style: TextStyle(color: DS.textTertiary, fontSize: 14)),
          ],
        ),
      );
    }
    return ListView.separated(
      padding: const EdgeInsets.all(DS.sp16),
      itemCount: _groups.length,
      separatorBuilder: (_, __) => const SizedBox(height: DS.sp12),
      itemBuilder: (_, i) {
        final group = _groups[i];
        return GestureDetector(
          onTap: () => _openGroup(group),
          child: Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: DS.surface1,
              borderRadius: BorderRadius.circular(DS.rMd),
              border: Border.all(color: DS.glassBorder, width: 0.5),
            ),
            child: Row(
              children: [
                ClipRRect(
                  borderRadius: BorderRadius.circular(10),
                  child: Container(
                    width: 56,
                    height: 76,
                    color: DS.surface2,
                    child: group.cover.isNotEmpty
                        ? Image.network(group.cover, fit: BoxFit.cover, errorBuilder: (_, __, ___) => const Icon(Icons.menu_book_rounded, color: DS.textDisabled))
                        : const Icon(Icons.menu_book_rounded, color: DS.textDisabled),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(group.title, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700, color: DS.textPrimary)),
                      const SizedBox(height: 4),
                      if (group.author.isNotEmpty)
                        Text(group.author, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12, color: DS.textTertiary)),
                      if (group.description.isNotEmpty) ...[
                        const SizedBox(height: 4),
                        Text(group.description, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12, color: DS.textDisabled, height: 1.35)),
                      ],
                      const SizedBox(height: 8),
                      Wrap(
                        spacing: 6,
                        runSpacing: 6,
                        children: group.hits.take(4).map((hit) => Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                          decoration: BoxDecoration(
                            color: hit.official ? DS.accent.withValues(alpha: 0.12) : DS.surface2,
                            borderRadius: BorderRadius.circular(999),
                          ),
                          child: Text(hit.sourceName, style: TextStyle(fontSize: 10, color: hit.official ? DS.accent : DS.textTertiary)),
                        )).toList(),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                Column(
                  children: [
                    Text('${group.hits.length}源', style: const TextStyle(fontSize: 11, color: DS.textTertiary)),
                    const SizedBox(height: 6),
                    const Icon(Icons.chevron_right_rounded, color: DS.textTertiary),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _chip(String label, {VoidCallback? onTap}) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(color: DS.surface2, borderRadius: BorderRadius.circular(DS.rMd)),
        child: Text(label, style: const TextStyle(color: DS.textSecondary, fontSize: 13)),
      ),
    );
  }
}

class _SearchHit {
  final String sourceId;
  final String sourceName;
  final String comicId;
  final String title;
  final String cover;
  final String author;
  final String description;
  final bool official;

  _SearchHit({
    required this.sourceId,
    required this.sourceName,
    required this.comicId,
    required this.title,
    required this.cover,
    required this.author,
    required this.description,
    this.official = false,
  });
}

class _SearchGroup {
  final String title;
  String cover;
  String author;
  String description;
  final List<_SearchHit> hits = [];

  _SearchGroup({required this.title, required this.cover, required this.author, required this.description});
}
