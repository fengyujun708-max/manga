import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../../app/ds.dart';
import '../../auth/bloc/auth_bloc.dart';

/// 个人中心
class ProfilePage extends StatelessWidget {
  const ProfilePage({super.key});

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
            title: const Text('我的', style: DS.headline),
          ),

          SliverToBoxAdapter(child: Padding(
            padding: const EdgeInsets.fromLTRB(DS.sp16, DS.sp4, DS.sp16, 0),
            child: Column(children: [
              // 用户卡
              Glass(radius: DS.rXl, blur: 30, padding: const EdgeInsets.all(DS.sp20),
                child: Row(children: [
                  Container(width: 64, height: 64,
                    decoration: BoxDecoration(shape: BoxShape.circle,
                        gradient: LinearGradient(colors: [DS.accent.withValues(alpha: 0.7), DS.accent.withValues(alpha: 0.25)])),
                    child: const Center(child: Icon(Icons.person_rounded, size: 32, color: Colors.white))),
                  const SizedBox(width: DS.sp16),
                  Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: const [
                    Text('漫界用户', style: TextStyle(fontSize: 19, fontWeight: FontWeight.w800, color: DS.textPrimary)),
                    SizedBox(height: 4),
                    Text('138****8000', style: TextStyle(fontSize: 13, color: DS.textTertiary)),
                  ])),
                  const Icon(Icons.chevron_right_rounded, size: 20, color: DS.textDisabled),
                ])),

              // 数据统计
              const SizedBox(height: DS.sp12),
              Glass(radius: DS.rLg, blur: 24, padding: const EdgeInsets.symmetric(vertical: DS.sp16),
                child: Row(children: [
                  _stat('128', '收藏'), _divider(),
                  _stat('36', '历史'), _divider(),
                  _stat('5', '下载'),
                ])),

              const SizedBox(height: DS.sp20),
            ]),
          )),

          SliverPadding(
            padding: const EdgeInsets.fromLTRB(DS.sp16, 0, DS.sp16, 120),
            sliver: SliverList(
              delegate: SliverChildListDelegate([
                _section('我的漫画', [
                  _Item(Icons.history_rounded, '阅读记录', () {}),
                  _Item(Icons.bookmark_border_rounded, '我的收藏', () => context.go('/library')),
                  _Item(Icons.download_outlined, '下载管理', () => context.go('/library')),
                  _Item(Icons.notifications_outlined, '消息中心', () {}),
                ]),
                const SizedBox(height: DS.sp12),
                _section('应用', [
                  _Item(Icons.extension_outlined, '漫画源管理', () => context.push('/source-manager')),
                  _Item(Icons.settings_outlined, '设置', () => context.push('/settings')),
                  _Item(Icons.info_outline_rounded, '关于漫界', () {}),
                ]),
                const SizedBox(height: DS.sp24),
                GestureDetector(
                  onTap: () => context.read<AuthBloc>().add(AuthLogoutRequested()),
                  child: Container(
                    width: double.infinity, padding: const EdgeInsets.symmetric(vertical: 14),
                    decoration: BoxDecoration(color: DS.error.withValues(alpha: 0.07), borderRadius: BorderRadius.circular(DS.rMd)),
                    child: const Center(child: Text('退出登录',
                        style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: DS.error))),
                  ),
                ),
              ]),
            ),
          ),
        ],
      ),
    );
  }

  static Widget _stat(String value, String label) => Expanded(child: Column(children: [
    Text(value, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800, color: DS.textPrimary)),
    const SizedBox(height: 2),
    Text(label, style: DS.micro),
  ]));

  static Widget _divider() => Container(width: 0.5, height: 28, color: DS.glassBorder);

  static Widget _section(String title, List<_Item> items) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
    Padding(padding: const EdgeInsets.only(left: 4, bottom: 8), child: Text(title, style: DS.caption)),
    Glass(radius: DS.rLg, blur: 24, padding: const EdgeInsets.symmetric(horizontal: DS.sp4),
      child: Column(children: [
        for (var i = 0; i < items.length; i++) ...[
          if (i > 0) Container(height: 0.5, margin: const EdgeInsets.only(left: 48), color: DS.glassBorder),
          _MenuTile(item: items[i]),
        ],
      ])),
  ]);
}

class _Item {
  final IconData icon;
  final String title;
  final VoidCallback onTap;
  const _Item(this.icon, this.title, this.onTap);
}

class _MenuTile extends StatelessWidget {
  final _Item item;
  const _MenuTile({required this.item});
  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: () { HapticFeedback.selectionClick(); item.onTap(); },
      child: Padding(padding: const EdgeInsets.symmetric(horizontal: DS.sp12, vertical: 13),
        child: Row(children: [
          Icon(item.icon, size: 20, color: DS.textSecondary),
          const SizedBox(width: DS.sp16),
          Expanded(child: Text(item.title, style: const TextStyle(fontSize: 15, color: DS.textPrimary))),
          const Icon(Icons.chevron_right_rounded, size: 18, color: DS.textDisabled),
        ])),
    );
  }
}
