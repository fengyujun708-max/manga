import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:ui';
import 'package:go_router/go_router.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../ds.dart';
import '../../plugins/source_installer.dart';

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
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      if (!mounted) return;
      // 内置源自动提取（首次）
      final prefs = await SharedPreferences.getInstance();
      if (!(prefs.getBool('sources_extracted') ?? false)) {
        await SourceInstaller.extractBundledSources();
        await prefs.setBool('sources_extracted', true);
      }
      // 版本更新检测
      try {
        final updates = await SourceInstaller.checkUpdates();
        if (updates.isNotEmpty && mounted) {
          _showUpdateDialog(updates);
        }
      } catch (_) {}
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

  void _showUpdateDialog(List<SourceUpdate> updates) {
    showDialog(context: context, barrierDismissible: false, builder: (_) => AlertDialog(
      backgroundColor: DS.surface2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(DS.rLg)),
      title: Row(children: [
        Icon(Icons.system_update_rounded, color: DS.accent, size: 22),
        SizedBox(width: 8),
        Text('漫画源更新', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: DS.textPrimary)),
      ]),
      content: Column(mainAxisSize: MainAxisSize.min, children: [
        ...updates.take(5).map((u) => Padding(
          padding: EdgeInsets.symmetric(vertical: 4),
          child: Row(children: [
            Icon(Icons.extension_rounded, size: 16, color: DS.textTertiary),
            SizedBox(width: 8),
            Expanded(child: Text('${u.name}  ${u.currentVersion} → ${u.newVersion}', style: TextStyle(fontSize: 13, color: DS.textSecondary))),
          ]),
        )),
        if (updates.length > 5) Text('...等 ${updates.length} 个源', style: TextStyle(fontSize: 12, color: DS.textTertiary)),
      ]),
      actions: [
        TextButton(onPressed: () => Navigator.pop(context), child: Text('稍后', style: TextStyle(color: DS.textTertiary))),
        FilledButton(
          onPressed: () async {
            Navigator.pop(context);
            final count = await SourceInstaller.updateAll(updates);
            if (mounted && count > 0) {
              ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('已更新 $count 个源'), behavior: SnackBarBehavior.floating));
            }
          },
          style: FilledButton.styleFrom(backgroundColor: DS.accent),
          child: Text('立即更新'),
        ),
      ],
    ));
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