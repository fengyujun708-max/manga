import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../app/theme/theme.dart';
import '../../../app/components/manjie_card.dart';
import '../../../app/components/manjie_avatar.dart';
import '../../../app/components/manjie_button.dart';
import '../../../app/components/manjie_toast.dart';

class CommunityPage extends StatefulWidget {
  const CommunityPage({super.key});

  @override
  State<CommunityPage> createState() => _CommunityPageState();
}

class _CommunityPageState extends State<CommunityPage> with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 4, vsync: this);
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
        title: const Text('社区'),
        actions: [
          IconButton(
            icon: const Icon(Icons.edit),
            onPressed: () => context.push('/community/create'),
            tooltip: '发帖',
          ),
        ],
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: AppTheme.primary,
          labelColor: AppTheme.primary,
          unselectedLabelColor: AppTheme.textSecondary,
          isScrollable: true,
          tabs: const [
            Tab(text: '推荐'),
            Tab(text: '最新'),
            Tab(text: '求漫'),
            Tab(text: '求源'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _PostList(type: '推荐'),
          _PostList(type: '最新'),
          _RequestTab(type: 'manga'),
          _RequestTab(type: 'source'),
        ],
      ),
    );
  }
}

class _PostList extends StatelessWidget {
  final String type;
  const _PostList({required this.type});

  @override
  Widget build(BuildContext context) {
    final posts = List.generate(15, (i) => _MockPost(
      id: '$i',
      user: '用户${i + 1}',
      avatar: null,
      title: ['推荐一部超好看的漫画《XXX》', '求问这部漫画的名字', '最新话分析：剧情走向预测', '大家的书架里都有什么？', '漫画推荐：这几部必看'][i % 5],
      content: ['最近发现了一部宝藏漫画，画风精美剧情紧凑...', '在某个地方看到这部漫画但忘记名字了...', '最新一话的信息量很大，来聊聊我的看法...', '分享一下我最近在追的漫画，大家有推荐的吗？', '整理了几部个人觉得非常值得看的漫画'][i % 5],
      likes: (i + 1) * 3,
      comments: i + 1,
      time: '${i + 1}小时前',
      tags: i % 3 == 0 ? ['讨论', '推荐'] : i % 3 == 1 ? ['求漫'] : ['分析'],
      hasComicRef: i == 0 || i == 4,
    ));

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: posts.length + 1,
      itemBuilder: (_, i) {
        if (i == 0) {
          return Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    readOnly: true,
                    decoration: InputDecoration(
                      hintText: '分享你的想法...',
                      prefixIcon: const Icon(Icons.edit, size: 18),
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(24)),
                      filled: true,
                      fillColor: AppTheme.surface,
                      contentPadding: const EdgeInsets.symmetric(vertical: 12),
                    ),
                    onTap: () => context.push('/community/create'),
                  ),
                ),
                const SizedBox(width: 8),
                ManjieAvatar(name: '我', size: 40),
              ],
            ),
          );
        }
        return _PostCard(post: posts[i - 1]);
      },
    );
  }
}

class _PostCard extends StatelessWidget {
  final _MockPost post;
  const _PostCard({required this.post});

