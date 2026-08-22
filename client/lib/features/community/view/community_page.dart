import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import '../../../app/ds.dart';

/// 社区页 — Feed 流
class CommunityPage extends StatefulWidget {
  const CommunityPage({super.key});
  @override
  State<CommunityPage> createState() => _CommunityPageState();
}

class _CommunityPageState extends State<CommunityPage> {
  int _tab = 0;
  static const _tabs = ['推荐', '最新', '求漫', '求源'];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: DS.bg,
      body: CustomScrollView(
        physics: const BouncingScrollPhysics(),
        slivers: [
          SliverAppBar(
            floating: true, snap: true,
            backgroundColor: Colors.transparent, elevation: 0,
            title: Row(children: [
              const Text('社区', style: DS.headline),
              const Spacer(),
              _iconBtn(Icons.edit_square_rounded, () => context.push('/community/create')),
            ]),
          ),

          // 分类胶囊
          SliverToBoxAdapter(child: SizedBox(
            height: 40,
            child: ListView.separated(
              padding: const EdgeInsets.symmetric(horizontal: DS.sp16),
              scrollDirection: Axis.horizontal,
              itemCount: _tabs.length,
              separatorBuilder: (_, __) => const SizedBox(width: DS.sp8),
              itemBuilder: (_, i) {
                final selected = _tab == i;
                return GestureDetector(
                  onTap: () { HapticFeedback.selectionClick(); setState(() => _tab = i); },
                  child: AnimatedContainer(duration: DS.durStd, curve: DS.cStd,
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: selected ? DS.accent : DS.surface2,
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(_tabs[i], style: TextStyle(fontSize: 13,
                        fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
                        color: selected ? Colors.white : DS.textSecondary)),
                  ),
                );
              },
            ),
          )),

          // 发布入口
          SliverToBoxAdapter(child: Padding(
            padding: const EdgeInsets.fromLTRB(DS.sp16, DS.sp12, DS.sp16, DS.sp4),
            child: GestureDetector(
              onTap: () => context.push('/community/create'),
              child: Glass(radius: DS.rLg, blur: 24, padding: const EdgeInsets.all(DS.sp12),
                child: Row(children: [
                  _avatar('我', 34),
                  const SizedBox(width: DS.sp12),
                  Expanded(child: Text('分享你的想法...', style: const TextStyle(fontSize: 14, color: DS.textDisabled))),
                  Container(width: 30, height: 30,
                    decoration: BoxDecoration(color: DS.accent.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(9)),
                    child: const Icon(Icons.add_rounded, size: 18, color: DS.accent)),
                ])),
            ),
          )),

          // 列表
          if (_tab < 2) _postFeed() else _requestFeed(),
        ],
      ),
    );
  }

  Widget _postFeed() {
    final titles = ['推荐一部超好看的漫画《XXX》', '求问这部漫画的名字', '最新话分析：剧情走向预测', '大家的书架里都有什么？', '漫画推荐：这几部必看'];
    final contents = ['最近发现了一部宝藏漫画，画风精美剧情紧凑...', '在某个地方看到这部漫画但忘记名字了...', '最新一话的信息量很大，来聊聊我的看法...', '分享一下我最近在追的漫画，大家有推荐的吗？', '整理了几部个人觉得非常值得看的漫画'];
    return SliverPadding(
      padding: const EdgeInsets.fromLTRB(DS.sp16, DS.sp8, DS.sp16, 120),
      sliver: SliverList(
        delegate: SliverChildBuilderDelegate((_, i) {
          return Padding(padding: const EdgeInsets.only(bottom: DS.sp12),
            child: _PostCard(post: _MockPost(
              id: '$i', user: '用户${i + 1}',
              title: titles[i % 5], content: contents[i % 5],
              likes: (i + 1) * 3, comments: i + 1, time: '${i + 1}小时前',
              tags: i % 3 == 0 ? ['讨论'] : (i % 3 == 1 ? ['求漫'] : ['分析']),
              hasComicRef: i == 0 || i == 4,
            )));
        }, childCount: 15),
      ),
    );
  }

  Widget _requestFeed() {
    final isManga = _tab == 2;
    return SliverPadding(
      padding: const EdgeInsets.fromLTRB(DS.sp16, DS.sp8, DS.sp16, 120),
      sliver: SliverList(
        delegate: SliverChildBuilderDelegate((_, i) {
          final solved = i % 4 >= 2;
          return Padding(padding: const EdgeInsets.only(bottom: DS.sp12), child: Glass(
            radius: DS.rLg, blur: 24, padding: const EdgeInsets.all(DS.sp16),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Row(children: [
                Container(padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(color: DS.glassFillStrong, borderRadius: BorderRadius.circular(6)),
                  child: Text(isManga ? '求漫' : '求源', style: const TextStyle(fontSize: 10, fontWeight: FontWeight.w700, color: DS.textSecondary))),
                const SizedBox(width: 8),
                Text('用户${i + 1}', style: DS.caption),
                const Spacer(),
                Text('${i + 1}天前', style: DS.micro),
              ]),
              const SizedBox(height: DS.sp8),
              Text(isManga ? '求漫画：《XXX》找了很久没找到' : '求一个能看XXX的源',
                  style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600, color: DS.textPrimary)),
              const SizedBox(height: 4),
              Text(isManga ? '记得是几年前看过的，主角有特殊能力...' : '之前用的源最近失效了，求推荐稳定的替代源',
                  style: DS.bodySec, maxLines: 2, overflow: TextOverflow.ellipsis),
              const SizedBox(height: DS.sp12),
              Row(children: [
                const Icon(Icons.chat_bubble_outline_rounded, size: 15, color: DS.textTertiary),
                const SizedBox(width: 4),
                Text('${i + 1} 个回答', style: DS.caption),
                const Spacer(),
                Container(padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(color: solved ? DS.success.withValues(alpha: 0.1) : DS.warning.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(6)),
                  child: Text(solved ? '已解决' : '寻找中',
                      style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: solved ? DS.success : DS.warning))),
              ]),
            ])),
          );
        }, childCount: 8),
      ),
    );
  }

  Widget _iconBtn(IconData icon, VoidCallback tap) {
    return GestureDetector(
      onTap: () { HapticFeedback.lightImpact(); tap(); },
      child: Container(width: 38, height: 38, decoration: BoxDecoration(color: DS.glassFill, borderRadius: BorderRadius.circular(12)), child: Icon(icon, size: 19, color: DS.textPrimary)),
    );
  }

  static Widget _avatar(String name, double size) => Container(
    width: size, height: size,
    decoration: const BoxDecoration(shape: BoxShape.circle, gradient: LinearGradient(colors: [DS.surface3, DS.surface1])),
    child: Center(child: Text(name.characters.first, style: TextStyle(fontSize: size * 0.4, fontWeight: FontWeight.w700, color: DS.textSecondary))),
  );
}

