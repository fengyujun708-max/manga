import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:get_it/get_it.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../../../app/ds.dart';
import '../../../core/network/api_client.dart';

/// 内容管理（管理端）— 漫界官方：创建漫画 / 管理章节
/// 鉴权：JWT（ApiClient 自动携带），需 admin 角色（服务器校验）
class AdminOfficialPage extends StatefulWidget {
  const AdminOfficialPage({super.key});
  @override
  State<AdminOfficialPage> createState() => _AdminOfficialPageState();
}

class _AdminOfficialPageState extends State<AdminOfficialPage> {
  List<Map<String, dynamic>> _series = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/admin/official/series', params: {'page': 1, 'limit': 50});
      final data = res.data is Map ? Map<String, dynamic>.from(res.data as Map) : <String, dynamic>{};
      final items = (data['items'] as List?) ?? const [];
      _series = items.whereType<Map>().map((e) => Map<String, dynamic>.from(e)).toList();
    } catch (e) {
      _error = e.toString();
    }
    if (mounted) setState(() => _loading = false);
  }

  Future<void> _createSeries() async {
    final formKey = GlobalKey<_SeriesFormState>();
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        backgroundColor: DS.surface1,
        title: const Text('创建漫画', style: TextStyle(color: DS.textPrimary, fontSize: 17, fontWeight: FontWeight.w700)),
        content: _SeriesForm(key: formKey),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('取消')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('创建')),
        ],
      ),
    );
    if (ok != true) return;
    final state = formKey.currentState;
    if (state == null) return;
    final dto = state.value();
    if (dto['title'].toString().trim().isEmpty) {
      _snack('标题必填');
      return;
    }
    try {
      final api = GetIt.instance<ApiClient>();
      await api.post('/admin/official/series', data: dto);
      _snack('创建成功');
      _load();
    } catch (e) {
      _snack('创建失败: $e');
    }
  }

  void _openSeries(Map<String, dynamic> s) {
    Navigator.of(context).push(MaterialPageRoute(
      builder: (_) => _SeriesEpisodesPage(series: s, onChanged: _load),
    ));
  }

  Future<void> _deleteSeries(Map<String, dynamic> s) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        backgroundColor: DS.surface1,
        title: Text('删除《${s['title']}》？', style: const TextStyle(color: DS.textPrimary, fontSize: 16)),
        content: const Text('将删除全部章节及服务器上的图片文件，不可恢复', style: TextStyle(color: DS.textSecondary, fontSize: 13)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('取消')),
          FilledButton(style: FilledButton.styleFrom(backgroundColor: Colors.red), onPressed: () => Navigator.pop(context, true), child: const Text('删除')),
        ],
      ),
    );
    if (ok != true) return;
    try {
      final api = GetIt.instance<ApiClient>();
      await api.delete('/admin/official/series/${s['id']}');
      _snack('已删除');
      _load();
    } catch (e) {
      _snack('删除失败: $e');
    }
  }

  void _snack(String msg) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg, style: const TextStyle(fontSize: 13))));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: DS.bg,
      appBar: AppBar(
        backgroundColor: DS.bg, elevation: 0,
        title: const Text('内容管理 · 漫界官方', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w800, color: DS.textPrimary)),
        actions: [
          IconButton(icon: const Icon(Icons.refresh_rounded, color: DS.textSecondary), onPressed: _load),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        backgroundColor: DS.accent,
        onPressed: _createSeries,
        child: const Icon(Icons.add_rounded, color: Colors.white),
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_loading) return const Center(child: CircularProgressIndicator(color: DS.accent));
    if (_error != null) {
      return Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
        const Icon(Icons.cloud_off_rounded, size: 40, color: DS.textDisabled),
        const SizedBox(height: 10),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Text('加载失败\n$_error', textAlign: TextAlign.center, style: const TextStyle(fontSize: 12, color: DS.textTertiary)),
        ),
        const SizedBox(height: 12),
        FilledButton(onPressed: _load, child: const Text('重试')),
      ]));
    }
    if (_series.isEmpty) {
      return const Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
        Icon(Icons.library_add_rounded, size: 52, color: DS.textDisabled),
        SizedBox(height: 12),
        Text('还没有官方漫画', style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600, color: DS.textSecondary)),
        SizedBox(height: 4),
        Text('点右下角 + 创建第一部', style: TextStyle(fontSize: 12, color: DS.textTertiary)),
      ]));
    }
    return RefreshIndicator(
      onRefresh: _load,
      color: DS.accent,
      child: ListView.separated(
        physics: const BouncingScrollPhysics(),
        padding: const EdgeInsets.only(bottom: 100),
        itemCount: _series.length,
        separatorBuilder: (_, __) => const Divider(height: 1, color: DS.glassBorder),
        itemBuilder: (_, i) {
          final s = _series[i];
          final cover = (s['coverUrl'] ?? '').toString();
          return ListTile(
            onTap: () => _openSeries(s),
            leading: ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: SizedBox(
                width: 44, height: 60,
                child: cover.isNotEmpty
                    ? CachedNetworkImage(imageUrl: cover, fit: BoxFit.cover,
                        errorWidget: (_, __, ___) => Container(color: DS.surface2, child: const Icon(Icons.menu_book_rounded, color: DS.textDisabled)))
                    : Container(color: DS.surface2, child: const Icon(Icons.menu_book_rounded, color: DS.textDisabled)),
              ),
            ),
            title: Text(s['title'] ?? '', maxLines: 1, overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: DS.textPrimary)),
            subtitle: Text('${s['author'] ?? '未知作者'} · ${s['episodeCount'] ?? 0} 话',
              style: const TextStyle(fontSize: 12, color: DS.textTertiary)),
            trailing: IconButton(
              icon: const Icon(Icons.delete_outline_rounded, color: DS.textTertiary, size: 20),
              onPressed: () => _deleteSeries(s),
            ),
          );
        },
      ),
    );
  }
}