  @override
  Widget build(BuildContext context) {
    return ManjieCard(
      margin: const EdgeInsets.only(bottom: 12),
      onTap: () => context.push('/community/post/${post.id}'),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 用户信息
          Row(
            children: [
              ManjieAvatar(name: post.user, size: 36),
              const SizedBox(width: 10),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(post.user, style: const TextStyle(color: AppTheme.textPrimary, fontWeight: FontWeight.w500, fontSize: 14)),
                  Text(post.time, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
                ],
              ),
              const Spacer(),
              if (post.tags.isNotEmpty)
                ...post.tags.map((t) => Container(
                  margin: const EdgeInsets.only(left: 4),
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(
                    color: AppTheme.primary.withOpacity(0.15),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Text(t, style: const TextStyle(color: AppTheme.primary, fontSize: 11)),
                )),
            ],
          ),
          const SizedBox(height: 12),

          // 标题
          Text(post.title, style: const TextStyle(color: AppTheme.textPrimary, fontWeight: FontWeight.w600, fontSize: 16)),
          const SizedBox(height: 6),

          // 内容
          Text(post.content, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 14, height: 1.5),
            maxLines: 3, overflow: TextOverflow.ellipsis),
          const SizedBox(height: 12),

          // 漫画引用卡片
          if (post.hasComicRef)
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppTheme.primary.withOpacity(0.08),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: AppTheme.primary.withOpacity(0.15)),
              ),
              child: Row(
                children: [
                  Container(
                    width: 48, height: 64,
                    decoration: BoxDecoration(
                      color: AppTheme.primary.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: const Center(child: Icon(Icons.auto_stories, color: AppTheme.primary, size: 24)),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('海贼王', style: TextStyle(color: AppTheme.textPrimary, fontWeight: FontWeight.w600, fontSize: 14)),
                        const Text('⭐ 9.5 · 更新至 1120 话', style: TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
                      ],
                    ),
                  ),
                  const Icon(Icons.chevron_right, color: AppTheme.primary, size: 20),
                ],
              ),
            ),

          const SizedBox(height: 12),

          // 操作栏
          Row(
            children: [
              _ActionButton(icon: Icons.favorite_border, count: post.likes, color: Colors.red),
              const SizedBox(width: 16),
              _ActionButton(icon: Icons.chat_bubble_outline, count: post.comments),
              const SizedBox(width: 16),
              _ActionButton(icon: Icons.bookmark_border, count: null),
              const Spacer(),
              const Icon(Icons.more_horiz, color: AppTheme.textSecondary, size: 20),
            ],
          ),
        ],
      ),
    );
  }
}

class _ActionButton extends StatelessWidget {
  final IconData icon;
  final int? count;
  final Color? color;

  const _ActionButton({required this.icon, this.count, this.color});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {},
      child: Row(
        children: [
          Icon(icon, size: 18, color: color ?? AppTheme.textSecondary),
          if (count != null) ...[
            const SizedBox(width: 4),
            Text('$count', style: TextStyle(color: color ?? AppTheme.textSecondary, fontSize: 12)),
          ],
        ],
      ),
    );
  }
}

