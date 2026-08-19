import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../app/theme/theme.dart';
import '../../app/components/manjie_button.dart';
import '../../app/components/manjie_toast.dart';
import '../reader/view/reader_page.dart';
import '../reader/models/reader_models.dart';

class ComicDetailPage extends StatefulWidget {
  final String comicId;
  const ComicDetailPage({super.key, required this.comicId});

  @override
  State<ComicDetailPage> createState() => _ComicDetailPageState();
}

class _ComicDetailPageState extends State<ComicDetailPage> {
  bool _isFavorited = false;
  bool _showFullDescription = false;
  bool _chapterDescending = true;
  int _selectedTab = 0; // 0: 章节, 1: 简介, 2: 相关

  // 模拟数据
  final _comic = {
    'title': '海贼王',
    'author': '尾田荣一郎',
    'status': '连载中',
    'rating': '9.5',
    'description': '拥有财富、名声、权力，这世界上的一切的男人 "海贼王"哥尔·D·罗杰，在被行刑受死之前说了一句话，让全世界的人都涌向了大海。"想要我的宝藏吗？如果想要的话，那就到海上去找吧，我全部都放在那里。"世界开始迎接"大海贼时代"的来临。\n\n路飞与草帽一伙的新冒险，和之国篇进入高潮！',
    'tags': '热血, 冒险, 友情, 战斗',
  };

  final List<Map<String, String>> _chapters = List.generate(120, (i) => {
    'id': 'ch${120 - i}',
    'title': '第 ${120 - i} 话',
    'updateTime': '2025-0${(i % 9) + 1}-${(i % 28) + 1}',
    'pages': '${(i % 5) + 15}',
  });

  final List<Map<String, String>> _recommendations = [
    {'title': '咒术回战', 'author': '芥见下下', 'color': '0xFFE74C3C'},
    {'title': '鬼灭之刃', 'author': '吾峠呼世晴', 'color': '0xFF3498DB'},
    {'title': '一拳超人', 'author': 'ONE', 'color': '0xFFE67E22'},
    {'title': '进击的巨人', 'author': '谏山创', 'color': '0xFF2ECC71'},
    {'title': '全职猎人', 'author': '冨樫义博', 'color': '0xFF673AB7'},
    {'title': '钢之炼金术师', 'author': '荒川弘', 'color': '0xFF795548'},
  ];

  List<Map<String, String>> get _displayChapters {
    final sorted = List<Map<String, String>>.from(_chapters);
    if (!_chapterDescending) sorted.sort((a, b) => b['id']!.compareTo(a['id']!));
    return sorted;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: CustomScrollView(
        slivers: [
          // 顶部封面
          SliverAppBar(
            expandedHeight: 300,
            pinned: true,
            stretch: true,
            backgroundColor: AppTheme.surface,
            flexibleSpace: FlexibleSpaceBar(
              background: Stack(
                fit: StackFit.expand,
                children: [
                  // 封面渐变背景
                  Container(
                    decoration: const BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                        colors: [Color(0xFF6C5CE7), Color(0xFF0F3460), Color(0xFF0A0A1A)],
                      ),
                    ),
                  ),
                  // 装饰
                  Positioned(
                    right: -60, top: -60,
                    child: Container(
                      width: 250, height: 250,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: Colors.white.withOpacity(0.05),
                      ),
                    ),
                  ),
                  // 底部渐变
                  Positioned(
                    left: 0, right: 0, bottom: 0,
                    child: Container(
                      height: 120,
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          begin: Alignment.topCenter,
                          end: Alignment.bottomCenter,
                          colors: [Colors.transparent, AppTheme.background],
                        ),
                      ),
                    ),
                  ),
                  // 返回按钮
                  Positioned(
                    top: MediaQuery.of(context).padding.top + 8,
                    left: 8,
                    child: IconButton(
                      icon: Container(
                        padding: const EdgeInsets.all(8),
                        decoration: BoxDecoration(
                          color: Colors.black.withOpacity(0.4),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(Icons.arrow_back, color: Colors.white, size: 22),
                      ),
                      onPressed: () => Navigator.of(context).pop(),
                    ),
                  ),
                  // 分享按钮
                  Positioned(
                    top: MediaQuery.of(context).padding.top + 8,
                    right: 8,
                    child: IconButton(
                      icon: Container(
                        padding: const EdgeInsets.all(8),
                        decoration: BoxDecoration(
                          color: Colors.black.withOpacity(0.4),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(Icons.share, color: Colors.white, size: 20),
                      ),
                      onPressed: () {},
                    ),
                  ),
                ],
              ),
            ),
          ),

