import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../theme/theme.dart';

class AppShell extends StatelessWidget {
  final Widget child;
  const AppShell({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: child,
      bottomNavigationBar: _buildBottomNav(context),
    );
  }

  Widget _buildBottomNav(BuildContext context) {
    final location = GoRouterState.of(context).matchedLocation;
    final int currentIndex = switch (location) {
      '/home' => 0,
      '/discover' => 1,
      '/library' => 2,
      '/community' => 3,
      '/profile' => 4,
      _ => 0,
    };

    return Container(
      decoration: const BoxDecoration(
        border: Border(top: BorderSide(color: AppTheme.divider, width: 0.5)),
      ),
      child: BottomNavigationBar(
        currentIndex: currentIndex,
        onTap: (i) {
          final routes = ['/home', '/discover', '/library', '/community', '/profile'];
          context.go(routes[i]);
        },
        items: const [
          BottomNavigationBarItem(icon: Icon(Icons.home_outlined), activeIcon: Icon(Icons.home), label: '首页'),
          BottomNavigationBarItem(icon: Icon(Icons.explore_outlined), activeIcon: Icon(Icons.explore), label: '发现'),
          BottomNavigationBarItem(icon: Icon(Icons.library_books_outlined), activeIcon: Icon(Icons.library_books), label: '书架'),
          BottomNavigationBarItem(icon: Icon(Icons.forum_outlined), activeIcon: Icon(Icons.forum), label: '社区'),
          BottomNavigationBarItem(icon: Icon(Icons.person_outline), activeIcon: Icon(Icons.person), label: '我的'),
        ],
      ),
    );
  }
}
