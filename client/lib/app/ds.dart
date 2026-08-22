import 'package:flutter/material.dart';
import 'dart:ui';

/// 漫界 Cinematic Design System v2.0
/// 95% 中性色 + 5% 品牌红 — 黑白灰为主，红色仅做 Accent
class DS {
  // ── 色彩 ──
  static const Color bg = Color(0xFF08090B);
  static const Color surface1 = Color(0xFF101114);
  static const Color surface2 = Color(0xFF16181C);
  static const Color surface3 = Color(0xFF1C1E22);

  static const Color textPrimary = Color(0xFFF5F5F5);
  static const Color textSecondary = Color(0xFFA1A1AA);
  static const Color textTertiary = Color(0xFF71717A);
  static const Color textDisabled = Color(0xFF52525B);

  static const Color accent = Color(0xFFFF3B30);
  static const Color accentDim = Color(0x14FF3B30);
  static const Color success = Color(0xFF34D399);
  static const Color warning = Color(0xFFFBBF24);
  static const Color error = Color(0xFFEF4444);

  // ── 玻璃 ──
  static const Color glassFill = Color(0x0AFFFFFF);
  static const Color glassFillStrong = Color(0x14FFFFFF);
  static const Color glassBorder = Color(0x14FFFFFF);

  // ── 渐变 ──
  static const LinearGradient heroScrim = LinearGradient(
    begin: Alignment.topCenter, end: Alignment.bottomCenter,
    colors: [Color(0x00000000), Color(0xE608090B)],
    stops: [0.3, 1.0],
  );
  static const LinearGradient cardScrim = LinearGradient(
    begin: Alignment.topCenter, end: Alignment.bottomCenter,
    colors: [Color(0x00000000), Color(0xB3000000)],
    stops: [0.5, 1.0],
  );

  // ── 间距 ──
  static const double sp4 = 4, sp8 = 8, sp12 = 12, sp16 = 16, sp20 = 20, sp24 = 24, sp32 = 32, sp48 = 48;

  // ── 圆角 ──
  static const double rSm = 10, rMd = 14, rLg = 20, rXl = 28;

  // ── 动效 ──
  static const Duration durMicro = Duration(milliseconds: 140);
  static const Duration durStd = Duration(milliseconds: 200);
  static const Duration durEmphasis = Duration(milliseconds: 350);
  static const Duration durHero = Duration(milliseconds: 700);
  static const Curve cMicro = Curves.easeOut;
  static const Curve cStd = Curves.easeOutCubic;
  static const Curve cEmphasis = Curves.easeInOutCubic;
  static const Curve cHero = Curves.easeOutBack;

  // ── 文字样式 ──
  static const TextStyle display = TextStyle(fontSize: 34, fontWeight: FontWeight.w700, color: textPrimary, letterSpacing: -1.0, height: 1.1);
  static const TextStyle headline = TextStyle(fontSize: 24, fontWeight: FontWeight.w700, color: textPrimary, letterSpacing: -0.5, height: 1.2);
  static const TextStyle title = TextStyle(fontSize: 18, fontWeight: FontWeight.w600, color: textPrimary, letterSpacing: -0.3, height: 1.3);
  static const TextStyle body = TextStyle(fontSize: 15, fontWeight: FontWeight.w400, color: textPrimary, height: 1.5);
  static const TextStyle bodySec = TextStyle(fontSize: 15, fontWeight: FontWeight.w400, color: textSecondary, height: 1.5);
  static const TextStyle caption = TextStyle(fontSize: 13, fontWeight: FontWeight.w400, color: textTertiary, letterSpacing: 0.2, height: 1.4);
  static const TextStyle micro = TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: textTertiary, letterSpacing: 0.5);
}

/// 真正的液态玻璃组件 — Apple Liquid Glass
class Glass extends StatelessWidget {
  final Widget child;
  final double blur;
  final Color? fill;
  final Color? border;
  final double radius;
  final EdgeInsets padding;
  final EdgeInsets? margin;

  const Glass({
    super.key,
    required this.child,
    this.blur = 50,
    this.fill,
    this.border,
    this.radius = DS.rLg,
    this.padding = const EdgeInsets.all(0),
    this.margin,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: margin,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(radius),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: blur, sigmaY: blur),
          child: Container(
            padding: padding,
            decoration: BoxDecoration(
              color: fill ?? DS.glassFill,
              borderRadius: BorderRadius.circular(radius),
              border: Border.all(color: border ?? DS.glassBorder, width: 0.5),
            ),
            child: child,
          ),
        ),
      ),
    );
  }
}

/// 弹性按钮 — 按压 scale 0.97 + 品牌色渐变
class SpringButton extends StatefulWidget {
  final Widget child;
  final VoidCallback? onPressed;
  final Color? color;
  final double radius;
  final EdgeInsets padding;

  const SpringButton({
    super.key,
    required this.child,
    this.onPressed,
    this.color,
    this.radius = DS.rMd,
    this.padding = const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
  });

  @override
  State<SpringButton> createState() => _SpringButtonState();
}

