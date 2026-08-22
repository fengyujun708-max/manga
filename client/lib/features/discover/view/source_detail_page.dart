import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'dart:ui';
import '../../../app/ds.dart';
import '../../../plugins/source_data_service.dart';
import '../../../plugins/source_routes_service.dart';

/// 源详情 — 沉浸式板块浏览 + 搜索 + 分类
class SourceDetailPage extends StatefulWidget {
  final String sourceId;
  final String sourceName;
  const SourceDetailPage({super.key, required this.sourceId, this.sourceName = ''});
  @override
  State<SourceDetailPage> createState() => _SourceDetailPageState();
}

class _SourceDetailPageState extends State<SourceDetailPage> {
  bool _loading = true;
  String? _error;
  List<dynamic> _sections = [];
  List<dynamic> _searchResults = [];
  bool _searching = false;
  String _mode = '';
  bool _showSearch = false;
  final _searchCtrl = TextEditingController();

  @override
  void initState() { super.initState(); _load(); }
  @override
  void dispose() { _searchCtrl.dispose(); super.dispose(); }

  void _showRouteSheet() {
    showModalBottomSheet(
      context: context,
      backgroundColor: DS.surface1,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(DS.rLg))),
      builder: (_) => _RouteSheet(sourceId: widget.sourceId, onSwitched: () { _load(); }),
    );
  }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    final result = await SourceDataService.instance.explore(widget.sourceId);
    if (!mounted) return;
    setState(() { _sections = result['sections'] ?? []; _mode = result['mode'] ?? ''; _loading = false; if ((result['error'] ?? '').isNotEmpty) _error = result['error']; });
  }

  Future<void> _search(String q) async {
    if (q.trim().isEmpty) { setState(() { _searching = false; _searchResults = []; }); return; }
    setState(() => _searching = true);
    final result = await SourceDataService.instance.search(widget.sourceId, q.trim(), 1);
    if (!mounted) return;
    setState(() { _searchResults = result['items'] ?? []; _searching = false; });
  }

  void _enterComic(String id) => GoRouter.of(context).push('/source/${widget.sourceId}/comic/$id');

  @override
  Widget build(BuildContext context) {
    final name = widget.sourceName.isNotEmpty ? widget.sourceName : widget.sourceId;
    final isSearch = _searching || _searchResults.isNotEmpty;
    return Scaffold(
      backgroundColor: DS.bg,
      body: CustomScrollView(
        physics: const BouncingScrollPhysics(),
        slivers: [
          SliverAppBar(
            expandedHeight: 80, pinned: true, stretch: true,
            backgroundColor: Colors.transparent, elevation: 0,
            leading: GestureDetector(
              onTap: () => context.pop(),
              child: Container(margin: const EdgeInsets.all(8), decoration: BoxDecoration(color: DS.glassFill, shape: BoxShape.circle), child: const Icon(Icons.arrow_back_ios_new_rounded, size: 18, color: DS.textPrimary)),
            ),
            title: Text(name, style: DS.headline),
            actions: [
              GestureDetector(
                onTap: _showRouteSheet,
                child: Container(margin: const EdgeInsets.only(right: DS.sp8), padding: const EdgeInsets.all(6), decoration: const BoxDecoration(color: DS.glassFill, shape: BoxShape.circle), child: const Icon(Icons.network_check_rounded, size: 20, color: DS.textPrimary)),
              ),
              if (_mode.isNotEmpty)
                Container(
                  margin: const EdgeInsets.only(right: DS.sp8),
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(color: _mode == 'local' ? DS.success.withValues(alpha: 0.1) : DS.accent.withValues(alpha: 0.1), borderRadius: BorderRadius.circular(DS.rSm)),
                  child: Text(_mode == 'local' ? '本地' : '代理', style: TextStyle(fontSize: 10, color: _mode == 'local' ? DS.success : DS.accent)),
                ),
              GestureDetector(
                onTap: () => setState(() => _showSearch = !_showSearch),
                child: Container(margin: const EdgeInsets.only(right: DS.sp12), padding: const EdgeInsets.all(6), decoration: const BoxDecoration(color: DS.glassFill, shape: BoxShape.circle), child: Icon(_showSearch ? Icons.close_rounded : Icons.search_rounded, size: 20, color: DS.textPrimary)),
              ),
            ],
            bottom: _showSearch ? PreferredSize(
              preferredSize: const Size.fromHeight(56),
              child: Padding(padding: const EdgeInsets.fromLTRB(DS.sp16, 0, DS.sp16, DS.sp12),
                child: Glass(radius: DS.rMd, padding: const EdgeInsets.symmetric(horizontal: DS.sp16),
                  child: Row(children: [
                    const Icon(Icons.search_rounded, size: 20, color: DS.textTertiary),
                    const SizedBox(width: 10),
                    Expanded(child: TextField(controller: _searchCtrl, autofocus: true, style: const TextStyle(fontSize: 14, color: DS.textPrimary), decoration: const InputDecoration(hintText: '搜索漫画', hintStyle: TextStyle(color: DS.textTertiary, fontSize: 14), border: InputBorder.none, isDense: true), onSubmitted: _search, textInputAction: TextInputAction.search)),
                    if (_searching) const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 1.5, color: DS.accent)),
                  ]),
                ),
              ),
            ) : null,
          ),

          if (_loading)
            const SliverFillRemaining(child: Center(child: CircularProgressIndicator(color: DS.accent, strokeWidth: 2)))
          else if (_error != null && _sections.isEmpty)
            SliverFillRemaining(child: EmptyState(icon: Icons.cloud_off_rounded, title: '加载失败', subtitle: '$_error\n提示：海外源需设备直连，可开启 VPN 后重试', actionLabel: '重试', onAction: _load))
          else if (isSearch && _searchResults.isEmpty)
            const SliverFillRemaining(child: EmptyState(icon: Icons.search_off_rounded, title: '未找到漫画'))
          else if (isSearch)
            SliverPadding(padding: const EdgeInsets.all(DS.sp16), sliver: SliverGrid(
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 3, childAspectRatio: 0.52, crossAxisSpacing: DS.sp12, mainAxisSpacing: DS.sp12),
              delegate: SliverChildBuilderDelegate((_, i) {
                final item = _searchResults[i] as Map;
                return ComicCard(cover: (item['cover'] ?? '').toString(), title: (item['title'] ?? '').toString(), subtitle: (item['subTitle'] ?? '').toString(), onTap: () => _enterComic((item['id'] ?? '').toString()));
              }, childCount: _searchResults.length),
            ))
          else ...[
            SliverList(delegate: SliverChildBuilderDelegate((_, i) {
              if (i >= _sections.length) return null;
              final sec = _sections[i];
              final title = sec['title']?.toString() ?? '板块';
              final items = (sec['items'] as List?) ?? [];
              return Padding(
                padding: const EdgeInsets.only(bottom: DS.sp20),
                child: _HorizontalSection(title: title, items: items.cast<Map>(), onMore: () => GoRouter.of(context).push('/source/${widget.sourceId}/category?initial=$title'), onTap: _enterComic),
              );
            }, childCount: _sections.length)),
            SliverToBoxAdapter(
              child: Padding(padding: const EdgeInsets.fromLTRB(DS.sp16, 0, DS.sp16, DS.sp16),
                child: GestureDetector(
                  onTap: () => GoRouter.of(context).push('/source/${widget.sourceId}/category'),
                  child: Glass(radius: DS.rMd, padding: const EdgeInsets.symmetric(horizontal: DS.sp16, vertical: 14),
                    child: Row(children: [
                      Container(width: 36, height: 36, decoration: BoxDecoration(color: DS.accent, borderRadius: BorderRadius.circular(10)), child: const Icon(Icons.category_rounded, size: 18, color: Colors.white)),
                      const SizedBox(width: DS.sp12),
                      const Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Text('全部分类', style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600, color: DS.textPrimary)),
                        Text('浏览更多漫画', style: TextStyle(fontSize: 12, color: DS.textTertiary)),
                      ])),
                      const Icon(Icons.chevron_right_rounded, color: DS.textTertiary),
                    ]),
                  ),
                ),
              ),
            ),
            const SliverToBoxAdapter(child: SizedBox(height: 120)),
          ],
        ],
      ),
    );
  }
}