          // 漫画信息
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 标题
                  Text(_comic['title']!, style: Theme.of(context).textTheme.headlineLarge?.copyWith(
                    fontWeight: FontWeight.bold,
                  )),
                  const SizedBox(height: 8),

                  // 作者 + 状态
                  Row(
                    children: [
                      _InfoChip(icon: Icons.person, text: _comic['author']!),
                      const SizedBox(width: 12),
                      _InfoChip(icon: Icons.circle, text: _comic['status']!, color: AppTheme.accent),
                      const SizedBox(width: 12),
                      _InfoChip(icon: Icons.star, text: _comic['rating']!, color: Colors.amber),
                    ],
                  ),
                  const SizedBox(height: 12),

                  // 标签
                  Wrap(
                    spacing: 6, runSpacing: 6,
                    children: _comic['tags']!.split(', ').map((t) => Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: AppTheme.primary.withOpacity(0.15),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Text(t, style: const TextStyle(color: AppTheme.primary, fontSize: 12)),
                    )).toList(),
                  ),
                  const SizedBox(height: 16),

                  // 操作按钮
                  Row(
                    children: [
                      Expanded(
                        child: ManjieButton(
                          label: '开始阅读',
                          icon: Icons.play_arrow,
                          onPressed: () => _openReader(context, 0),
                        ),
                      ),
                      const SizedBox(width: 12),
                      ManjieButton(
                        label: '',
                        icon: _isFavorited ? Icons.favorite : Icons.favorite_border,
                        variant: ManjieButtonVariant.outlined,
                        width: 50,
                        onPressed: () {
                          setState(() => _isFavorited = !_isFavorited);
                          ManjieToast.success(context, _isFavorited ? '已收藏' : '已取消收藏');
                        },
                      ),
                      const SizedBox(width: 8),
                      ManjieButton(
                        label: '',
                        icon: Icons.download_outlined,
                        variant: ManjieButtonVariant.outlined,
                        width: 50,
                        onPressed: () => ManjieToast.show(context, '开始下载...'),
                      ),
                    ],
                  ),

                  // 简介
                  const SizedBox(height: 20),
                  GestureDetector(
                    onTap: () => setState(() => _showFullDescription = !_showFullDescription),
                    child: Text(
                      _comic['description']!,
                      style: const TextStyle(color: AppTheme.textSecondary, fontSize: 14, height: 1.6),
                      maxLines: _showFullDescription ? null : 3,
                      overflow: _showFullDescription ? null : TextOverflow.ellipsis,
                    ),
                  ),
                  if (!_showFullDescription)
                    TextButton(
                      onPressed: () => setState(() => _showFullDescription = true),
                      child: const Text('展开全部 ↓', style: TextStyle(color: AppTheme.primary, fontSize: 13)),
                    ),
                ],
              ),
            ),
          ),

          // Tab 切换
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Row(
                children: [
                  _TabButton(label: '章节', count: _chapters.length, selected: _selectedTab == 0, onTap: () => setState(() => _selectedTab = 0)),
                  const SizedBox(width: 16),
                  _TabButton(label: '相关推荐', count: _recommendations.length, selected: _selectedTab == 1, onTap: () => setState(() => _selectedTab = 1)),
                ],
              ),
            ),
          ),
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              child: Divider(color: AppTheme.divider, height: 1),
            ),
          ),

          // Tab 内容
          if (_selectedTab == 0) ..._buildChapterList(),
          if (_selectedTab == 1) ..._buildRecommendations(),
        ],
      ),
    );
  }

  List<Widget> _buildChapterList() {
    return [
      // 排序切换
      SliverToBoxAdapter(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Row(
            children: [
              const Text('全部章节', style: TextStyle(color: AppTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.w600)),
              const Spacer(),
              GestureDetector(
                onTap: () => setState(() => _chapterDescending = !_chapterDescending),
                child: Row(
                  children: [
                    Text(_chapterDescending ? '倒序' : '正序', style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
                    const SizedBox(width: 4),
                    Icon(_chapterDescending ? Icons.arrow_downward : Icons.arrow_upward, size: 16, color: AppTheme.textSecondary),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
      // 章节列表
      SliverList(
        delegate: SliverChildBuilderDelegate(
          (_, i) {
            final ch = _displayChapters[i];
            return _ChapterTile(
              title: ch['title']!,
              updateTime: ch['updateTime']!,
              pages: ch['pages']!,
              isRead: i < 5,
              onTap: () => _openReader(context, i),
            );
          },
          childCount: _displayChapters.length,
        ),
      ),
      const SliverToBoxAdapter(child: SizedBox(height: 80)),
    ];
  }

  List<Widget> _buildRecommendations() {
    return [
      SliverGrid(
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 3,
          childAspectRatio: 0.7,
          crossAxisSpacing: 8,
          mainAxisSpacing: 8,
        ),
        delegate: SliverChildBuilderDelegate(
          (_, i) {
            final item = _recommendations[i];
            return GestureDetector(
              onTap: () {},
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Container(
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(10),
                        color: Color(int.parse(item['color']!, radix: 16)),
                      ),
                      child: Center(
                        child: Text(item['title']!.substring(0, 1), style: const TextStyle(fontSize: 36, color: Colors.white24)),
                      ),
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(item['title']!, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 13), maxLines: 1),
                  Text(item['author']!, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 11)),
                ],
              ),
            );
          },
          childCount: _recommendations.length,
        ),
      ),
      const SliverToBoxAdapter(child: SizedBox(height: 80)),
    ];
  }

  void _openReader(BuildContext context, int chapterIndex) {
    final chapters = _chapters.reversed.toList();
    Navigator.of(context).push(MaterialPageRoute(
      builder: (_) => ReaderPage(
        comicId: widget.comicId,
        comicTitle: _comic['title']!,
        chapters: chapters.map((c) => Chapter(
          id: c['id']!,
          title: c['title']!,
          pageCount: int.parse(c['pages']!),
        )).toList(),
        initialChapter: chapterIndex,
      ),
    ));
  }
}

class _InfoChip extends StatelessWidget {
  final IconData icon;
  final String text;
  final Color? color;

  const _InfoChip({required this.icon, required this.text, this.color});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 14, color: color ?? AppTheme.textSecondary),
        const SizedBox(width: 4),
        Text(text, style: TextStyle(color: color ?? AppTheme.textSecondary, fontSize: 13)),
      ],
    );
  }
}

