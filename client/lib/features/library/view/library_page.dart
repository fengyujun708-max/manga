import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../app/theme/theme.dart';
import '../../app/components/manjie_card.dart';
import '../../app/components/manjie_empty_state.dart';

class LibraryPage extends StatefulWidget {
  const LibraryPage({super.key});

  @override
  State<LibraryPage> createState() => _LibraryPageState();
}

class _LibraryPageState extends State<LibraryPage> with SingleTickerProviderStateMixin {
  late TabController _tabController;
  bool _isGrid = true;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
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
        title: const Text('书架'),
        actions: [
          IconButton(
            icon: Icon(_isGrid ? Icons.view_list : Icons.grid_view),
            onPressed: () => setState(() => _isGrid = !_isGrid),
            tooltip: _isGrid ? '列表模式' : '网格模式',
          ),
        ],
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: AppTheme.primary,
          labelColor: AppTheme.primary,
          unselectedLabelColor: AppTheme.textSecondary,
          tabs: const [
            Tab(text: '收藏', icon: Icon(Icons.bookmark, size: 18)),
            Tab(text: '历史', icon: Icon(Icons.history, size: 18)),
            Tab(text: '下载', icon: Icon(Icons.download, size: 18)),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _FavoritesTab(isGrid: _isGrid),
          _HistoryTab(isGrid: _isGrid),
          _DownloadsTab(isGrid: _isGrid),
        ],
      ),
    );
  }
}

// ====== 收藏 Tab ======
class _FavoritesTab extends StatelessWidget {
  final bool isGrid;
  const _FavoritesTab({required this.isGrid});

  @override
  Widget build(BuildContext context) {
    // 模拟收藏数据
    final favorites = List.generate(8, (i) => _MockComic(
      title: '收藏漫画 ${i + 1}',
      author: '作者 ${i + 1}',
      chapter: '${(i + 1) * 10}',
      updateTime: '${i + 1}天前',
      hasUpdate: i < 3,
    ));

    if (favorites.isEmpty) {
      return const ManjieEmptyState(
        icon: Icons.bookmark_border,
        title: '还没有收藏',
        subtitle: '去发现页找找喜欢的漫画吧',
      );
    }

    if (isGrid) {
      return _buildGrid(context, favorites);
    }
    return _buildList(context, favorites);
  }

  Widget _buildGrid(BuildContext context, List<_MockComic> items) {
    return GridView.builder(
      padding: const EdgeInsets.all(16),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 3, childAspectRatio: 0.7, crossAxisSpacing: 8, mainAxisSpacing: 8,
      ),
      itemCount: items.length,
      itemBuilder: (_, i) => _ComicGridItem(comic: items[i], onTap: () => context.push('/comic/${i + 1}')),
    );
  }

  Widget _buildList(BuildContext context, List<_MockComic> items) {
    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: items.length,
      itemBuilder: (_, i) => _ComicListItem(comic: items[i], onTap: () => context.push('/comic/${i + 1}')),
    );
  }
}

// ====== 历史 Tab ======
class _HistoryTab extends StatelessWidget {
  final bool isGrid;
  const _HistoryTab({required this.isGrid});

  @override
  Widget build(BuildContext context) {
    final history = List.generate(5, (i) => _MockComic(
      title: '阅读记录 ${i + 1}',
      author: '作者 ${i + 1}',
      chapter: '${(i + 1) * 20}',
      progress: 0.3 + (i * 0.15),
      updateTime: '${i + 1}小时前',
    ));

    if (isGrid) {
      return GridView.builder(
        padding: const EdgeInsets.all(16),
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 3, childAspectRatio: 0.7, crossAxisSpacing: 8, mainAxisSpacing: 8,
        ),
        itemCount: history.length,
        itemBuilder: (_, i) => _ComicGridItem(comic: history[i], showProgress: true),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: history.length + 1,
      itemBuilder: (_, i) {
        if (i == 0) {
          return Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Row(
              children: [
                const Text('阅读记录', style: TextStyle(color: AppTheme.textPrimary, fontSize: 16, fontWeight: FontWeight.w600)),
                const Spacer(),
                TextButton.icon(
                  icon: const Icon(Icons.delete_sweep, size: 18),
                  label: const Text('清空'),
                  onPressed: () {},
                  style: TextButton.styleFrom(foregroundColor: Colors.red),
                ),
              ],
            ),
          );
        }
        return _ComicListItem(comic: history[i - 1], showProgress: true);
      },
    );
  }
}