// ===== 创建表单 =====
class _SeriesForm extends StatefulWidget {
  const _SeriesForm({Key? key}) : super(key: key);
  @override
  State<_SeriesForm> createState() => _SeriesFormState();
}

class _SeriesFormState extends State<_SeriesForm> {
  final _title = TextEditingController();
  final _author = TextEditingController();
  final _desc = TextEditingController();
  final _genres = TextEditingController();
  final _cover = TextEditingController();
  bool _completed = false;

  Map<String, dynamic> value() => {
        'title': _title.text.trim(),
        'author': _author.text.trim(),
        'description': _desc.text.trim(),
        'genres': _genres.text.split(RegExp(r'[,，]')).map((e) => e.trim()).where((e) => e.isNotEmpty).toList(),
        'coverUrl': _cover.text.trim(),
        'status': _completed ? 'COMPLETED' : 'ONGOING',
      };

  @override
  Widget build(BuildContext context) {
    final style = const TextStyle(fontSize: 13, color: DS.textPrimary);
    return SingleChildScrollView(child: Column(mainAxisSize: MainAxisSize.min, children: [
      _field(_title, '标题 *', style),
      const SizedBox(height: 8),
      _field(_author, '作者', style),
      const SizedBox(height: 8),
      _field(_desc, '简介', style, maxLines: 3),
      const SizedBox(height: 8),
      _field(_genres, '标签（逗号分隔）', style),
      const SizedBox(height: 8),
      _field(_cover, '封面 URL（可留空后补）', style),
      const SizedBox(height: 8),
      Row(children: [
        const Text('已完结', style: TextStyle(fontSize: 13, color: DS.textSecondary)),
        Switch(value: _completed, onChanged: (v) => setState(() => _completed = v), activeColor: DS.accent),
      ]),
    ]));
  }

  Widget _field(TextEditingController c, String hint, TextStyle style, {int maxLines = 1}) {
    return TextField(
      controller: c, maxLines: maxLines,
      style: style,
      decoration: InputDecoration(
        hintText: hint, hintStyle: const TextStyle(fontSize: 13, color: DS.textTertiary),
        filled: true, fillColor: DS.surface2,
        contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: BorderSide.none),
      ),
    );
  }
}

// ===== 章节管理页 =====
class _SeriesEpisodesPage extends StatefulWidget {
  final Map<String, dynamic> series;
  final VoidCallback onChanged;
  const _SeriesEpisodesPage({required this.series, required this.onChanged});
  @override
  State<_SeriesEpisodesPage> createState() => _SeriesEpisodesPageState();
}