class _SpringButtonState extends State<SpringButton> with SingleTickerProviderStateMixin {
  late AnimationController _ctrl;
  late Animation<double> _scale;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(duration: DS.durMicro, vsync: this);
    _scale = Tween<double>(begin: 1, end: 0.97).animate(CurvedAnimation(parent: _ctrl, curve: DS.cMicro));
  }

  @override
  void dispose() { _ctrl.dispose(); super.dispose(); }

  @override
  Widget build(BuildContext context) {
    final enabled = widget.onPressed != null;
    return GestureDetector(
      onTapDown: enabled ? (_) { _ctrl.forward(); HapticFeedback.selectionClick(); } : null,
      onTapUp: enabled ? (_) { _ctrl.reverse(); widget.onPressed!(); } : null,
      onTapCancel: () => _ctrl.reverse(),
      child: AnimatedBuilder(
        animation: _scale,
        builder: (ctx, child) => Transform.scale(scale: _scale.value, child: child),
        child: Container(
          padding: widget.padding,
          decoration: BoxDecoration(
            color: widget.color ?? DS.accent,
            borderRadius: BorderRadius.circular(widget.radius),
            boxShadow: enabled ? [
              BoxShadow(color: (widget.color ?? DS.accent).withValues(alpha: 0.25), blurRadius: 20, offset: const Offset(0, 6)),
            ] : null,
          ),
          child: DefaultTextStyle(
            style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: Colors.white),
            child: widget.child,
          ),
        ),
      ),
    );
  }
}

/// 漫画卡片 — 封面 + 标题 + 元数据 (支持长按)
class ComicCard extends StatelessWidget {
  final String cover;
  final String title;
  final String? subtitle;
  final String? badge;
  final double? progress; // 0-1 阅读进度
  final double width;
  final VoidCallback? onTap;
  final VoidCallback? onLongPress;

  const ComicCard({
    super.key,
    required this.cover,
    required this.title,
    this.subtitle,
    this.badge,
    this.progress,
    this.width = 130,
    this.onTap,
    this.onLongPress,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      onLongPress: onLongPress,
      child: SizedBox(
        width: width,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 封面
            Stack(
              children: [
                ClipRRect(
                  borderRadius: BorderRadius.circular(DS.rMd),
                  child: AspectRatio(
                    aspectRatio: 0.72,
                    child: cover.isNotEmpty
                      ? Image.network(cover, fit: BoxFit.cover,
                          errorBuilder: (_, __, ___) => _coverFallback())
                      : _coverFallback(),
                  ),
                ),
                // 底部渐隐
                Positioned.fill(child: Container(
                  decoration: const BoxDecoration(
                    borderRadius: BorderRadius.all(Radius.circular(DS.rMd)),
                    gradient: DS.cardScrim,
                  ),
                )),
                // 徽章
                if (badge != null)
                  Positioned(top: 6, left: 6,
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(color: DS.accent, borderRadius: BorderRadius.circular(DS.rSm)),
                      child: Text(badge!, style: const TextStyle(fontSize: 9, fontWeight: FontWeight.w700, color: Colors.white)),
                    ),
                  ),
                // 阅读进度条
                if (progress != null)
                  Positioned(bottom: 0, left: 0, right: 0,
                    child: ClipRRect(
                      borderRadius: const BorderRadius.vertical(bottom: Radius.circular(DS.rMd)),
                      child: LinearProgressIndicator(
                        value: progress, minHeight: 3,
                        backgroundColor: Colors.transparent,
                        valueColor: const AlwaysStoppedAnimation(DS.accent),
                      ),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 6),
            Text(title, maxLines: 2, overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500, color: DS.textPrimary, height: 1.3)),
            if (subtitle != null)
              Text(subtitle!, maxLines: 1, overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontSize: 11, color: DS.textTertiary)),
          ],
        ),
      ),
    );
  }

  Widget _coverFallback() => Container(
    color: DS.surface2,
    child: const Center(child: Icon(Icons.menu_book_rounded, size: 28, color: DS.textDisabled)),
  );
}

/// 继续阅读卡片 — 横向大卡 + 进度
class ContinueReadingCard extends StatelessWidget {
  final String cover;
  final String title;
  final String chapter;
  final double progress;
  final VoidCallback? onTap;

  const ContinueReadingCard({
    super.key,
    required this.cover,
    required this.title,
    required this.chapter,
    required this.progress,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Glass(
        radius: DS.rLg,
        padding: const EdgeInsets.all(12),
        fill: DS.glassFill,
        child: Row(
          children: [
            ClipRRect(
              borderRadius: BorderRadius.circular(DS.rMd),
              child: SizedBox(
                width: 64, height: 90,
                child: cover.isNotEmpty
                  ? Image.network(cover, fit: BoxFit.cover,
                      errorBuilder: (_, __, ___) => Container(color: DS.surface2))
                  : Container(color: DS.surface2),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, maxLines: 1, overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600, color: DS.textPrimary)),
                const SizedBox(height: 4),
                Text(chapter, style: const TextStyle(fontSize: 13, color: DS.textTertiary)),
                const Spacer(),
                // 进度条
                ClipRRect(
                  borderRadius: BorderRadius.circular(2),
                  child: LinearProgressIndicator(
                    value: progress, minHeight: 4,
                    backgroundColor: DS.surface3,
                    valueColor: const AlwaysStoppedAnimation(DS.accent),
                  ),
                ),
                const SizedBox(height: 4),
                Text('${(progress * 100).toInt()}%', style: const TextStyle(fontSize: 11, color: DS.textTertiary)),
              ],
            )),
            const SizedBox(width: 8),
            const Icon(Icons.play_circle_fill_rounded, size: 36, color: DS.accent),
          ],
        ),
      ),
    );
  }
}

