import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../app/theme/theme.dart';
import '../../app/components/manjie_button.dart';
import '../../app/components/manjie_card.dart';
import '../../app/components/manjie_toast.dart';
import '../../plugins/manga_source.dart';
import '../../plugins/runtime/js_engine.dart';
import '../bloc/source_bloc.dart';

class SourceManagerPage extends StatefulWidget {
  const SourceManagerPage({super.key});

  @override
  State<SourceManagerPage> createState() => _SourceManagerPageState();
}

class _SourceManagerPageState extends State<SourceManagerPage> with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
    context.read<SourceBloc>().add(SourceLoadRequested());
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('漫画源管理'),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(text: '已安装'),
            Tab(text: '源市场'),
            Tab(text: '添加源'),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => context.read<SourceBloc>().add(SourceRefreshRequested()),
            tooltip: '检查更新',
          ),
        ],
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _InstalledSourcesTab(),
          _SourceMarketTab(),
          _AddSourceTab(),
        ],
      ),
    );
  }
}

class _InstalledSourcesTab extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return BlocBuilder<SourceBloc, SourceState>(
      builder: (context, state) {
        if (state is SourceLoading) {
          return const Center(child: CircularProgressIndicator());
        }
        if (state is SourceError) {
          return Center(child: Text('加载失败: ${state.message}'));
        }
        if (state is SourceLoaded) {
          final sources = state.sources;
          if (sources.isEmpty) {
            return const Center(child: Text('暂无已安装源，去源市场添加'));
          }
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: sources.length,
            itemBuilder: (_, i) => _SourceCard(source: sources[i]),
          );
        }
        return const SizedBox();
      },
    );
  }
}

class _SourceCard extends StatelessWidget {
  final MangaSource source;
  const _SourceCard({required this.source});

  @override
  Widget build(BuildContext context) {
    return ManjieCard(
      margin: const EdgeInsets.only(bottom: 12),
      child: Row(
        children: [
          Container(
            width: 48, height: 48,
            decoration: BoxDecoration(
              color: source.enabled ? Theme.of(context).colorScheme.primary.withOpacity(0.2) : Theme.of(context).colorScheme.surface,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Center(child: Text(source.manifest.icon.isNotEmpty ? source.manifest.icon : '📚', style: const TextStyle(fontSize: 24))),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(source.manifest.name, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15)),
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: source.enabled ? Colors.green.withOpacity(0.15) : Colors.grey.withOpacity(0.15),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(source.enabled ? '已启用' : '已禁用', style: TextStyle(fontSize: 11, color: source.enabled ? Colors.green : Colors.grey)),
                    ),
                    const SizedBox(width: 8),
                    Text('v${source.manifest.version}', style: const TextStyle(color: Colors.grey, fontSize: 12)),
                  ],
                ),
                const SizedBox(height: 4),
                Text(source.manifest.description, style: const TextStyle(color: Colors.grey, fontSize: 13), maxLines: 1, overflow: TextOverflow.ellipsis),
              ],
            ),
          ),
          Switch(
            value: source.enabled,
            onChanged: (v) => context.read<SourceBloc>().add(SourceToggleEnabled(source.id, v)),
            activeColor: Theme.of(context).colorScheme.primary,
          ),
          PopupMenuButton<String>(
            onSelected: (value) {
              switch (value) {
                case 'update': context.read<SourceBloc>().add(SourceUpdateRequested(source.id)); break;
                case 'test': context.read<SourceBloc>().add(SourceTestRequested(source.id)); break;
                case 'delete': context.read<SourceBloc>().add(SourceDeleteRequested(source.id)); break;
              }
            },
            itemBuilder: (_) => [
              const PopupMenuItem(value: 'update', child: Row(children: [Icon(Icons.system_update, size: 20), SizedBox(width: 8), Text('检查更新')])),
              const PopupMenuItem(value: 'test', child: Row(children: [Icon(Icons.science, size: 20), SizedBox(width: 8), Text('测试源')])),
              const PopupMenuItem(value: 'delete', child: Row(children: [Icon(Icons.delete, size: 20, color: Colors.red), SizedBox(width: 8), Text('卸载', style: TextStyle(color: Colors.red))])),
            ],
          ),
        ],
      ),
    );
  }
}

