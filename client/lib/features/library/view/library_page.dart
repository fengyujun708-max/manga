import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import '../../../app/ds.dart';

/// 书架页 — 收藏 / 历史 / 下载
class LibraryPage extends StatefulWidget {
  const LibraryPage({super.key});
  @override
  State<LibraryPage> createState() => _LibraryPageState();
}

class _LibraryPageState extends State<LibraryPage> {
  int _tab = 0;
  bool _isGrid = true;

  static const _tabs = ['收藏', '历史', '下载'];

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
              const Text('书库', style: DS.headline),
              const Spacer(),
              _iconBtn(_isGrid ? Icons.view_list_rounded : Icons.grid_view_rounded,
                  () => setState(() => _isGrid = !_isGrid)),
            ]),
          ),

          // 分段选择器
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(DS.sp16, DS.sp4, DS.sp16, DS.sp12),
              child: Container(
                padding: const EdgeInsets.all(3),
                decoration: BoxDecoration(color: DS.surface1, borderRadius: BorderRadius.circular(DS.rMd)),
                child: Row(children: [
                  for (var i = 0; i < _tabs.length; i++)
                    Expanded(child: GestureDetector(
                      onTap: () { HapticFeedback.selectionClick(); setState(() => _tab = i); },
                      child: AnimatedContainer(
                        duration: DS.durStd, curve: DS.cStd,
                        padding: const EdgeInsets.symmetric(vertical: 9),
                        decoration: BoxDecoration(
                          color: _tab == i ? DS.surface3 : Colors.transparent,
                          borderRadius: BorderRadius.circular(DS.rSm - 2),
                        ),
                        child: Center(child: Text(_tabs[i], style: TextStyle(
                          fontSize: 13, fontWeight: _tab == i ? FontWeight.w700 : FontWeight.w500,
                          color: _tab == i ? DS.textPrimary : DS.textTertiary))),
                      ),
                    )),
                ]),
              ),
            ),
          ),

          switch (_tab) {
            0 => _buildFavorites(),
            1 => _buildHistory(),
            _ => SliverFillRemaining(child: EmptyState(
                icon: Icons.download_outlined, title: '暂无下载',
                subtitle: '离线漫画将保存在这里，随时畅读')),
          },
        ],
      ),
    );
  }

  // ── 收藏 ──
  Widget _buildFavorites() {
    final items = List.generate(8, (i) => _Item(
      title: '收藏漫画 ${i + 1}', cover: '',
      subtitle: '更新至 ${(i + 1) * 10} 话 · ${i + 1}天前',
      hasUpdate: i < 3,
    ));
    return SliverPadding(
      padding: const EdgeInsets.fromLTRB(DS.sp16, 0, DS.sp16, 120),
      sliver: _isGrid
        ? SliverGrid(
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 3, childAspectRatio: 0.55, crossAxisSpacing: DS.sp12, mainAxisSpacing: DS.sp16),
            delegate: SliverChildBuilderDelegate((_, i) => ComicCard(
              cover: items[i].cover, title: items[i].title,
              badge: items[i].hasUpdate ? '更新' : null,
              onTap: () => GoRouter.of(context).push('/comic/${i + 1}'),
            ), childCount: items.length))
        : SliverList(
            delegate: SliverChildBuilderDelegate((_, i) => Padding(
              padding: const EdgeInsets.only(bottom: DS.sp8),
              child: _ListRow(item: items[i], progress: null),
            ), childCount: items.length)),
    );
  }

  // ── 历史 ──
  Widget _buildHistory() {
    final items = List.generate(5, (i) => _Item(
      title: '阅读记录 ${i + 1}', cover: '',
      subtitle: '第 ${(i + 1) * 20} 话 · ${i + 1}小时前',
      progress: 0.3 + i * 0.15,
    ));
    return SliverPadding(
      padding: const EdgeInsets.fromLTRB(DS.sp16, 0, DS.sp16, 120),
      sliver: SliverList(
        delegate: SliverChildBuilderDelegate((_, i) {
          if (i == 0) {
            return Padding(
              padding: const EdgeInsets.only(bottom: DS.sp8),
              child: Row(children: [
                Text('继续阅读', style: DS.title),
                const Spacer(),
                GestureDetector(
                  onTap: () {},
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                    decoration: BoxDecoration(color: DS.error.withValues(alpha: 0.08), borderRadius: BorderRadius.circular(DS.rSm)),
                    child: const Text('清空', style: TextStyle(fontSize: 12, color: DS.error, fontWeight: FontWeight.w600)),
                  ),
                ),
              ]),
            );
          }
          return Padding(
            padding: const EdgeInsets.only(bottom: DS.sp8),
            child: _ListRow(item: items[i - 1], progress: items[i - 1].progress),
          );
        }, childCount: items.length + 1),
      ),
    );
  }

  Widget _iconBtn(IconData icon, VoidCallback tap) {
    return GestureDetector(
      onTap: () { HapticFeedback.lightImpact(); tap(); },
      child: Container(width: 38, height: 38, decoration: BoxDecoration(color: DS.glassFill, borderRadius: BorderRadius.circular(12)), child: Icon(icon, size: 20, color: DS.textPrimary)),
    );
  }
}

/// 列表行 — 封面 + 信息 + 进度/更新徽章
class _ListRow extends StatelessWidget {
  final _Item item;
  final double? progress;
  const _ListRow({required this.item, this.progress});

  @override
  Widget build(BuildContext context) {
    return Glass(
      radius: DS.rLg,
      blur: 24,
      padding: const EdgeInsets.all(DS.sp12),
      child: Row(children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(DS.rMd),
          child: SizedBox(width: 56, height: 78, child: _cover()),
        ),
        const SizedBox(width: DS.sp12),
        Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Expanded(child: Text(item.title, maxLines: 1, overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600, color: DS.textPrimary))),
            if (item.hasUpdate)
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                decoration: BoxDecoration(color: DS.accent.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(6)),
                child: const Text('更新', style: TextStyle(fontSize: 10, color: DS.accent, fontWeight: FontWeight.w700)),
              ),
          ]),
          const SizedBox(height: 4),
          Text(item.subtitle, style: const TextStyle(fontSize: 12, color: DS.textTertiary)),
          if (progress != null) ...[
            const SizedBox(height: 10),
            ClipRRect(borderRadius: BorderRadius.circular(2),
              child: LinearProgressIndicator(value: progress, minHeight: 4,
                  backgroundColor: DS.surface3, valueColor: const AlwaysStoppedAnimation(DS.accent))),
            const SizedBox(height: 4),
            Text('${(progress! * 100).toInt()}%', style: DS.micro),
          ],
        ])),
        const SizedBox(width: DS.sp8),
        const Icon(Icons.chevron_right_rounded, size: 18, color: DS.textDisabled),
      ]),
    );
  }

  Widget _cover() => Container(
    color: DS.surface2,
    child: Center(child: Text(item.title.characters.first,
        style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w800, color: DS.textDisabled))),
  );
}

class _Item {
  final String title;
  final String cover;
  final String subtitle;
  final double progress;
  final bool hasUpdate;
  _Item({required this.title, required this.cover, required this.subtitle, this.progress = 0, this.hasUpdate = false});
}