// ====== 下载 Tab ======
class _DownloadsTab extends StatelessWidget {
  final bool isGrid;
  const _DownloadsTab({required this.isGrid});

  @override
  Widget build(BuildContext context) {
    return const ManjieEmptyState(
      icon: Icons.download_outlined,
      title: '暂无下载',
      subtitle: '下载的漫画将在这里显示',
    );
  }
}

// ====== 通用组件 ======

class _ComicGridItem extends StatelessWidget {
  final _MockComic comic;
  final bool showProgress;
  final VoidCallback? onTap;

  const _ComicGridItem({required this.comic, this.showProgress = false, this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Stack(
              children: [
                Container(
                  width: double.infinity,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(10),
                    color: AppTheme.surfaceLight,
                    border: Border.all(color: AppTheme.divider.withOpacity(0.3)),
                  ),
                  child: Center(
                    child: Text(comic.title.substring(0, 1),
                      style: const TextStyle(fontSize: 32, color: AppTheme.textSecondary.withOpacity(0.3))),
                  ),
                ),
                if (comic.hasUpdate)
                  Positioned(
                    top: 6, right: 6,
                    child: Container(
                      width: 10, height: 10,
                      decoration: BoxDecoration(
                        color: Colors.red, shape: BoxShape.circle,
                        border: Border.all(color: AppTheme.background, width: 2),
                      ),
                    ),
                  ),
                if (showProgress)
                  Positioned(
                    left: 0, right: 0, bottom: 0,
                    child: ClipRRect(
                      borderRadius: const BorderRadius.only(bottomLeft: Radius.circular(10), bottomRight: Radius.circular(10)),
                      child: LinearProgressIndicator(
                        value: comic.progress,
                        backgroundColor: Colors.black26,
                        valueColor: const AlwaysStoppedAnimation<Color>(AppTheme.primary),
                        minHeight: 3,
                      ),
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 6),
          Text(comic.title, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 13),
            maxLines: 1, overflow: TextOverflow.ellipsis),
          Text('更新至 ${comic.chapter} 话', style: const TextStyle(color: AppTheme.textSecondary, fontSize: 11)),
        ],
      ),
    );
  }
}

class _ComicListItem extends StatelessWidget {
  final _MockComic comic;
  final bool showProgress;
  final VoidCallback? onTap;

  const _ComicListItem({required this.comic, this.showProgress = false, this.onTap});

  @override
  Widget build(BuildContext context) {
    return ManjieCard(
      margin: const EdgeInsets.only(bottom: 8),
      onTap: onTap,
      child: Row(
        children: [
          // 封面
          Container(
            width: 60, height: 80,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(8),
              color: AppTheme.surfaceLight,
              border: Border.all(color: AppTheme.divider.withOpacity(0.3)),
            ),
            child: Center(child: Text(comic.title.substring(0, 1),
              style: const TextStyle(fontSize: 24, color: AppTheme.textSecondary.withOpacity(0.3)))),
          ),
          const SizedBox(width: 12),
          // 信息
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(comic.title, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.w500),
                        maxLines: 1, overflow: TextOverflow.ellipsis),
                    ),
                    if (comic.hasUpdate)
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                          color: Colors.red.withOpacity(0.15),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: const Text('更新', style: TextStyle(color: Colors.red, fontSize: 10)),
                      ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(comic.author, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
                if (showProgress) ...[
                  const SizedBox(height: 6),
                  ClipRRect(
                    borderRadius: BorderRadius.circular(2),
                    child: LinearProgressIndicator(
                      value: comic.progress,
                      backgroundColor: AppTheme.divider,
                      valueColor: const AlwaysStoppedAnimation<Color>(AppTheme.primary),
                      minHeight: 3,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text('${(comic.progress * 100).toInt()}%', style: const TextStyle(color: AppTheme.textSecondary, fontSize: 11)),
                ] else ...[
                  const SizedBox(height: 4),
                  Text('更新至 ${comic.chapter} 话 · ${comic.updateTime}',
                    style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
                ],
              ],
            ),
          ),
          const Icon(Icons.chevron_right, color: AppTheme.textSecondary, size: 20),
        ],
      ),
    );
  }
}

class _MockComic {
  final String title;
  final String author;
  final String chapter;
  final double progress;
  final String updateTime;
  final bool hasUpdate;

  _MockComic({
    required this.title,
    required this.author,
    required this.chapter,
    this.progress = 0,
    this.updateTime = '',
    this.hasUpdate = false,
  });
}