class _HorizontalSection extends StatelessWidget {
  final String title;
  final List<Map> items;
  final VoidCallback onMore;
  final void Function(String) onTap;
  const _HorizontalSection({required this.title, required this.items, required this.onMore, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Padding(padding: const EdgeInsets.symmetric(horizontal: DS.sp16), child: Row(children: [
        Text(title, style: DS.title),
        const Spacer(),
        GestureDetector(onTap: onMore, child: Row(children: [Text('更多', style: const TextStyle(fontSize: 13, color: DS.textTertiary)), const Icon(Icons.chevron_right_rounded, size: 16, color: DS.textTertiary)])),
      ])),
      const SizedBox(height: DS.sp12),
      SizedBox(height: 220, child: ListView.separated(
        padding: const EdgeInsets.symmetric(horizontal: DS.sp16),
        scrollDirection: Axis.horizontal, physics: const BouncingScrollPhysics(),
        itemCount: items.length, separatorBuilder: (_, __) => const SizedBox(width: DS.sp12),
        itemBuilder: (_, i) {
          final item = items[i];
          return ComicCard(
            cover: (item['cover'] ?? '').toString(),
            title: (item['title'] ?? '').toString(),
            subtitle: (item['subTitle'] ?? '').toString(),
            width: 130, onTap: () => onTap((item['id'] ?? '').toString()),
          );
        },
      )),
    ]);
  }
}
class _RouteSheet extends StatefulWidget {
  final String sourceId;
  final VoidCallback onSwitched;
  const _RouteSheet({required this.sourceId, required this.onSwitched});
  @override
  State<_RouteSheet> createState() => _RouteSheetState();
}

