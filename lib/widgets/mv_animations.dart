import 'package:flutter/material.dart';
import 'package:mangaverse/config/theme/mangaverse_theme.dart';

/// 卡片入场动画 — 从底部滑入 + 淡入
class MVCardEntrance extends StatefulWidget {
  final Widget child;
  final int index;
  final Duration delayPerItem;

  const MVCardEntrance({
    super.key,
    required this.child,
    this.index = 0,
    this.delayPerItem = const Duration(milliseconds: 40),
  });

  @override
  State<MVCardEntrance> createState() => _MVCardEntranceState();
}

class _MVCardEntranceState extends State<MVCardEntrance>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _fadeAnim;
  late Animation<Offset> _slideAnim;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: MangaVerseAnimations.normal,
      vsync: this,
    );
    final delay = widget.delayPerItem * widget.index;
    final curve = MangaVerseAnimations.easeOutExpo;

    _fadeAnim = CurvedAnimation(
      parent: _controller,
      curve: curve,
    );
    _slideAnim = Tween<Offset>(
      begin: Offset(0, 0.08),
      end: Offset.zero,
    ).animate(CurvedAnimation(parent: _controller, curve: curve));

    Future.delayed(delay, () => _controller.forward());
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return FadeTransition(
      opacity: _fadeAnim,
      child: SlideTransition(
        position: _slideAnim,
        child: widget.child,
      ),
    );
  }
}

/// 弹性缩放按钮反馈
class MVScaleTap extends StatefulWidget {
  final Widget child;
  final VoidCallback? onTap;
  final double scale;

  const MVScaleTap({
    super.key,
    required this.child,
    this.onTap,
    this.scale = 0.96,
  });

  @override
  State<MVScaleTap> createState() => _MVScaleTapState();
}

class _MVScaleTapState extends State<MVScaleTap>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _scaleAnim;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: Duration(milliseconds: 150),
      vsync: this,
    );
    _scaleAnim = TweenSequence([
      TweenSequenceItem(tween: Tween(begin: 1.0, end: widget.scale), weight: 1),
      TweenSequenceItem(tween: Tween(begin: widget.scale, end: 1.0), weight: 1),
    ]).animate(CurvedAnimation(
      parent: _controller,
      curve: MangaVerseAnimations.easeOutBack,
    ));
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        _controller.forward().then((_) => _controller.reverse());
        widget.onTap?.call();
      },
      child: AnimatedBuilder(
        animation: _scaleAnim,
        builder: (context, child) => Transform.scale(
          scale: _scaleAnim.value,
          child: child,
        ),
      ),
    );
  }
}

/// 数字滚动动画 (用于进度/评分)
class MVAnimatedCount extends StatelessWidget {
  final int value;
  final TextStyle? style;

  const MVAnimatedCount({super.key, required this.value, this.style});

  @override
  Widget build(BuildContext context) {
    return TweenAnimationBuilder<int>(
      tween: IntTween(begin: 0, end: value),
      duration: MangaVerseAnimations.slow,
      curve: MangaVerseAnimations.easeOutExpo,
      builder: (context, value, _) => Text('$value', style: style),
    );
  }
}
