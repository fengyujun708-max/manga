import 'package:flutter/material.dart';
import 'dart:ui';
import 'package:go_router/go_router.dart';
import '../theme/theme.dart';

/// 液态玻璃底栏 — 滚动下滑隐藏，上滑/停止显示
/// 仿 iOS Safari / Twitter 底栏行为
class AppShell extends StatefulWidget {
  final Widget child;
  const AppShell({super.key, required this.child});

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell>
    with SingleTickerProviderStateMixin {
  late AnimationController _animController;
  late Animation<double> _barAnimation;
  late Animation<double> _barOffset;

  bool _isBarVisible = true;
  double _lastScrollOffset = 0;

  static const _routes = ['/home', '/discover', '/library', '/community', '/profile'];
  static const _labels = ['首页', '发现', '书架', '社区', '我的'];
  static const _icons = [
    Icons.auto_awesome_outlined,
    Icons.travel_explore_outlined,
    Icons.menu_book_outlined,
    Icons.diversity_3_outlined,
    Icons.face_6_outlined,
  ];
  static const _activeIcons = [
    Icons.auto_awesome,
    Icons.travel_explore,
    Icons.menu_book,
    Icons.diversity_3,
    Icons.face_6,
  ];

  @override
  void initState() {
    super.initState();
    _animController = AnimationController(
      duration: const Duration(milliseconds: 350),
      vsync: this,
    );
    _barAnimation = CurvedAnimation(parent: _animController, curve: Curves.easeOutCubic);
    _barOffset = Tween<double>(begin: 0, end: 1).animate(_barAnimation);
  }

  @override
  void dispose() {
    _animController.dispose();
    super.dispose();
  }

  void _onScroll(ScrollNotification notification) {
    if (notification is ScrollUpdateNotification) {
      final metrics = notification.metrics;
      final currentScroll = metrics.pixels;
      final delta = currentScroll - _lastScrollOffset;

      // 滚动方向判断
      if (delta > 3 && currentScroll > 50) {
        // 下滑 → 隐藏
        if (_isBarVisible) {
          _isBarVisible = false;
          _animController.forward();
        }
      } else if (delta < -3) {
        // 上滑 → 显示
        if (!_isBarVisible) {
          _isBarVisible = true;
          _animController.reverse();
        }
      }
      _lastScrollOffset = currentScroll.clamp(0, double.infinity);
    } else if (notification is ScrollEndNotification) {
      // 停止滚动 → 显示
      if (!_isBarVisible) {
        _isBarVisible = true;
        _animController.reverse();
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final location = GoRouterState.of(context).matchedLocation;
    int currentIndex = _routes.indexWhere((r) => location.startsWith(r));
    if (currentIndex < 0) currentIndex = 0;

    return Scaffold(
      body: NotificationListener<ScrollNotification>(
        onNotification: (notification) {
          _onScroll(notification);
          return false;
        },
        child: widget.child,
      ),
      extendBody: true,
      bottomNavigationBar: AnimatedBuilder(
        animation: _barOffset,
        builder: (context, child) {
          return Transform.translate(
            offset: Offset(0, _barOffset.value * 120),
            child: Opacity(opacity: 1 - _barOffset.value * 0.3, child: child),
          );
        },
        child: _buildLiquidGlassBar(currentIndex),
      ),
    );
  }

  Widget _buildLiquidGlassBar(int currentIndex) {
    return Container(
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 12),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(28),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 30, sigmaY: 30),
          child: Container(
            decoration: BoxDecoration(
              color: AppTheme.surface.withValues(alpha: 0.55),
              border: Border.all(color: AppTheme.glassBorder, width: 0.5),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.35),
                  blurRadius: 24,
                  offset: const Offset(0, 10),
                ),
                BoxShadow(
                  color: AppTheme.primary.withValues(alpha: 0.08),
                  blurRadius: 40,
                  offset: const Offset(0, 0),
                ),
              ],
            ),
            child: SafeArea(
              top: false,
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 6),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: List.generate(5, (i) => _buildNavItem(i, currentIndex)),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildNavItem(int index, int currentIndex) {
    final isActive = index == currentIndex;

    return GestureDetector(
      onTap: () {
        if (index != currentIndex) {
          context.go(_routes[index]);
        }
      },
      behavior: HitTestBehavior.opaque,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 350),
        curve: Curves.easeOutCubic,
        padding: EdgeInsets.symmetric(
          horizontal: isActive ? 18 : 14,
          vertical: 10,
        ),
        decoration: BoxDecoration(
          color: isActive
              ? AppTheme.primary.withValues(alpha: 0.12)
              : Colors.transparent,
          borderRadius: BorderRadius.circular(20),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // 图标 — 选中时发光
            TweenAnimationBuilder<double>(
              tween: Tween(begin: 0, end: isActive ? 1 : 0),
              duration: const Duration(milliseconds: 300),
              curve: Curves.easeOutCubic,
              builder: (context, val, child) {
                return Transform.scale(
                  scale: 1 + val * 0.15,
                  child: Container(
                    decoration: isActive && val > 0.5
                        ? BoxDecoration(
                            boxShadow: [
                              BoxShadow(
                                color: AppTheme.primary.withValues(alpha: val * 0.4),
                                blurRadius: 12,
                                spreadRadius: -2,
                              ),
                            ],
                          )
                        : null,
                    child: Icon(
                      isActive ? _activeIcons[index] : _icons[index],
                      size: 24,
                      color: isActive
                          ? AppTheme.primary
                          : AppTheme.textTertiary,
                    ),
                  ),
                );
              },
            ),
            const SizedBox(height: 5),
            // 文字
            AnimatedDefaultTextStyle(
              duration: const Duration(milliseconds: 250),
              style: TextStyle(
                fontSize: 10.5,
                fontWeight: isActive ? FontWeight.w700 : FontWeight.w400,
                color: isActive ? AppTheme.primary : AppTheme.textTertiary,
                letterSpacing: 0.3,
              ),
              child: Text(_labels[index]),
            ),
          ],
        ),
      ),
    );
  }
}
