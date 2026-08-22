import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:ui';
import 'package:go_router/go_router.dart';
import '../ds.dart';
import '../../features/sources/view/source_setup_dialog.dart';

/// 漫界 App Shell — 浮动液态玻璃底栏
/// 滚动下滑隐藏 + 上滑/停止显示 + 选中微动效
class AppShell extends StatefulWidget {
  final Widget child;
  const AppShell({super.key, required this.child});
  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> with SingleTickerProviderStateMixin {
  late AnimationController _barCtrl;
  late Animation<double> _barOffset;
  late Animation<double> _barOpacity;
  bool _visible = true;
  double _lastOffset = 0;
  int _index = 0;

  static const _routes = ['/home', '/discover', '/library', '/community', '/profile'];
  static const _labels = ['首页', '发现', '书架', '社区', '我的'];
  static const _icons = [
    Icons.home_outlined, Icons.explore_outlined, Icons.bookmark_outline,
    Icons.forum_outlined, Icons.person_outline,
  ];
  static const _activeIcons = [
    Icons.home, Icons.explore, Icons.bookmark, Icons.forum, Icons.person,
  ];

  @override
  void initState() {
    super.initState();
    _barCtrl = AnimationController(duration: DS.durEmphasis, vsync: this);
    _barOffset = Tween<double>(begin: 0, end: 1).animate(CurvedAnimation(parent: _barCtrl, curve: DS.cEmphasis));
    _barOpacity = Tween<double>(begin: 1, end: 0).animate(CurvedAnimation(parent: _barCtrl, curve: Curves.easeOut));
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) SourceSetupDialog.maybeShow(context);
    });
  }

  @override
  void dispose() { _barCtrl.dispose(); super.dispose(); }

  void _onScroll(ScrollNotification n) {
    if (n is ScrollUpdateNotification) {
      final cur = n.metrics.pixels;
      final d = cur - _lastOffset;
      if (d > 4 && cur > 80) { if (_visible) { _visible = false; _barCtrl.forward(); } }
      else if (d < -4) { if (!_visible) { _visible = true; _barCtrl.reverse(); } }
      _lastOffset = cur;
    } else if (n is ScrollEndNotification) {
      if (!_visible) { _visible = true; _barCtrl.reverse(); }
    }
  }

  @override
  Widget build(BuildContext context) {
    final loc = GoRouterState.of(context).matchedLocation;
    _index = _routes.indexWhere((r) => loc.startsWith(r));
    if (_index < 0) _index = 0;

    return Scaffold(
      backgroundColor: DS.bg,
      body: NotificationListener<ScrollNotification>(
        onNotification: (n) { _onScroll(n); return false; },
        child: widget.child,
      ),
      extendBody: true,
      bottomNavigationBar: AnimatedBuilder(
        animation: _barCtrl,
        builder: (ctx, child) => Opacity(
          opacity: _barOpacity.value,
          child: Transform.translate(offset: Offset(0, _barOffset.value * 120), child: child),
        ),
        child: _buildBar(),
      ),
    );
  }

  Widget _buildBar() {
    return Container(
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 12),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(DS.rXl),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 50, sigmaY: 50),
          child: Container(
            decoration: BoxDecoration(
              color: DS.glassFillStrong,
              border: Border.all(color: DS.glassBorder, width: 0.5),
              boxShadow: [
                BoxShadow(color: Colors.black.withValues(alpha: 0.3), blurRadius: 20, offset: const Offset(0, 8)),
              ],
            ),
            child: SafeArea(
              top: false,
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 6),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: List.generate(5, (i) => _item(i)),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _item(int i) {
    final active = i == _index;
    final color = active ? DS.accent : DS.textTertiary;

    return GestureDetector(
      onTap: () {
        if (i != _index) { HapticFeedback.selectionClick(); context.go(_routes[i]); }
      },
      behavior: HitTestBehavior.opaque,
      child: AnimatedContainer(
        duration: DS.durStd,
        curve: DS.cStd,
        padding: EdgeInsets.symmetric(horizontal: active ? 18 : 12, vertical: 8),
        decoration: BoxDecoration(
          color: active ? DS.accentDim : Colors.transparent,
          borderRadius: BorderRadius.circular(20),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TweenAnimationBuilder<double>(
              tween: Tween(begin: 0, end: active ? 1 : 0),
              duration: DS.durStd,
              curve: Curves.easeOutBack,
              builder: (ctx, v, child) => Transform.scale(
                scale: 1 + v * 0.15,
                child: Icon(active ? _activeIcons[i] : _icons[i], size: 22, color: color),
              ),
            ),
            const SizedBox(height: 3),
            AnimatedDefaultTextStyle(
              duration: DS.durStd,
              style: TextStyle(
                fontSize: 10, fontWeight: active ? FontWeight.w600 : FontWeight.w400,
                color: color, letterSpacing: 0.3,
              ),
              child: Text(_labels[i]),
            ),
          ],
        ),
      ),
    );
  }
}