class _SeriesEpisodesPageState extends State<_SeriesEpisodesPage> {
  bool _loading = true;
  List<dynamic> _episodes = [];

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/official/series/${widget.series['id']}');
      final data = res.data is Map ? Map<String, dynamic>.from(res.data as Map) : <String, dynamic>{};
      _episodes = (data['episodes'] as List?) ?? [];
    } catch (_) {}
    if (mounted) setState(() => _loading = false);
  }

  Future<void> _addEpisode() async {
    final ctrlNo = TextEditingController();
    final ctrlTitle = TextEditingController();
    final ctrlImages = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        backgroundColor: DS.surface1,
        title: const Text('添加章节', style: TextStyle(color: DS.textPrimary, fontSize: 16, fontWeight: FontWeight.w700)),
        content: SingleChildScrollView(child: Column(mainAxisSize: MainAxisSize.min, children: [
          _in(ctrlNo, '章节号（如 1 / 2 / 3）', '数字，用于排序'),
          const SizedBox(height: 8),
          _in(ctrlTitle, '章节标题（可空）', '如：第 1 话'),
          const SizedBox(height: 8),
          _in(ctrlImages, '图片 URL（每行一个）', 'https://.../001.jpg\nhttps://.../002.jpg', maxLines: 6),
        ])),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('取消')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('保存')),
        ],
      ),
    );
    if (ok != true) return;
    final images = ctrlImages.text.split('\n').map((e) => e.trim()).where((e) => e.isNotEmpty).toList();
    final epNumber = int.tryParse(ctrlNo.text.trim());
    if (epNumber == null || epNumber <= 0) { _snack('章节号必须为正整数'); return; }
    try {
      final api = GetIt.instance<ApiClient>();
      await api.post('/admin/official/series/${widget.series['id']}/episodes', data: {
        'episodes': [
          {'epNumber': epNumber, 'title': ctrlTitle.text.trim(), 'images': images}
        ]
      });
      _snack('章节已保存');
      _load();
      widget.onChanged();
    } catch (e) {
      _snack('保存失败: $e');
    }
  }

  void _snack(String msg) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg, style: const TextStyle(fontSize: 13))));
  }

  Widget _in(TextEditingController c, String hint, String helper, {int maxLines = 1}) {
    return TextField(
      controller: c, maxLines: maxLines,
      style: const TextStyle(fontSize: 13, color: DS.textPrimary),
      decoration: InputDecoration(
        hintText: hint, helperText: helper,
        hintStyle: const TextStyle(fontSize: 12, color: DS.textTertiary),
        helperStyle: const TextStyle(fontSize: 11, color: DS.textDisabled),
        filled: true, fillColor: DS.surface2,
        contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: BorderSide.none),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final sorted = List<dynamic>.from(_episodes)
      ..sort((a, b) => ((b['epNumber'] ?? 0) as num).compareTo((a['epNumber'] ?? 0) as num));
    return Scaffold(
      backgroundColor: DS.bg,
      appBar: AppBar(
        backgroundColor: DS.bg, elevation: 0,
        title: Text(widget.series['title'] ?? '章节管理',
          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w800, color: DS.textPrimary)),
        actions: [
          IconButton(onPressed: _load, icon: const Icon(Icons.refresh_rounded, color: DS.textSecondary)),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        backgroundColor: DS.accent,
        onPressed: _addEpisode,
        child: const Icon(Icons.add_rounded, color: Colors.white),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: DS.accent))
          : sorted.isEmpty
              ? const Center(child: Text('暂无章节，点 + 添加', style: TextStyle(fontSize: 13, color: DS.textTertiary)))
              : RefreshIndicator(
                  onRefresh: _load,
                  color: DS.accent,
                  child: ListView.separated(
                    physics: const BouncingScrollPhysics(),
                    padding: const EdgeInsets.only(bottom: 100),
                    itemCount: sorted.length,
                    separatorBuilder: (_, __) => const Divider(height: 1, color: DS.glassBorder),
                    itemBuilder: (_, i) {
                      final ep = Map<String, dynamic>.from(sorted[i] as Map);
                      final images = (((ep['metadata'] as Map)?['images']) as List?) ?? [];
                      return ListTile(
                        leading: Container(
                          width: 40, height: 40,
                          alignment: Alignment.center,
                          decoration: BoxDecoration(color: DS.surface2, borderRadius: BorderRadius.circular(10)),
                          child: Text('${ep['epNumber'] ?? '?'}',
                            style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w700, color: DS.accent)),
                        ),
                        title: Text(ep['title']?.toString() ?? '第 ${ep['epNumber']} 话',
                          maxLines: 1, overflow: TextOverflow.ellipsis,
                          style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: DS.textPrimary)),
                        subtitle: Text('${images.length} 张图 · ${ep['availability'] ?? 'FREE'}',
                          style: const TextStyle(fontSize: 12, color: DS.textTertiary)),
                      );
                    },
                  ),
                ),
    );
  }
}