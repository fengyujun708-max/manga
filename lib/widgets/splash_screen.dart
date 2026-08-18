import 'package:flutter/material.dart';
import 'package:mangaverse/config/theme/mangaverse_theme.dart';

/// 闪屏页 — 品牌展示 + 加载动画
/// 在后台初始化完成前显示，提升启动感知速度
class SplashScreen extends StatefulWidget {
  final Future<void> Function() onInitialized;
  final Widget child;

  const SplashScreen({
    super.key,
    required this.onInitialized,
    required this.child,
  });

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _fadeIn;
  late Animation<double> _pulse;
  bool _isReady = false;
  String _status = '正在启动...';

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: Duration(milliseconds: 1500),
      vsync: this,
    );
    _fadeIn = CurvedAnimation(
      parent: _controller,
      curve: Curves.easeOut,
    );
    _pulse = Tween<double>(begin: 0.95, end: 1.05).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeInOut),
    );
    _controller.forward();
    _init();
  }

  Future<void> _init() async {
    await Future.delayed(Duration(milliseconds: 500)); // 至少显示闪屏
    await widget.onInitialized();
    if (mounted) {
      setState(() => _isReady = true);
      _controller.reverse().then((_) {
        if (mounted) setState(() {});
      });
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_isReady && _controller.isDismissed) return widget.child;

    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) {
        return FadeTransition(
          opacity: _fadeIn,
          child: Scaffold(
            backgroundColor: MangaVerseColors.background,
            body: Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  ScaleTransition(
                    scale: _pulse,
                    child: Icon(
                      Icons.auto_stories,
                      size: 72,
                      color: MangaVerseColors.accent,
                    ),
                  ),
                  SizedBox(height: 16),
                  Text(
                    'MangaVerse',
                    style: TextStyle(
                      fontSize: 28,
                      fontWeight: FontWeight.w800,
                      color: MangaVerseColors.foreground,
                      letterSpacing: -0.5,
                    ),
                  ),
                  SizedBox(height: 4),
                  Text(
                    '终极漫画阅读体验',
                    style: TextStyle(
                      fontSize: 13,
                      color: MangaVerseColors.mutedForeground,
                      letterSpacing: 0.3,
                    ),
                  ),
                  SizedBox(height: 48),
                  SizedBox(
                    width: 120,
                    child: LinearProgressIndicator(
                      backgroundColor: MangaVerseColors.surfaceVariant,
                      valueColor: AlwaysStoppedAnimation(MangaVerseColors.accent),
                    ),
                  ),
                  SizedBox(height: 12),
                  AnimatedSwitcher(
                    duration: Duration(milliseconds: 300),
                    child: Text(
                      _status,
                      key: ValueKey(_status),
                      style: TextStyle(
                        fontSize: 12,
                        color: MangaVerseColors.mutedForeground,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}
