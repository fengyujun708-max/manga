import 'package:flutter/material.dart';
import 'dart:ui';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import '../theme/theme.dart';

/// 液态玻璃底栏 — 滚动下滑隐藏，上滑/停止显示
/// 弹簧曲线 + 真实 BackdropFilter + 内发光边框
class AppShell extends StatefulWidget {
  final Widget child;
  const AppShell({super.key, required this.child});

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell>
    with TickerProviderStateMixin {
  // 底栏显隐动画
  late AnimationController _barCtrl;
  late Animation<double> _barOffset;
  late Animation<double> _barOpacity;

  bool _isBarVisible = true;
  double _lastOffset = 0;
  int _currentIndex = 0;

  // 路由配置
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
    _barCtrl = AnimationController(
      duration: const Duration(milliseconds: 400),
      vsync: this,
    );
    _barOffset = Tween<double>(begin: 0, end: 1).animate(
      CurvedAnimation(parent: _barCtrl, curve: Curves.easeInOutCubic),
    );
    _barOpacity = Tween<double>(begin: 1, end: 0).animate(
      CurvedAnimation(parent: _barCtrl, curve: Curves.easeOut),
    );
  }

  @override
  void dispose() {
    _barCtrl.dispose();
    super.dispose();
  }

  void _onScroll(ScrollNotification n) {
    if (n is ScrollUpdateNotification) {
      final current = n.metrics.pixels;
      final delta = current - _lastOffset;
      if (delta > 4 && current > 60) {
        // 下滑 → 隐藏
        if (_isBarVisible) {
          _isBarVisible = false;
          _barCtrl.forward();
        }
      } else if (delta < -4) {
        // 上滑 → 显示
        if (!_isBarVisible) {
          _isBarVisible = true;
          _barCtrl.reverse();
        }
      }
      _lastOffset = current;
    } else if (n is ScrollEndNotification) {
      // 停止 → 显示
      if (!_isBarVisible) {
        _isBarVisible = true;
        _barCtrl.reverse();
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final loc = GoRouterState.of(context).matchedLocation;
    _currentIndex = _routes.indexWhere((r) => loc.startsWith(r));
    if (_currentIndex < 0) _currentIndex = 0;

    return Scaffold(
      body: NotificationListener<ScrollNotification>(
        onNotification: (n) { _onScroll(n); return false; },
        child: widget.child,
      ),
      extendBody: true,
      bottomNavigationBar: AnimatedBuilder(
        animation: _barCtrl,
        builder: (ctx, child) {
          return Opacity(
            opacity: _barOpacity.value,
            child: Transform.translate(
              offset: Offset(0, _barOffset.value * 100),
              child: child,
            ),
          );
        },
        child: _buildBar(),
      ),
    );
  }

  Widget _buildBar() {
    return Container(
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 14),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(28),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 35, sigmaY: 35),
          child: Container(
            decoration: BoxDecoration(
              color: AppTheme.surface.withValues(alpha: 0.5),
              border: Border.all(color: AppTheme.glassBorder, width: 0.5),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.35),
                  blurRadius: 28, offset: const Offset(0, 12),
                ),
                BoxShadow(
                  color: AppTheme.primary.withValues(alpha: 0.06),
                  blurRadius: 50, offset: const Offset(0, 0),
                ),
              ],
            ),
            child: SafeArea(
              top: false,
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 7),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: List.generate(5, (i) => _buildItem(i)),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildItem(int i) {
    final active = i == _currentIndex;
    final color = active ? AppTheme.primary : AppTheme.textTertiary;

    return GestureDetector(
      onTap: () {
        if (i != _currentIndex) {
          HapticFeedback.selectionClick();
          context.go(_routes[i]);
        }
      },
      behavior: HitTestBehavior.opaque,
      child: AnimatedContainer(
        duration: AppTheme.durNormal,
        curve: AppTheme.smoothOut,
        padding: EdgeInsets.symmetric(
          horizontal: active ? 20 : 14,
          vertical: 10,
        ),
        decoration: BoxDecoration(
          color: active
            ? AppTheme.primary.withValues(alpha: 0.1)
            : Colors.transparent,
          borderRadius: BorderRadius.circular(22),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // 图标 — 弹性缩放 + 发光
            TweenAnimationBuilder<double>(
              tween: Tween(begin: 0, end: active ? 1 : 0),
              duration: AppTheme.durNormal,
              curve: Curves.easeOutBack,
              builder: (ctx, v, child) {
                return Transform.scale(
                  scale: 1 + v * 0.18,
                  child: Container(
                    decoration: active && v > 0.5
                      ? BoxDecoration(
                          boxShadow: [
                            BoxShadow(
                              color: AppTheme.primary.withValues(alpha: v * 0.45),
                              blurRadius: 14,
                              spreadRadius: -3,
                            ),
                          ],
                        )
                      : null,
                    child: Icon(
                      active ? _activeIcons[i] : _icons[i],
                      size: 24,
                      color: color,
                    ),
                  ),
                );
              },
            ),
            const SizedBox(height: 5),
            AnimatedDefaultTextStyle(
              duration: AppTheme.durNormal,
              style: TextStyle(
                fontSize: 10.5,
                fontWeight: active ? FontWeight.w700 : FontWeight.w400,
                color: color,
                letterSpacing: 0.3,
              ),
              child: Text(_labels[i]),
            ),
          ],
        ),
      ),
    );
  }
}
