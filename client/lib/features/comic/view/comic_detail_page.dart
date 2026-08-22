import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import '../../../app/ds.dart';
import '../../reader/view/reader_page.dart';
import '../../reader/models/reader_models.dart';

/// 漫画详情页 — Cinematic Hero
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
  int _tab = 0; // 0 章节 / 1 相关

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

  static const _recommendations = [
    {'title': '咒术回战', 'author': '芥见下下'},
    {'title': '鬼灭之刃', 'author': '吾峠呼世晴'},
    {'title': '一拳超人', 'author': 'ONE'},
    {'title': '进击的巨人', 'author': '谏山创'},
    {'title': '全职猎人', 'author': '冨樫义博'},
    {'title': '钢之炼金术师', 'author': '荒川弘'},
  ];

  List<Map<String, String>> get _displayChapters {
    final sorted = List<Map<String, String>>.from(_chapters);
    if (!_chapterDescending) sorted.sort((a, b) => b['id']!.compareTo(a['id']!));
    return sorted;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: DS.bg,
      body: CustomScrollView(
        physics: const BouncingScrollPhysics(),
        slivers: [
          // ── 电影感 Hero ──
          SliverAppBar(
            expandedHeight: 340,
            pinned: true, stretch: true,
            backgroundColor: DS.bg, elevation: 0,
            leading: _circleBtn(Icons.arrow_back_ios_new_rounded, () => Navigator.of(context).pop()),
            actions: [_circleBtn(Icons.share_rounded, ())],
            flexibleSpace: FlexibleSpaceBar(
              background: Stack(fit: StackFit.expand, children: [
                // 封面占位背景
                Container(color: DS.surface1),
                Center(child: Icon(Icons.menu_book_rounded, size: 140, color: Colors.white.withValues(alpha: 0.04))),
                // 底部渐隐入 bg
                const Positioned(left: 0, right: 0, bottom: 0, height: 200, child: DecoratedBox(decoration: BoxDecoration(gradient: DS.heroScrim))),
                // 标题区
                Positioned(left: DS.sp16, right: DS.sp16, bottom: DS.sp16, child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Row(children: [
                      Container(padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(color: DS.accent, borderRadius: BorderRadius.circular(DS.rSm)),
                        child: Text(_comic['status']!, style: const TextStyle(fontSize: 10, fontWeight: FontWeight.w700, color: Colors.white))),
                      const SizedBox(width: 8),
                      const Icon(Icons.star_rounded, size: 14, color: DS.warning),
                      const SizedBox(width: 2),
                      Text(_comic['rating']!, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: DS.warning)),
                    ]),
                    const SizedBox(height: 8),
                    Text(_comic['title']!, style: DS.display),
                    const SizedBox(height: 4),
                    Text(_comic['author']!, style: DS.bodySec),
                  ],
                )),
              ]),
            ),
          ),

          // ── 信息区 ──
          SliverToBoxAdapter(child: Padding(
            padding: const EdgeInsets.fromLTRB(DS.sp16, DS.sp8, DS.sp16, 0),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              // 标签
              Wrap(spacing: 6, runSpacing: 6, children: _comic['tags']!.split(', ').map((t) => Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                decoration: BoxDecoration(color: DS.glassFill, borderRadius: BorderRadius.circular(DS.rSm)),
                child: Text(t, style: const TextStyle(fontSize: 12, color: DS.textSecondary)),
              )).toList()),
              const SizedBox(height: DS.sp16),

              // 操作区
              Row(children: [
                Expanded(child: SpringButton(
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  onPressed: () => _openReader(context, 0),
                  child: const Row(mainAxisSize: MainAxisSize.min, children: [
                    Icon(Icons.play_arrow_rounded, size: 22, color: Colors.white),
                    SizedBox(width: 6), Text('开始阅读'),
                  ]),
                )),
                const SizedBox(width: DS.sp12),
                _roundAction(icon: _isFavorited ? Icons.favorite_rounded : Icons.favorite_border_rounded,
                  active: _isFavorited, onTap: () {
                    HapticFeedback.lightImpact();
                    setState(() => _isFavorited = !_isFavorited);
                  }),
                const SizedBox(width: DS.sp8),
                _roundAction(icon: Icons.download_outlined, onTap: () {}),
              ]),

              // 简介
              const SizedBox(height: DS.sp20),
              GestureDetector(
                onTap: () => setState(() => _showFullDescription = !_showFullDescription),
                child: Text(_comic['description']!, style: DS.bodySec, maxLines: _showFullDescription ? null : 3,
                    overflow: _showFullDescription ? null : TextOverflow.ellipsis),
              ),
              if (!_showFullDescription)
                GestureDetector(onTap: () => setState(() => _showFullDescription = true),
                  child: const Padding(padding: EdgeInsets.only(top: 6),
                    child: Text('展开全部', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: DS.textPrimary)))),
            ]),
          )),

          // ── Tab 切换 ──
          SliverToBoxAdapter(child: Padding(
            padding: const EdgeInsets.fromLTRB(DS.sp16, DS.sp24, DS.sp16, DS.sp12),
            child: Row(children: [
              _tabBtn('章节', _chapters.length, 0),
              const SizedBox(width: DS.sp20),
              _tabBtn('相关推荐', _recommendations.length, 1),
            ]),
          )),

          if (_tab == 0) ..._buildChapters() else ..._buildRelated(),
        ],
      ),
    );
  }

  List<Widget> _buildChapters() {
    return [
      SliverToBoxAdapter(child: Padding(
        padding: const EdgeInsets.fromLTRB(DS.sp16, 0, DS.sp16, DS.sp12),
        child: Row(children: [
          Text('共 ${_chapters.length} 话', style: DS.caption),
          const Spacer(),
          GestureDetector(
            onTap: () { HapticFeedback.selectionClick(); setState(() => _chapterDescending = !_chapterDescending); },
            child: Row(children: [
              Text(_chapterDescending ? '倒序' : '正序', style: const TextStyle(fontSize: 13, color: DS.textTertiary)),
              Icon(_chapterDescending ? Icons.arrow_downward_rounded : Icons.arrow_upward_rounded, size: 15, color: DS.textTertiary),
            ]),
          ),
        ]),
      )),
      SliverPadding(
        padding: const EdgeInsets.fromLTRB(DS.sp16, 0, DS.sp16, 100),
        sliver: SliverGrid(
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 2, childAspectRatio: 3.4, crossAxisSpacing: DS.sp8, mainAxisSpacing: DS.sp8),
          delegate: SliverChildBuilderDelegate((_, i) {
            final ch = _displayChapters[i];
            final isRead = i < 5;
            return GestureDetector(
              onTap: () => _openReader(context, i),
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: DS.sp12),
                decoration: BoxDecoration(
                  color: isRead ? DS.surface1 : DS.surface2,
                  borderRadius: BorderRadius.circular(DS.rMd),
                  border: Border.all(color: i == 0 ? DS.accent.withValues(alpha: 0.5) : Colors.transparent, width: 1),
                ),
                child: Row(children: [
                  Expanded(child: Text(ch['title']!, maxLines: 1, overflow: TextOverflow.ellipsis,
                      style: TextStyle(fontSize: 13, fontWeight: FontWeight.w500, color: isRead ? DS.textTertiary : DS.textPrimary))),
                  Text('${ch['pages']}P', style: DS.micro),
                ]),
              ),
            );
          }, childCount: _displayChapters.length),
        ),
      ),
    ];
  }

  List<Widget> _buildRelated() {
    return [
      SliverPadding(
        padding: const EdgeInsets.fromLTRB(DS.sp16, 0, DS.sp16, 100),
        sliver: SliverGrid(
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 3, childAspectRatio: 0.55, crossAxisSpacing: DS.sp12, mainAxisSpacing: DS.sp16),
          delegate: SliverChildBuilderDelegate((_, i) {
            final item = _recommendations[i];
            return ComicCard(
              cover: '', title: item['title']!, subtitle: item['author'],
              onTap: () {},
            );
          }, childCount: _recommendations.length),
        ),
      ),
    ];
  }

  Widget _tabBtn(String label, int count, int idx) {
    final selected = _tab == idx;
    return GestureDetector(
      onTap: () { HapticFeedback.selectionClick(); setState(() => _tab = idx); },
      child: AnimatedContainer(duration: DS.durStd, curve: DS.cStd,
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
        decoration: BoxDecoration(
          color: selected ? DS.surface3 : Colors.transparent,
          borderRadius: BorderRadius.circular(DS.rMd),
        ),
        child: Row(mainAxisSize: MainAxisSize.min, children: [
          Text(label, style: TextStyle(fontSize: 14, fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
              color: selected ? DS.textPrimary : DS.textTertiary)),
          const SizedBox(width: 5),
          Text('$count', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: selected ? DS.accent : DS.textDisabled)),
        ]),
      ),
    );
  }

  Widget _roundAction({required IconData icon, bool active = false, VoidCallback? onTap}) {
    return GestureDetector(
      onTap: onTap,
      child: Container(width: 50, height: 50,
        decoration: BoxDecoration(
          color: active ? DS.accent.withValues(alpha: 0.15) : DS.surface2,
          borderRadius: BorderRadius.circular(DS.rMd),
          border: Border.all(color: active ? DS.accent.withValues(alpha: 0.4) : Colors.transparent, width: 1),
        ),
        child: Icon(icon, size: 22, color: active ? DS.accent : DS.textSecondary)),
    );
  }

  Widget _circleBtn(IconData icon, VoidCallback onTap) {
    return Padding(padding: const EdgeInsets.all(6), child: GestureDetector(
      onTap: onTap,
      child: Container(width: 36, height: 36,
        decoration: BoxDecoration(color: Colors.black.withValues(alpha: 0.35), shape: BoxShape.circle),
        child: Icon(icon, size: 18, color: Colors.white)),
    ));
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
