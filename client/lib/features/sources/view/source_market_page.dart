import 'package:flutter/material.dart';
import '../../../app/theme/theme.dart';
import '../../../app/components/manjie_card.dart';
import '../../../app/components/manjie_button.dart';
import '../../../app/components/manjie_toast.dart';
import '../../../app/components/manjie_shimmer.dart';

class SourceMarketPage extends StatefulWidget {
  const SourceMarketPage({super.key});

  @override
  State<SourceMarketPage> createState() => _SourceMarketPageState();
}

class _SourceMarketPageState extends State<SourceMarketPage> {
  bool _loading = true;
  final List<_MarketSource> _sources = [];
  String _searchQuery = '';

  @override
  void initState() {
    super.initState();
    _loadSources();
  }

  Future<void> _loadSources() async {
    setState(() => _loading = true);

    // 模拟从注册表加载
    await Future.delayed(const Duration(seconds: 1));
    _sources.addAll([
      _MarketSource(
        id: 'bika', name: '哔咔漫画', version: '2.1.0',
        description: '哔咔漫画源，支持搜索、分类、排行',
        icon: '🔥', downloads: 15230, rating: 4.8, installed: true,
      ),
      _MarketSource(
        id: 'jm', name: '禁漫天堂', version: '3.0.5',
        description: '禁漫天堂源，支持搜索、分类、收藏',
        icon: '🔞', downloads: 12890, rating: 4.6, installed: true,
      ),
      _MarketSource(
        id: 'ehentai', name: 'E-Hentai', version: '1.5.0',
        description: 'E-Hentai 源，支持搜索、标签浏览',
        icon: '🌐', downloads: 8760, rating: 4.5, installed: false,
      ),
      _MarketSource(
        id: 'nhentai', name: 'NHentai', version: '2.0.0',
        description: 'NHentai 源，支持搜索、热门、随机',
        icon: '📖', downloads: 7650, rating: 4.3, installed: false,
      ),
      _MarketSource(
        id: 'copymanhua', name: '拷贝漫画', version: '1.8.0',
        description: '拷贝漫画源，支持搜索、分类、追更',
        icon: '📋', downloads: 6540, rating: 4.7, installed: false,
      ),
      _MarketSource(
        id: 'komiic', name: 'Komiic', version: '1.2.0',
        description: 'Komiic 源，支持搜索、收藏',
        icon: '📚', downloads: 4320, rating: 4.2, installed: false,
      ),
      _MarketSource(
        id: 'manhuaren', name: '漫画人', version: '1.0.0',
        description: '漫画人源，支持搜索、分类、排行',
        icon: '🎨', downloads: 3210, rating: 4.0, installed: false,
      ),
      _MarketSource(
        id: 'manhuagui', name: '漫画柜', version: '1.1.0',
        description: '漫画柜源，支持搜索、分类',
        icon: '🗄️', downloads: 2980, rating: 3.8, installed: false,
      ),
    ]);

    setState(() => _loading = false);
  }

  List<_MarketSource> get _filteredSources {
    if (_searchQuery.isEmpty) return _sources;
    return _sources.where((s) =>
      s.name.contains(_searchQuery) || s.id.contains(_searchQuery)
    ).toList();
  }

  void _installSource(_MarketSource source) {
    setState(() => source.installing = true);
    // 模拟安装
    Future.delayed(const Duration(seconds: 2), () {
      setState(() {
        source.installed = true;
        source.installing = false;
      });
      ManjieToast.success(context, '${source.name} 安装成功');
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('源市场')),
      body: Column(
        children: [
          // 搜索栏
          Container(
            padding: const EdgeInsets.all(16),
            child: TextField(
              decoration: InputDecoration(
                hintText: '搜索源名称...',
                prefixIcon: const Icon(Icons.search),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                filled: true,
                fillColor: AppTheme.surface,
              ),
              onChanged: (v) => setState(() => _searchQuery = v),
            ),
          ),
          // 源列表
          Expanded(
            child: _loading
              ? const ManjieGridShimmer()
              : RefreshIndicator(
                  onRefresh: _loadSources,
                  child: ListView.builder(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    itemCount: _filteredSources.length,
                    itemBuilder: (_, i) => _MarketSourceCard(
                      source: _filteredSources[i],
                      onInstall: () => _installSource(_filteredSources[i]),
                    ),
                  ),
                ),
          ),
        ],
      ),
    );
  }
}

class _MarketSourceCard extends StatelessWidget {
  final _MarketSource source;
  final VoidCallback onInstall;

  const _MarketSourceCard({required this.source, required this.onInstall});

  @override
  Widget build(BuildContext context) {
    return ManjieCard(
      margin: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 图标
          Container(
            width: 52, height: 52,
            decoration: BoxDecoration(
              color: AppTheme.primary.withOpacity(0.15),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Center(child: Text(source.icon, style: const TextStyle(fontSize: 26))),
          ),
          const SizedBox(width: 12),
          // 信息
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(source.name, style: const TextStyle(color: AppTheme.textPrimary, fontWeight: FontWeight.w600, fontSize: 15)),
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: AppTheme.primary.withOpacity(0.15),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text('v${source.version}',
                        style: const TextStyle(color: AppTheme.primary, fontSize: 10)),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(source.description, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                  maxLines: 2, overflow: TextOverflow.ellipsis),
                const SizedBox(height: 6),
                Row(
                  children: [
                    Icon(Icons.star, size: 14, color: Color(0xFFFFC107)),
                    const SizedBox(width: 2),
                    Text('${source.rating}', style: TextStyle(color: Color(0xFFFFC107), fontSize: 12)),
                    const SizedBox(width: 12),
                    Icon(Icons.download, size: 14, color: AppTheme.textSecondary),
                    const SizedBox(width: 2),
                    Text(_formatDownloads(source.downloads), style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          // 安装按钮
          if (source.installed)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
              decoration: BoxDecoration(
                color: AppTheme.accent.withOpacity(0.15),
                borderRadius: BorderRadius.circular(8),
              ),
              child: const Text('已安装', style: TextStyle(color: AppTheme.accent, fontSize: 12)),
            )
          else
            SizedBox(
              width: 60, height: 32,
              child: source.installing
                ? const Center(child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2)))
                : ElevatedButton(
                    onPressed: onInstall,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppTheme.primary,
                      padding: EdgeInsets.zero,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                    ),
                    child: const Text('安装', style: TextStyle(fontSize: 12, color: Colors.white)),
                  ),
            ),
        ],
      ),
    );
  }

  String _formatDownloads(int count) {
    if (count >= 10000) return '${(count / 10000).toStringAsFixed(1)}万';
    return count.toString();
  }
}

class _MarketSource {
  final String id;
  final String name;
  final String version;
  final String description;
  final String icon;
  final int downloads;
  final double rating;
  bool installed;
  bool installing;

  _MarketSource({
    required this.id,
    required this.name,
    required this.version,
    required this.description,
    required this.icon,
    required this.downloads,
    required this.rating,
    this.installed = false,
    this.installing = false,
  });
}