class _TabButton extends StatelessWidget {
  final String label;
  final int count;
  final bool selected;
  final VoidCallback onTap;

  const _TabButton({required this.label, required this.count, required this.selected, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Column(
        children: [
          Row(
            children: [
              Text(label, style: TextStyle(
                color: selected ? AppTheme.primary : AppTheme.textSecondary,
                fontSize: 15, fontWeight: selected ? FontWeight.w600 : FontWeight.normal,
              )),
              const SizedBox(width: 4),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                decoration: BoxDecoration(
                  color: selected ? AppTheme.primary.withOpacity(0.15) : AppTheme.divider,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text('$count', style: TextStyle(
                  color: selected ? AppTheme.primary : AppTheme.textSecondary, fontSize: 11)),
              ),
            ],
          ),
          if (selected)
            Container(
              margin: const EdgeInsets.only(top: 6),
              width: 24, height: 3,
              decoration: BoxDecoration(
                color: AppTheme.primary,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
        ],
      ),
    );
  }
}

class _ChapterTile extends StatelessWidget {
  final String title;
  final String updateTime;
  final String pages;
  final bool isRead;
  final VoidCallback onTap;

  const _ChapterTile({
    required this.title, required this.updateTime, required this.pages,
    required this.isRead, required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      onTap: onTap,
      leading: Container(
        width: 4, height: 4,
        decoration: BoxDecoration(
          color: isRead ? AppTheme.accent : AppTheme.primary,
          shape: BoxShape.circle,
        ),
      ),
      title: Text(title, style: TextStyle(
        color: isRead ? AppTheme.textSecondary : AppTheme.textPrimary,
        fontWeight: isRead ? FontWeight.normal : FontWeight.w500,
        fontSize: 14,
      )),
      subtitle: Text('$pages页 · $updateTime', style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
      trailing: isRead
        ? const Icon(Icons.check_circle, size: 18, color: AppTheme.accent)
        : const Icon(Icons.chevron_right, size: 18, color: AppTheme.textSecondary),
    );
  }
}