import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../auth/bloc/auth_bloc.dart';

class ProfilePage extends StatelessWidget {
  const ProfilePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('我的')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 36,
                    backgroundColor: Theme.of(context).colorScheme.primary,
                    child: const Icon(Icons.person, size: 36, color: Colors.white),
                  ),
                  const SizedBox(width: 16),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('漫界用户', style: Theme.of(context).textTheme.titleLarge),
                      const SizedBox(height: 4),
                      Text('138****8000', style: Theme.of(context).textTheme.bodyMedium),
                    ],
                  ),
                  const Spacer(),
                  Icon(Icons.chevron_right, color: Theme.of(context).colorScheme.primary),
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),
          _MenuItem(icon: Icons.history, title: '阅读记录', onTap: () {}),
          _MenuItem(icon: Icons.bookmark_outline, title: '我的收藏', onTap: () {}),
          _MenuItem(icon: Icons.download_outlined, title: '下载管理', onTap: () {}),
          _MenuItem(icon: Icons.message_outlined, title: '消息中心', onTap: () {}),
          const Divider(height: 32),
          _MenuItem(icon: Icons.source_outlined, title: '漫画源管理', onTap: () => context.push('/source-manager')),
          _MenuItem(icon: Icons.settings_outlined, title: '设置', onTap: () => context.push('/settings')),
          _MenuItem(icon: Icons.info_outline, title: '关于漫界', onTap: () {}),
          const SizedBox(height: 32),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton(
              onPressed: () => context.read<AuthBloc>().add(AuthLogoutRequested()),
              style: OutlinedButton.styleFrom(foregroundColor: Colors.red),
              child: const Text('退出登录'),
            ),
          ),
        ],
      ),
    );
  }
}

class _MenuItem extends StatelessWidget {
  final IconData icon;
  final String title;
  final VoidCallback onTap;
  const _MenuItem({required this.icon, required this.title, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon, color: Theme.of(context).colorScheme.primary),
      title: Text(title),
      trailing: Icon(Icons.chevron_right, color: Theme.of(context).colorScheme.primary),
      onTap: onTap,
    );
  }
}