class _MockPost {
  final String id, user, title, content, time;
  final int likes, comments;
  final List<String> tags;
  final bool hasComicRef;
  _MockPost({required this.id, required this.user, required this.title, required this.content,
      required this.likes, required this.comments, required this.time, required this.tags, this.hasComicRef = false});
}

class _PostCard extends StatelessWidget {
  final _MockPost post;
  const _PostCard({required this.post});

  @override
  Widget build(BuildContext context) {
    return Glass(
      radius: DS.rLg, blur: 24, padding: const EdgeInsets.all(DS.sp16),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Container(width: 36, height: 36,
            decoration: const BoxDecoration(shape: BoxShape.circle, gradient: LinearGradient(colors: [DS.surface3, DS.surface1])),
            child: Center(child: Text(post.user.characters.last, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700, color: DS.textSecondary)))),
          const SizedBox(width: 10),
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(post.user, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: DS.textPrimary)),
            Text(post.time, style: DS.micro),
          ])),
          for (final t in post.tags) Container(
            margin: const EdgeInsets.only(left: 4),
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
            decoration: BoxDecoration(color: DS.glassFillStrong, borderRadius: BorderRadius.circular(6)),
            child: Text(t, style: const TextStyle(fontSize: 10, fontWeight: FontWeight.w600, color: DS.textTertiary)),
          ),
        ]),
        const SizedBox(height: DS.sp12),
        Text(post.title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: DS.textPrimary, height: 1.35)),
        const SizedBox(height: 6),
        Text(post.content, style: DS.bodySec, maxLines: 3, overflow: TextOverflow.ellipsis),
        if (post.hasComicRef) ...[
          const SizedBox(height: DS.sp12),
          Container(
            padding: const EdgeInsets.all(DS.sp12),
            decoration: BoxDecoration(color: DS.surface2, borderRadius: BorderRadius.circular(DS.rMd)),
            child: Row(children: [
              ClipRRect(borderRadius: BorderRadius.circular(6),
                child: Container(width: 44, height: 60, color: DS.surface3,
                    child: const Icon(Icons.menu_book_rounded, size: 20, color: DS.textDisabled))),
              const SizedBox(width: DS.sp12),
              Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: const [
                Text('海贼王', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: DS.textPrimary)),
                SizedBox(height: 2),
                Text('★ 9.5 · 更新至 1120 话', style: TextStyle(fontSize: 12, color: DS.textTertiary)),
              ])),
              const Icon(Icons.chevron_right_rounded, size: 18, color: DS.accent),
            ]),
          ),
        ],
        const SizedBox(height: DS.sp12),
        Row(children: [
          _Action(icon: Icons.favorite_border_rounded, count: post.likes),
          const SizedBox(width: DS.sp20),
          _Action(icon: Icons.chat_bubble_outline_rounded, count: post.comments),
          const SizedBox(width: DS.sp20),
          const _Action(icon: Icons.bookmark_border_rounded),
        ]),
      ]),
    );
  }
}