class _RouteSheetState extends State<_RouteSheet> {
  List<RouteProbe>? _results;
  bool _probing = true;
  String? _current;

  @override
  void initState() {
    super.initState();
    SourceRoutesService.instance.warm().then((_) async {
      _current = (SourceRoutesService.instance.getOverrides(widget.sourceId)['domains'] ?? '').toString();
      await _probe();
    });
  }

  Future<void> _probe() async {
    setState(() => _probing = true);
    final hosts = await SourceRoutesService.instance.extractHosts(widget.sourceId);
    _results = hosts.isEmpty ? [] : await SourceRoutesService.instance.probe(hosts);
    if (mounted) setState(() => _probing = false);
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(DS.sp16, DS.sp16, DS.sp16, DS.sp24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(children: [
              const Icon(Icons.lan_rounded, size: 18, color: DS.accent),
              const SizedBox(width: 8),
              Text('网络检测 · ${widget.sourceId}', style: const TextStyle(color: DS.textPrimary, fontSize: 15, fontWeight: FontWeight.w700)),
              const Spacer(),
              GestureDetector(onTap: _probe, child: const Icon(Icons.refresh_rounded, size: 20, color: DS.textTertiary)),
            ]),
            const SizedBox(height: DS.sp12),
            if (_probing)
              const Padding(padding: EdgeInsets.symmetric(vertical: 32), child: Center(child: CircularProgressIndicator(color: DS.accent, strokeWidth: 2)))
            else if (_results == null || _results!.isEmpty)
              Padding(padding: const EdgeInsets.symmetric(vertical: 24), child: Text('未在源脚本中发现候选线路', style: TextStyle(color: DS.textTertiary, fontSize: 13))),
            if (!_probing && _results != null && _results!.isNotEmpty)
              Flexible(child: SingleChildScrollView(child: Column(children: [
                for (final r in _results!)
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    dense: true,
                    leading: Icon(r.ok ? Icons.check_circle_rounded : Icons.cancel_rounded, size: 20, color: r.ok ? ((r.latencyMs ?? 0) < 800 ? DS.success : DS.warning) : DS.error),
                    title: Text(r.host, style: const TextStyle(color: DS.textPrimary, fontSize: 13)),
                    subtitle: r.ok ? Text('${r.latencyMs} ms', style: const TextStyle(color: DS.textTertiary, fontSize: 11)) : null,
                    trailing: _current == r.host ? const Icon(Icons.radio_button_checked_rounded, size: 18, color: DS.accent) : null,
                    onTap: () async {
                      await SourceRoutesService.instance.setOverride(widget.sourceId, 'domains', r.host);
                      setState(() => _current = r.host);
                      widget.onSwitched();
                    },
                  ),
              ]))),
            if (!_probing && (_results?.any((r) => r.ok) ?? false))
              Padding(
                padding: const EdgeInsets.only(top: DS.sp8),
                child: SizedBox(width: double.infinity, height: 44, child: ElevatedButton.icon(
                  style: ElevatedButton.styleFrom(backgroundColor: DS.accent, foregroundColor: Colors.white, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(DS.rMd))),
                  icon: const Icon(Icons.bolt_rounded, size: 18), label: const Text('自动选择最优', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
                  onPressed: () async {
                    await SourceRoutesService.instance.autoSelect(widget.sourceId);
                    setState(() => _current = SourceRoutesService.instance.getOverrides(widget.sourceId)['domains']?.toString());
                    widget.onSwitched();
                    if (context.mounted) Navigator.pop(context);
                  },
                )),
              ),
          ],
        ),
      ),
    );
  }
}