/// 区块标题 — 标题 + 可选"更多"
class SectionHeader extends StatelessWidget {
  final String title;
  final String? subtitle;
  final VoidCallback? onMore;

  const SectionHeader({super.key, required this.title, this.subtitle, this.onMore});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(DS.sp16, DS.sp20, DS.sp16, DS.sp12),
      child: Row(
        children: [
          Text(title, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w700, color: DS.textPrimary, letterSpacing: -0.5)),
          if (subtitle != null) ...[
            const SizedBox(width: 8),
            Text(subtitle!, style: const TextStyle(fontSize: 13, color: DS.textTertiary)),
          ],
          const Spacer(),
          if (onMore != null)
            GestureDetector(
              onTap: onMore,
              child: const Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text('更多', style: TextStyle(fontSize: 13, color: DS.textTertiary)),
                  SizedBox(width: 2),
                  Icon(Icons.chevron_right_rounded, size: 16, color: DS.textTertiary),
                ],
              ),
            ),
        ],
      ),
    );
  }
}

/// 骨架屏
class Shimmer extends StatefulWidget {
  final double width;
  final double height;
  final double radius;
  const Shimmer({super.key, this.width = double.infinity, required this.height, this.radius = DS.rMd});

  @override
  State<Shimmer> createState() => _ShimmerState();
}

class _ShimmerState extends State<Shimmer> with SingleTickerProviderStateMixin {
  late AnimationController _ctrl;
  late Animation<double> _anim;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(duration: const Duration(milliseconds: 1200), vsync: this)..repeat();
    _anim = Tween<double>(begin: -1, end: 2).animate(CurvedAnimation(parent: _ctrl, curve: Curves.easeInOut));
  }

  @override
  void dispose() { _ctrl.dispose(); super.dispose(); }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _anim,
      builder: (ctx, _) {
        return Container(
          width: widget.width, height: widget.height,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(widget.radius),
            gradient: LinearGradient(
              begin: Alignment(_anim.value - 0.3, 0),
              end: Alignment(_anim.value + 0.3, 0),
              colors: [DS.surface1, DS.surface2, DS.surface1],
            ),
          ),
        );
      },
    );
  }
}

/// 空状态
class EmptyState extends StatelessWidget {
  final IconData icon;
  final String title;
  final String? subtitle;
  final String? actionLabel;
  final VoidCallback? onAction;

  const EmptyState({super.key, required this.icon, required this.title, this.subtitle, this.actionLabel, this.onAction});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(DS.sp32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, size: 56, color: DS.textDisabled),
            const SizedBox(height: DS.sp16),
            Text(title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600, color: DS.textSecondary)),
            if (subtitle != null) ...[
              const SizedBox(height: 6),
              Text(subtitle!, textAlign: TextAlign.center, style: const TextStyle(fontSize: 13, color: DS.textTertiary)),
            ],
            if (actionLabel != null && onAction != null) ...[
              const SizedBox(height: DS.sp24),
              SpringButton(onPressed: onAction, child: Text(actionLabel!)),
            ],
          ],
        ),
      ),
    );
  }
}

/// 入场动画
class FadeSlideIn extends StatelessWidget {
  final Widget child;
  final Duration delay;
  const FadeSlideIn({super.key, required this.child, this.delay = Duration.zero});

  @override
  Widget build(BuildContext context) {
    return _FadeSlideIn(delay: delay, child: child);
  }
}

class _FadeSlideIn extends StatefulWidget {
  final Widget child;
  final Duration delay;
  const _FadeSlideIn({required this.child, required this.delay});
  @override
  State<_FadeSlideIn> createState() => _FadeSlideInState();
}

class _FadeSlideInState extends State<_FadeSlideIn> with SingleTickerProviderStateMixin {
  late AnimationController _ctrl;
  late Animation<double> _fade;
  late Animation<Offset> _slide;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(duration: DS.durStd, vsync: this);
    _fade = CurvedAnimation(parent: _ctrl, curve: DS.cStd);
    _slide = Tween<Offset>(begin: const Offset(0, 0.04), end: Offset.zero)
        .animate(CurvedAnimation(parent: _ctrl, curve: DS.cStd));
    Future.delayed(widget.delay, () => _ctrl.forward());
  }

  @override
  void dispose() { _ctrl.dispose(); super.dispose(); }

  @override
  Widget build(BuildContext context) {
    return FadeTransition(
      opacity: _fade,
      child: SlideTransition(position: _slide, child: widget.child),
    );
  }
}