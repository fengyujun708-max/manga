import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../../../plugins/source_data_service.dart';
import '../../../app/ds.dart';

/// 搜索页 —— 支持全局搜索 / 源内搜索（传入 sourceId 时只搜该源）
class SearchPage extends StatefulWidget {
  final String? sourceId;
  final String sourceName;
  const SearchPage({super.key, this.sourceId, this.sourceName = ''});
  @override
  State<SearchPage> createState() => _SearchPageState();
}

class _SearchPageState extends State<SearchPage> {
  final _searchController = TextEditingController();
  bool _isSearching = false;
  bool _showResults = false;
  String? _error;

  final List<String> _searchHistory = [];
  List<Map<String, dynamic>> _results = [];

  @override
  void initState() {
    super.initState();
    _loadHistory();
  }

  @override
  void dispose() { _searchController.dispose(); super.dispose(); }

  Future<void> _loadHistory() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final h = prefs.getString('search_history') ?? '[]';
      final list = (jsonDecode(h) as List).map((e) => e.toString()).toList();
      if (mounted) setState(() { _searchHistory..clear()..addAll(list); });
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

  // 是否源内搜索
  bool get _inSource => widget.sourceId != null && widget.sourceId!.isNotEmpty;

  Future<void> _onSearch(String query) async {
    final q = query.trim();
    if (q.isEmpty) return;
    FocusScope.of(context).unfocus();
    setState(() { _isSearching = true; _showResults = true; _error = null; });
    _saveHistory(q);

    try {
      final result = await SourceDataService.instance.search(widget.sourceId ?? '', q, 1);
      if (!mounted) return;
      setState(() {
        _results = (result['items'] as List?)?.map((e) => Map<String, dynamic>.from(e as Map)).toList() ?? [];
        _isSearching = false;
        if (_results.isEmpty && result['error'] != null) _error = result['error'].toString();
      });
    } catch (e) {
      if (!mounted) return;
      setState(() { _isSearching = false; _error = e.toString(); });
    }
  }

  @override
  Widget build(BuildContext context) {
    final title = _inSource ? (widget.sourceName.isNotEmpty ? widget.sourceName : '源内搜索') : '搜索';
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
              decoration: InputDecoration(
                hintText: _inSource ? '搜索 $title 内的漫画…' : '搜索漫画、作者、标签…',
                hintStyle: const TextStyle(color: DS.textDisabled, fontSize: 14),
                border: InputBorder.none, isDense: true,
                contentPadding: const EdgeInsets.symmetric(vertical: 10)),
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
        if (!_inSource)
          const Padding(padding: EdgeInsets.only(bottom: DS.sp8),
            child: Text('请输入关键词搜索（当前为全局搜索）', style: TextStyle(fontSize: 13, color: DS.textTertiary))),
        if (_searchHistory.isNotEmpty) ...[
          Row(children: [
            const Text('搜索历史', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: DS.textPrimary)),
            const Spacer(),
            GestureDetector(onTap: () async {
              final prefs = await SharedPreferences.getInstance();
              await prefs.remove('search_history');
              setState(() => _searchHistory.clear());
            }, child: const Icon(Icons.delete_outline_rounded, size: 17, color: DS.textDisabled)),
          ]),
          const SizedBox(height: DS.sp12),
          Wrap(spacing: 8, runSpacing: 8, children: _searchHistory.map((h) => _chip(h,
              onTap: () { _searchController.text = h; _onSearch(h); })).toList()),
        ],
      ],
    );
  }

  Widget _buildResults() {
    if (_isSearching) {
      return const Center(child: CircularProgressIndicator(color: DS.accent, strokeWidth: 2.5));
    }
    if (_error != null) {
      return Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
        const Icon(Icons.cloud_off_rounded, size: 40, color: DS.textDisabled),
        const SizedBox(height: DS.sp12),
        Text(_error!, style: const TextStyle(color: DS.textSecondary, fontSize: 13), textAlign: TextAlign.center),
        const SizedBox(height: DS.sp16),
        FilledButton(onPressed: () => _onSearch(_searchController.text), child: const Text('重试')),
      ]));
    }
    if (_results.isEmpty) {
      return Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
        const Icon(Icons.search_off_rounded, size: 40, color: DS.textDisabled),
        const SizedBox(height: DS.sp12),
        const Text('未找到相关内容', style: TextStyle(color: DS.textTertiary, fontSize: 14)),
        const Text('试试其他关键词，或检查是否需要 VPN', style: TextStyle(color: DS.textDisabled, fontSize: 12)),
      ]));
    }
    return GridView.builder(
      padding: const EdgeInsets.all(DS.sp16),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 3, childAspectRatio: 0.55, crossAxisSpacing: DS.sp12, mainAxisSpacing: DS.sp16),
      itemCount: _results.length,
      itemBuilder: (_, i) {
        final item = _results[i];
        final cover = (item['cover'] ?? item['coverUrl'] ?? '').toString();
        final id = (item['id'] ?? '').toString();
        final title = (item['title'] ?? '').toString();
        final author = (item['author'] ?? '').toString();
        return GestureDetector(
          onTap: () {
            if (id.isNotEmpty && _inSource) {
              GoRouter.of(context).push('/source/${widget.sourceId}/comic/$id');
            }
          },
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Expanded(child: Container(
              decoration: BoxDecoration(borderRadius: BorderRadius.circular(DS.rSm), color: DS.surface1),
              clipBehavior: Clip.antiAlias,
              child: cover.isNotEmpty
                ? Image.network(cover, fit: BoxFit.cover, width: double.infinity,
                    errorBuilder: (_, __, ___) => Container(color: DS.surface2, child: const Icon(Icons.menu_book_rounded, color: DS.textDisabled)))
                : Container(color: DS.surface2, child: const Icon(Icons.menu_book_rounded, color: DS.textDisabled)),
            )),
            const SizedBox(height: 6),
            Text(title, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500, color: DS.textPrimary)),
            if (author.isNotEmpty)
              Text(author, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 11, color: DS.textTertiary)),
          ]),
        );
      },
    );
  }

  Widget _chip(String label, {VoidCallback? onTap}) {
    return GestureDetector(onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(color: DS.surface2, borderRadius: BorderRadius.circular(DS.rMd)),
        child: Text(label, style: const TextStyle(color: DS.textSecondary, fontSize: 13)),
      ));
  }
}