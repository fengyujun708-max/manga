import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../theme/theme.dart';

// 丝滑底部导航栏，带玻璃拟态背景 + 弹性指示器
class AppShell extends StatefulWidget {
  final Widget child;
  const AppShell({super.key, required this.child});

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> with SingleTickerProviderStateMixin {
  int _currentIndex = 0;

  static const _routes = ['/home', '/discover', '/library', '/community', '/profile'];
  static const _labels = ['首页', '发现', '书架', '社区', '我的'];
  static const _icons = [
    Icons.auto_awesome_outlined,    // 首页 — 星光
    Icons.travel_explore_outlined,  // 发现 — 探索
    Icons.menu_book_outlined,       // 书架 — 书本
    Icons.diversity_3_outlined,     // 社区 — 群体
    Icons.face_6_outlined,          // 我的 — 头像
  ];
  static const _activeIcons = [
    Icons.auto_awesome,
    Icons.travel_explore,
    Icons.menu_book,
    Icons.diversity_3,
    Icons.face_6,
  ];

  @override
  Widget build(BuildContext context) {
    final location = GoRouterState.of(context).matchedLocation;
    _currentIndex = _routes.indexWhere((r) => location.startsWith(r));
    if (_currentIndex < 0) _currentIndex = 0;

    return Scaffold(
      body: widget.child,
      extendBody: true,
      bottomNavigationBar: _buildGlassBottomNav(),
    );
  }

  Widget _buildGlassBottomNav() {
    return Container(
      margin: const EdgeInsets.fromLTRB(12, 0, 12, 12),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.3),
            blurRadius: 20,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(24),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 20, sigmaY: 20),
          child: Container(
            decoration: BoxDecoration(
              color: AppTheme.surface.withValues(alpha: 0.85),
              border: Border.all(color: AppTheme.glassBorder, width: 0.5),
            ),
            child: SafeArea(
              top: false,
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: List.generate(5, (i) => _buildNavItem(i)),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildNavItem(int index) {
    final isActive = _currentIndex == index;
    final color = isActive ? AppTheme.primary : AppTheme.textSecondary;

    return GestureDetector(
      onTap: () {
        if (index != _currentIndex) {
          setState(() => _currentIndex = index);
          context.go(_routes[index]);
        }
      },
      behavior: HitTestBehavior.opaque,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeOutCubic,
        padding: EdgeInsets.symmetric(
          horizontal: isActive ? 16 : 12,
          vertical: 8,
        ),
        decoration: BoxDecoration(
          color: isActive ? AppTheme.primary.withValues(alpha: 0.15) : Colors.transparent,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            AnimatedSwitcher(
              duration: const Duration(milliseconds: 200),
              transitionBuilder: (child, anim) => ScaleTransition(scale: anim, child: child),
              child: Icon(
                isActive ? _activeIcons[index] : _icons[index],
                key: ValueKey(isActive),
                size: 24,
                color: color,
              ),
            ),
            const SizedBox(height: 4),
            AnimatedDefaultTextStyle(
              duration: const Duration(milliseconds: 200),
              style: TextStyle(
                fontSize: 11,
                fontWeight: isActive ? FontWeight.w700 : FontWeight.w400,
                color: color,
              ),
              child: Text(_labels[index]),
            ),
          ],
        ),
      ),
    );
  }
}