class _SourceMarketTab extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return BlocBuilder<SourceBloc, SourceState>(
      builder: (context, state) {
        if (state is SourceMarketLoading) {
          return const Center(child: CircularProgressIndicator());
        }
        if (state is SourceMarketLoaded) {
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: state.sources.length,
            itemBuilder: (_, i) => _MarketSourceCard(source: state.sources[i]),
          );
        }
        return Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const CircularProgressIndicator(),
              const SizedBox(height: 16),
              TextButton(onPressed: () => context.read<SourceBloc>().add(SourceMarketLoadRequested()), child: const Text('加载源市场')),
            ],
          );
        }
      },
    );
  }
}

class _MarketSourceCard extends StatelessWidget {
  final SourceManifest source;
  const _MarketSourceCard({required this.source});

  @override
  Widget build(BuildContext context) {
    return ManjieCard(
      margin: const EdgeInsets.only(bottom: 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 52, height: 52,
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.primary.withOpacity(0.15),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Center(child: Text(source.icon.isNotEmpty ? source.icon : '📚', style: const TextStyle(fontSize: 26))),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(source.name, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15)),
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(color: Theme.of(context).colorScheme.primary.withOpacity(0.15), borderRadius: BorderRadius.circular(6)),
                      child: Text('v${source.version}', style: const TextStyle(color: Colors.blue, fontSize: 10)),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(source.description, style: const TextStyle(color: Colors.grey, fontSize: 13), maxLines: 2, overflow: TextOverflow.ellipsis),
                const SizedBox(height: 6),
                Row(
                  children: [
                    Icon(Icons.star, size: 14, color: Colors.amber.shade400),
                    const SizedBox(width: 2),
                    Text(source.rating.toString(), style: const TextStyle(color: Colors.amber, fontSize: 12)),
                    const SizedBox(width: 12),
                    Icon(Icons.download, size: 14, color: Colors.grey),
                    const SizedBox(width: 2),
                    Text('${source.downloads} 次下载', style: const TextStyle(color: Colors.grey, fontSize: 12)),
                  ],
                ),
              ],
            ),
          ),
          SizedBox(
            width: 80,
            height: 36,
            child: ElevatedButton(
              onPressed: () {
                // TODO: 安装源
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: Theme.of(context).colorScheme.primary,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
              ),
              child: const Text('安装', style: TextStyle(fontSize: 12)),
            ),
          ),
        ],
      ),
    );
  }
}

class _AddSourceTab extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final urlController = TextEditingController();
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('添加自定义源', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 16),
          TextField(
            controller: urlController,
            decoration: InputDecoration(
              labelText: '源地址',
              hintText: 'https://example.com/source.js 或 https://example.com/manifest.json',
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
              prefixIcon: const Icon(Icons.link),
            ),
          ),
          const SizedBox(height: 16),
          TextField(
            decoration: InputDecoration(
              labelText: '本地 JS 文件',
              hintText: '选择本地 .js 源文件',
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
              prefixIcon: const Icon(Icons.folder_open),
            ),
            readOnly: true,
            onTap: () {
              // TODO: 文件选择器
            },
          ),
          const SizedBox(height: 24),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: () {
                final url = urlController.text.trim();
                if (url.isNotEmpty) {
                  // TODO: 验证并安装源
                }
              },
              style: ElevatedButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 14)),
              child: const Text('添加源', style: TextStyle(fontSize: 16)),
            ),
          ),
          const SizedBox(height: 24),
          const Divider(),
          const Text('支持格式', style: TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          _FormatItem(icon: Icons.code, title: 'JavaScript 源', desc: '标准 JS 源文件，包含 search/getDetail/getChapters/getPages 等方法'),
          _FormatItem(icon: Icons.description, title: 'Manifest JSON', desc: '包含源元数据的 JSON 清单文件'),
        ],
      ),
    );
  }
}

class _FormatItem extends StatelessWidget {
  final IconData icon;
  final String title;
  final String desc;
  const _FormatItem({required this.icon, required this.title, required this.desc});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        children: [
          Icon(icon, color: Theme.of(context).colorScheme.primary, size: 24),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
                Text(desc, style: const TextStyle(color: Colors.grey, fontSize: 12)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}