import 'package:flutter/material.dart';
import '../../../app/theme/theme.dart';
import '../../../app/components/manjie_card.dart';
import '../../../app/components/manjie_button.dart';
import '../../../app/components/manjie_toast.dart';
import '../../../app/components/manjie_avatar.dart';

class RequestPage extends StatefulWidget {
  final String type; // 'manga' or 'source'
  const RequestPage({super.key, required this.type});

  @override
  State<RequestPage> createState() => _RequestPageState();
}

class _RequestPageState extends State<RequestPage> with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
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
        title: Text(widget.type == 'manga' ? '求漫' : '求源'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () => _showCreateDialog(context),
            tooltip: '发布',
          ),
        ],
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: AppTheme.primary,
          labelColor: AppTheme.primary,
          unselectedLabelColor: AppTheme.textSecondary,
          tabs: const [
            Tab(text: '进行中'),
            Tab(text: '已解决'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _RequestList(type: widget.type, status: 'pending'),
          _RequestList(type: widget.type, status: 'resolved'),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _showCreateDialog(context),
        backgroundColor: AppTheme.primary,
        icon: const Icon(Icons.add, color: Colors.white),
        label: const Text('发布求漫', style: TextStyle(color: Colors.white)),
      ),
    );
  }

  void _showCreateDialog(BuildContext context) {
    final nameController = TextEditingController();
    final descController = TextEditingController();

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => Container(
        padding: EdgeInsets.only(
          left: 20, right: 20, top: 20,
          bottom: MediaQuery.of(context).viewInsets.bottom + 20,
        ),
        decoration: const BoxDecoration(
          color: AppTheme.surface,
          borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(child: Container(width: 40, height: 4,
              decoration: BoxDecoration(color: AppTheme.divider, borderRadius: BorderRadius.circular(2)))),
            const SizedBox(height: 16),
            Text(widget.type == 'manga' ? '发布求漫' : '发布求源',
              style: const TextStyle(color: AppTheme.textPrimary, fontSize: 20, fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            TextField(
              controller: nameController,
              decoration: InputDecoration(
                labelText: widget.type == 'manga' ? '漫画名称' : '源名称',
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
              ),
              style: const TextStyle(color: AppTheme.textPrimary),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: descController,
              maxLines: 3,
              decoration: InputDecoration(
                labelText: '描述/线索',
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
              ),
              style: const TextStyle(color: AppTheme.textPrimary),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: ManjieButton(
                label: '发布',
                onPressed: () {
                  if (nameController.text.trim().isNotEmpty) {
                    Navigator.of(context).pop();
                    ManjieToast.success(context, '发布成功');
                  }
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _RequestList extends StatelessWidget {
  final String type;
  final String status;
  const _RequestList({required this.type, required this.status});

  @override
  Widget build(BuildContext context) {
    final items = List.generate(status == 'pending' ? 6 : 4, (i) => _MockRequest(
      id: '$i',
      title: type == 'manga'
        ? ['求漫画《XXX》找了很久', '求一部老漫画的名字'][i % 2]
        : ['求一个稳定的XXX源', '求能看高清漫画的源'][i % 2],
      user: '用户${i + 1}',
      time: '${i + 1}天前',
      answers: (i + 1) * 2,
      status: status == 'pending'
        ? ['待处理', '寻找中'][i % 2]
        : '已解决',
      hasAiMatch: i == 0,
    ));

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: items.length,
      itemBuilder: (_, i) => _RequestCard(type: type, request: items[i]),
    );
  }
}

class _RequestCard extends StatelessWidget {
  final String type;
  final _MockRequest request;
  const _RequestCard({required this.type, required this.request});

  Color _statusColor(String status) {
    switch (status) {
      case '已解决': return Colors.green;
      case '已找到': return Colors.green;
      case '寻找中': return Colors.orange;
      case '待处理': return Colors.orange;
      default: return AppTheme.textSecondary;
    }
  }

  @override
  Widget build(BuildContext context) {
    return ManjieCard(
      margin: const EdgeInsets.only(bottom: 12),
      onTap: () => _showDetail(context),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: _statusColor(request.status).withOpacity(0.15),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text(request.status, style: TextStyle(color: _statusColor(request.status), fontSize: 11)),
              ),
              const SizedBox(width: 8),
              Text(request.user, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
              const Spacer(),
              Text(request.time, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
            ],
          ),
          const SizedBox(height: 10),
          Text(request.title, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 16, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          Text(
            type == 'manga'
              ? '记得是几年前看的，主角使用剑术，画风很精美...'
              : '之前用的源最近不能用了，求推荐稳定的替代源',
            style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13),
            maxLines: 2, overflow: TextOverflow.ellipsis,
          ),
          const SizedBox(height: 12),

          // AI 匹配提示
          if (request.hasAiMatch)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
              decoration: BoxDecoration(
                color: AppTheme.accent.withOpacity(0.1),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: AppTheme.accent.withOpacity(0.2)),
              ),
              child: Row(
                children: [
                  Icon(Icons.auto_awesome, size: 16, color: AppTheme.accent),
                  const SizedBox(width: 6),
                  const Text('AI 已找到候选漫画', style: TextStyle(color: AppTheme.accent, fontSize: 12)),
                  const Spacer(),
                  Text('查看 →', style: TextStyle(color: AppTheme.accent, fontSize: 12)),
                ],
              ),
            ),
          if (request.hasAiMatch) const SizedBox(height: 12),

          // 底部
          Row(
            children: [
              const Icon(Icons.chat_bubble_outline, size: 16, color: AppTheme.textSecondary),
              const SizedBox(width: 4),
              Text('${request.answers} 个回答', style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
              if (request.status == '已解决') ...[
                const SizedBox(width: 16),
                const Icon(Icons.check_circle, size: 16, color: Colors.green),
                const SizedBox(width: 4),
                const Text('已绑定漫画', style: TextStyle(color: Colors.green, fontSize: 12)),
              ],
            ],
          ),
        ],
      ),
    );
  }

  void _showDetail(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => DraggableScrollableSheet(
        initialChildSize: 0.7,
        maxChildSize: 0.9,
        minChildSize: 0.5,
        builder: (_, scrollController) => Container(
          padding: const EdgeInsets.all(20),
          decoration: const BoxDecoration(
            color: AppTheme.surface,
            borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
          ),
          child: ListView(
            controller: scrollController,
            children: [
              Center(child: Container(width: 40, height: 4,
                decoration: BoxDecoration(color: AppTheme.divider, borderRadius: BorderRadius.circular(2)))),
              const SizedBox(height: 16),
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: _statusColor(request.status).withOpacity(0.15),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(request.status, style: TextStyle(color: _statusColor(request.status), fontSize: 11)),
                  ),
                  const Spacer(),
                  Text(request.time, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
                ],
              ),
              const SizedBox(height: 12),
              Text(request.title, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 20, fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              const Text('记得是几年前看的，主角使用剑术，画风很精美，类似浪客剑心的风格...',
                style: TextStyle(color: AppTheme.textSecondary, fontSize: 14, height: 1.5)),
              const SizedBox(height: 8),
              Text('发布者: ${request.user}', style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12)),

              // AI 匹配
              if (request.hasAiMatch) ...[
                const SizedBox(height: 16),
                const Divider(color: AppTheme.divider),
                const SizedBox(height: 12),
                Row(
                  children: [
                    const Icon(Icons.auto_awesome, size: 20, color: AppTheme.accent),
                    const SizedBox(width: 8),
                    const Text('AI 匹配结果', style: TextStyle(color: AppTheme.accent, fontSize: 16, fontWeight: FontWeight.w600)),
                  ],
                ),
                const SizedBox(height: 12),
                _AiMatchCard(
                  title: '浪客剑心',
                  match: '92%',
                  reason: '主角使用剑术、明治时代背景、画风相似',
                  onTap: () {},
                ),
                _AiMatchCard(
                  title: '鬼灭之刃',
                  match: '78%',
                  reason: '使用剑术战斗、大正时代背景',
                  onTap: () {},
                ),
              ],

              // 回答区
              const SizedBox(height: 16),
              const Divider(color: AppTheme.divider),
              const SizedBox(height: 12),
              const Text('回答', style: TextStyle(color: AppTheme.textPrimary, fontSize: 16, fontWeight: FontWeight.w600)),
              const SizedBox(height: 12),
              ...List.generate(3, (i) => _AnswerCard(
                user: '热心用户${i + 1}',
                content: ['这部漫画应该是《浪客剑心》吧？', '我觉得可能是《鬼灭之刃》', '是不是《银魂》？'][i],
                time: '${i + 1}小时前',
                isBest: i == 0,
              )),

              // 输入回答
              const SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      decoration: InputDecoration(
                        hintText: '输入你的回答...',
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(24)),
                        filled: true,
                        fillColor: AppTheme.surfaceLight,
                        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  IconButton(
                    icon: const Icon(Icons.send, color: AppTheme.primary),
                    onPressed: () => ManjieToast.success(context, '回答已发送'),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _AiMatchCard extends StatelessWidget {
  final String title;
  final String match;
  final String reason;
  final VoidCallback onTap;

  const _AiMatchCard({required this.title, required this.match, required this.reason, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return ManjieCard(
      margin: const EdgeInsets.only(bottom: 8),
      onTap: onTap,
      child: Row(
        children: [
          Container(
            width: 48, height: 64,
            decoration: BoxDecoration(
              color: AppTheme.primary.withOpacity(0.2),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Center(child: Text(title.substring(0, 1),
              style: const TextStyle(fontSize: 24, color: AppTheme.primary))),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(title, style: const TextStyle(color: AppTheme.textPrimary, fontWeight: FontWeight.w600)),
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: AppTheme.accent.withOpacity(0.15),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text('匹配度 $match', style: const TextStyle(color: AppTheme.accent, fontSize: 11)),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(reason, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
              ],
            ),
          ),
          const Icon(Icons.chevron_right, color: AppTheme.textSecondary, size: 20),
        ],
      ),
    );
  }
}

class _AnswerCard extends StatelessWidget {
  final String user;
  final String content;
  final String time;
  final bool isBest;

  const _AnswerCard({required this.user, required this.content, required this.time, this.isBest = false});

  @override
  Widget build(BuildContext context) {
    return ManjieCard(
      margin: const EdgeInsets.only(bottom: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              ManjieAvatar(name: user, size: 28),
              const SizedBox(width: 8),
              Text(user, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 13, fontWeight: FontWeight.w500)),
              const SizedBox(width: 8),
              if (isBest)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                  decoration: BoxDecoration(
                    color: Colors.green.withOpacity(0.15),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: const Text('最佳回答', style: TextStyle(color: Colors.green, fontSize: 10)),
                ),
              const Spacer(),
              Text(time, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 11)),
            ],
          ),
          const SizedBox(height: 8),
          Text(content, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 14)),
          const SizedBox(height: 8),
          Row(
            children: [
              GestureDetector(
                onTap: () => ManjieToast.success(context, '已标记为最佳回答'),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: Colors.green.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: const Text('设为最佳', style: TextStyle(color: Colors.green, fontSize: 11)),
                ),
              ),
              const SizedBox(width: 8),
              const Icon(Icons.thumb_up_outlined, size: 16, color: AppTheme.textSecondary),
              const SizedBox(width: 4),
              const Text('3', style: TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
            ],
          ),
        ],
      ),
    );
  }
}

class _MockRequest {
  final String id;
  final String title;
  final String user;
  final String time;
  final int answers;
  final String status;
  final bool hasAiMatch;

  _MockRequest({
    required this.id,
    required this.title,
    required this.user,
    required this.time,
    required this.answers,
    required this.status,
    this.hasAiMatch = false,
  });
}