// ====== 求漫/求源 Tab ======
class _RequestTab extends StatelessWidget {
  final String type;
  const _RequestTab({required this.type});

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: 8,
      itemBuilder: (_, i) => ManjieCard(
        margin: const EdgeInsets.only(bottom: 12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(
                    color: type == 'manga' ? Colors.orange.withOpacity(0.15) : Colors.green.withOpacity(0.15),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Text(type == 'manga' ? '求漫' : '求源', style: TextStyle(
                    color: type == 'manga' ? Colors.orange : Colors.green, fontSize: 11)),
                ),
                const SizedBox(width: 8),
                Text('用户${i + 1}', style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
                const Spacer(),
                Text('${i + 1}天前', style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              type == 'manga'
                ? ['求漫画：《XXX》找了很久没找到', '求这部漫画的名字，画风很独特'][i % 2]
                : ['求一个能看XXX的源', '求稳定的漫画源'][i % 2],
              style: const TextStyle(color: AppTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.w500),
            ),
            const SizedBox(height: 6),
            Text(
              type == 'manga'
                ? '记得是几年前看过的，主角有特殊能力...'
                : '之前用的源最近失效了，求推荐稳定的替代源',
              style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13),
              maxLines: 2, overflow: TextOverflow.ellipsis,
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Icon(Icons.chat_bubble_outline, size: 16, color: AppTheme.textSecondary),
                const SizedBox(width: 4),
                Text('${i + 1} 个回答', style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
                const Spacer(),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(
                    color: ['待处理', '寻找中', '已解决', '已找到'][i % 4] == '已解决' || ['待处理', '寻找中', '已解决', '已找到'][i % 4] == '已找到'
                      ? Colors.green.withOpacity(0.15) : Colors.orange.withOpacity(0.15),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Text(['待处理', '寻找中', '已解决', '已找到'][i % 4], style: TextStyle(
                    color: ['待处理', '寻找中', '已解决', '已找到'][i % 4] == '已解决' || ['待处理', '寻找中', '已解决', '已找到'][i % 4] == '已找到'
                      ? Colors.green : Colors.orange, fontSize: 11)),
                ),
              ],
            ),
          ],
        ),
      ),
    );
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

  @override
  void dispose() {
    _titleController.dispose();
    _contentController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('发帖'),
        actions: [
          TextButton(
            onPressed: () {
              if (_titleController.text.trim().isNotEmpty) {
                ManjieToast.success(context, '发布成功');
                Navigator.of(context).pop();
              }
            },
            child: const Text('发布', style: TextStyle(color: AppTheme.primary, fontWeight: FontWeight.w600)),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // 分类选择
          Row(
            children: [
              '讨论', '推荐', '求漫', '求源', '分析', '吐槽',
            ].map((t) => Padding(
              padding: const EdgeInsets.only(right: 8),
              child: ChoiceChip(
                label: Text(t),
                selected: _selectedType == t,
                onSelected: (v) => setState(() => _selectedType = t),
                selectedColor: AppTheme.primary,
                labelStyle: TextStyle(color: _selectedType == t ? Colors.white : AppTheme.textPrimary, fontSize: 13),
              ),
            )).toList(),
          ),
          const SizedBox(height: 16),

          // 标题
          TextField(
            controller: _titleController,
            decoration: const InputDecoration(
              hintText: '标题',
              border: InputBorder.none,
              hintStyle: TextStyle(color: AppTheme.textSecondary, fontSize: 18),
            ),
            style: const TextStyle(color: AppTheme.textPrimary, fontSize: 18, fontWeight: FontWeight.w600),
          ),
          const Divider(color: Color(0xFF312E81)),

          // 内容
          TextField(
            controller: _contentController,
            maxLines: 10,
            decoration: const InputDecoration(
              hintText: '分享你的想法、讨论漫画内容...',
              border: InputBorder.none,
              hintStyle: TextStyle(color: AppTheme.textSecondary, fontSize: 15),
            ),
            style: const TextStyle(color: AppTheme.textPrimary, fontSize: 15, height: 1.6),
          ),

          const SizedBox(height: 16),

          // 添加漫画引用
          OutlinedButton.icon(
            onPressed: () {},
            icon: const Icon(Icons.auto_stories, size: 18),
            label: const Text('引用漫画'),
            style: OutlinedButton.styleFrom(
              foregroundColor: AppTheme.primary,
              side: BorderSide(color: AppTheme.primary.withOpacity(0.3)),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
            ),
          ),
          const SizedBox(height: 8),

          // 添加图片
          OutlinedButton.icon(
            onPressed: () {},
            icon: const Icon(Icons.image, size: 18),
            label: const Text('添加图片'),
            style: OutlinedButton.styleFrom(
              foregroundColor: AppTheme.primary,
              side: BorderSide(color: AppTheme.primary.withOpacity(0.3)),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
            ),
          ),
        ],
      ),
    );
  }
}

class _MockPost {
  final String id;
  final String user;
  final String? avatar;
  final String title;
  final String content;
  final int likes;
  final int comments;
  final String time;
  final List<String> tags;
  final bool hasComicRef;

  _MockPost({
    required this.id,
    required this.user,
    this.avatar,
    required this.title,
    required this.content,
    required this.likes,
    required this.comments,
    required this.time,
    required this.tags,
    this.hasComicRef = false,
  });
}