class _Action extends StatelessWidget {
  final IconData icon;
  final int? count;
  const _Action({required this.icon, this.count});
  @override
  Widget build(BuildContext context) {
    return Row(mainAxisSize: MainAxisSize.min, children: [
      Icon(icon, size: 17, color: DS.textTertiary),
      if (count != null) ...[const SizedBox(width: 5), Text('$count', style: DS.caption)],
    ]);
  }
}

// ====== 创建帖子页面 ======
class CreatePostPage extends StatefulWidget {
  const CreatePostPage({super.key});
  @override
  State<CreatePostPage> createState() => _CreatePostPageState();
}

class _CreatePostPageState extends State<CreatePostPage> {
  final _titleController = TextEditingController();
  final _contentController = TextEditingController();
  String _selectedType = '讨论';

  static const _types = ['讨论', '推荐', '求漫', '求源', '分析', '吐槽'];

  @override
  void dispose() { _titleController.dispose(); _contentController.dispose(); super.dispose(); }

  void _publish() {
    if (_titleController.text.trim().isNotEmpty && mounted) {
      Navigator.of(context).pop();
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
        behavior: SnackBarBehavior.floating, backgroundColor: DS.surface3,
        content: Text('发布成功', style: TextStyle(color: DS.textPrimary))));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: DS.bg,
      appBar: AppBar(
        backgroundColor: DS.bg, elevation: 0, scrolledUnderElevation: 0,
        leading: IconButton(icon: const Icon(Icons.close_rounded, color: DS.textPrimary), onPressed: () => Navigator.pop(context)),
        title: const Text('发帖', style: DS.title),
        actions: [Padding(
          padding: const EdgeInsets.only(right: DS.sp16),
          child: GestureDetector(onTap: _publish, child: Center(
            child: SpringButton(
              onPressed: _titleController.text.trim().isEmpty ? null : _publish,
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 9),
              radius: DS.rMd,
              child: const Text('发布', style: TextStyle(fontSize: 13)),
            ),
          )),
        )],
      ),
      body: ListView(padding: const EdgeInsets.all(DS.sp16), children: [
        Wrap(spacing: 8, runSpacing: 8, children: _types.map((t) {
          final sel = _selectedType == t;
          return GestureDetector(
            onTap: () { HapticFeedback.selectionClick(); setState(() => _selectedType = t); },
            child: AnimatedContainer(duration: DS.durMicro,
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
              decoration: BoxDecoration(
                color: sel ? DS.accent : DS.surface2,
                borderRadius: BorderRadius.circular(18),
              ),
              child: Text(t, style: TextStyle(fontSize: 13, fontWeight: sel ? FontWeight.w700 : FontWeight.w500,
                  color: sel ? Colors.white : DS.textSecondary)),
            ),
          );
        }).toList()),
        const SizedBox(height: DS.sp20),
        TextField(
          controller: _titleController,
          onChanged: (_) => setState(() {}),
          autofocus: true,
          style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w700, color: DS.textPrimary, height: 1.3),
          decoration: const InputDecoration(hintText: '起个标题吧', border: InputBorder.none,
              hintStyle: TextStyle(fontSize: 20, fontWeight: FontWeight.w700, color: DS.textDisabled)),
        ),
        TextField(
          controller: _contentController,
          maxLines: 10,
          style: const TextStyle(fontSize: 15, color: DS.textPrimary, height: 1.6),
          decoration: const InputDecoration(hintText: '分享你的想法、讨论漫画内容...', border: InputBorder.none,
              hintStyle: TextStyle(fontSize: 15, color: DS.textDisabled)),
        ),
        const SizedBox(height: DS.sp16),
        Row(children: [
          _toolBtn(Icons.auto_stories_rounded, '引用漫画'),
          const SizedBox(width: DS.sp8),
          _toolBtn(Icons.image_outlined, '图片'),
        ]),
      ]),
    );
  }

  Widget _toolBtn(IconData icon, String label) {
    return GestureDetector(
      onTap: () {},
      child: Container(padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(color: DS.surface2, borderRadius: BorderRadius.circular(DS.rMd)),
        child: Row(children: [Icon(icon, size: 16, color: DS.textSecondary),
          const SizedBox(width: 6), Text(label, style: const TextStyle(fontSize: 12, color: DS.textSecondary))])),
    );
  }
}
