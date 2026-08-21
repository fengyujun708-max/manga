import 'package:flutter/material.dart';
import 'dart:ui';
import 'package:flutter/services.dart';

// ============================================================
//  漫界设计系统 v3 — Apple Liquid Glass × 深空沉浸
//  参考：iOS 26 Liquid Glass / Apple Music / Apple Books
// ============================================================

class AppTheme {
  // ── 品牌色 ──
  static const Color primary = Color(0xFF6366F1);     // 紫罗兰
  static const Color primaryDark = Color(0xFF4F46E5);
  static const Color primaryLight = Color(0xFF818CF8);
  static const Color accent = Color(0xFFF59E0B);       // 琥珀金
  static const Color accentWarm = Color(0xFFFB923C);

  // ── 深空背景 ──
  static const Color background = Color(0xFF000000);   // 纯黑（OLED友好）
  static const Color backgroundElevated = Color(0xFF0A0A0F);
  static const Color surface = Color(0xFF111118);
  static const Color surfaceLight = Color(0xFF1A1A24);
  static const Color surfaceDeep = Color(0xFF08080C);

  // ── 玻璃层（白色低alpha，Apple标准）──
  static const Color glassFillLight = Color(0x14FFFFFF);   // 0.08 白
  static const Color glassFillRegular = Color(0x0AFFFFFF); // 0.04 白
  static const Color glassFillThick = Color(0x1FFFFFFF);   // 0.12 白
  static const Color glassBorder = Color(0x24FFFFFF);      // 0.14 白 边框
  static const Color glassBorderHighlight = Color(0x38FFFFFF); // 0.22 顶部高光

  // ── 文字 ──
  static const Color textPrimary = Color(0xFFF5F5F7);   // Apple 白
  static const Color textSecondary = Color(0xFF8E8E93);  // Apple 灰
  static const Color textTertiary = Color(0xFF636366);

  // ── 语义 ──
  static const Color success = Color(0xFF34C759);  // Apple 绿
  static const Color destructive = Color(0xFFFF3B30); // Apple 红
  static const Color warning = Color(0xFFFF9500);  // Apple 橙

  // ── 渐变 ──
  static const LinearGradient primaryGradient = LinearGradient(
    colors: [Color(0xFF6366F1), Color(0xFF4F46E5)],
    begin: Alignment.topLeft, end: Alignment.bottomRight,
  );
  static const LinearGradient accentGradient = LinearGradient(
    colors: [Color(0xFFF59E0B), Color(0xFFFB923C)],
    begin: Alignment.topLeft, end: Alignment.bottomRight,
  );
  // 沉浸式 Hero 渐变（从透明到纯黑）
  static const LinearGradient heroScrim = LinearGradient(
    begin: Alignment.topCenter, end: Alignment.bottomCenter,
    colors: [Color(0x00000000), Color(0x80000000), Color(0xFF000000)],
    stops: [0, 0.5, 1],
  );
  // 玻璃卡片渐变
  static const LinearGradient glassCardGradient = LinearGradient(
    begin: Alignment.topLeft, end: Alignment.bottomRight,
    colors: [Color(0x14FFFFFF), Color(0x08FFFFFF)],
  );

  // ── 阴影 ──
  static List<BoxShadow> get cardShadow => [
    BoxShadow(color: Colors.black.withValues(alpha: 0.5), blurRadius: 24, offset: const Offset(0, 8)),
  ];
  static List<BoxShadow> get softShadow => [
    BoxShadow(color: Colors.black.withValues(alpha: 0.25), blurRadius: 12, offset: const Offset(0, 4)),
  ];
  static List<BoxShadow> get glowShadow => [
    BoxShadow(color: primary.withValues(alpha: 0.3), blurRadius: 40, spreadRadius: 0),
  ];

  // ── 圆角（Apple 连续圆角感）──
  static const double radiusSm = 12;
  static const double radiusMd = 18;
  static const double radiusLg = 24;
  static const double radiusXl = 32;
  static const double radiusHero = 40;

  // ── 动画曲线（Apple spring）──
  static const Curve springOut = Curves.easeOutBack;
  static const Curve smoothOut = Curves.easeOutCubic;
  static const Curve smoothInOut = Curves.easeInOutCubic;
  // Apple 的弹性曲线
  static const Curve appleSpring = Curves.easeOutBack;
  static const Curve appleDecel = Curves.decelerate;

  // ── 动画时长 ──
  static const Duration durFast = Duration(milliseconds: 250);
  static const Duration durNormal = Duration(milliseconds: 400);
  static const Duration durSlow = Duration(milliseconds: 700);

  static ThemeData get dark => ThemeData(
    brightness: Brightness.dark,
    scaffoldBackgroundColor: background,
    primaryColor: primary,
    colorScheme: const ColorScheme.dark(
      primary: primary, secondary: accent, surface: surface, error: destructive,
    ),
    appBarTheme: const AppBarTheme(
      backgroundColor: Colors.transparent, elevation: 0, centerTitle: true,
      systemOverlayStyle: SystemUiOverlayStyle.light,
    ),
    textTheme: const TextTheme(
      headlineLarge: TextStyle(fontSize: 30, fontWeight: FontWeight.w800, color: textPrimary, letterSpacing: -0.8, height: 1.1),
      headlineMedium: TextStyle(fontSize: 24, fontWeight: FontWeight.w700, color: textPrimary, letterSpacing: -0.5),
      titleLarge: TextStyle(fontSize: 20, fontWeight: FontWeight.w700, color: textPrimary, letterSpacing: -0.3),
      titleMedium: TextStyle(fontSize: 16, fontWeight: FontWeight.w600, color: textPrimary),
      bodyLarge: TextStyle(fontSize: 16, color: textPrimary, height: 1.5),
      bodyMedium: TextStyle(fontSize: 14, color: textSecondary, height: 1.4),
      bodySmall: TextStyle(fontSize: 12, color: textTertiary),
      labelSmall: TextStyle(fontSize: 11, color: textTertiary, fontWeight: FontWeight.w500),
    ),
    iconTheme: const IconThemeData(color: textPrimary, size: 24),
  );
  static ThemeData get light => dark;
}

// ============================================================
//  真正的 Apple Liquid Glass —— 毛玻璃 + 顶部高光 + 内容折射
// ============================================================
class LiquidGlass extends StatelessWidget {
  final Widget child;
  final double blur;
  final BorderRadius? radius;
  final EdgeInsets? padding;
  final EdgeInsets? margin;
  final Color? fillColor;
  final Color? borderColor;
  final List<BoxShadow>? shadows;
  final bool topHighlight;  // 顶部1px高光线
  final double borderWidth;

  const LiquidGlass({
    super.key, required this.child,
    this.blur = 50,
    this.radius, this.padding, this.margin,
    this.fillColor, this.borderColor, this.shadows,
    this.topHighlight = true,
    this.borderWidth = 0.5,
  });

  @override
  Widget build(BuildContext context) {
    final r = radius ?? BorderRadius.circular(AppTheme.radiusLg);
    return Container(
      margin: margin,
      child: ClipRRect(
        borderRadius: r,
        child: Stack(
          children: [
            // 1. 毛玻璃模糊层（Apple标准 sigma 40-80）
            BackdropFilter(
              filter: ImageFilter.blur(sigmaX: blur, sigmaY: blur),
              child: Container(
                decoration: BoxDecoration(
                  color: fillColor ?? AppTheme.glassFillRegular,
                  borderRadius: r,
                ),
              ),
            ),
            // 2. 玻璃边框
            Container(
              decoration: BoxDecoration(
                borderRadius: r,
                border: Border.all(
                  color: borderColor ?? AppTheme.glassBorder,
                  width: borderWidth,
                ),
              ),
            ),
            // 3. 顶部高光线（Apple 玻璃特征）
            if (topHighlight)
              Positioned(
                top: 0, left: 0, right: 0, height: 1,
                child: Container(
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.only(
                      topLeft: Radius.circular(r.topLeft.x),
                      topRight: Radius.circular(r.topRight.x),
                    ),
                    gradient: LinearGradient(
                      begin: Alignment.centerLeft, end: Alignment.centerRight,
                      colors: [
                        Colors.transparent,
                        AppTheme.glassBorderHighlight,
                        Colors.transparent,
                      ],
                      stops: [0.1, 0.5, 0.9],
                    ),
                  ),
                ),
              ),
            // 4. 阴影
            if (shadows != null)
              Container(
                decoration: BoxDecoration(
                  borderRadius: r,
                  boxShadow: shadows,
                ),
              ),
            // 5. 内容
            if (padding != null)
              Padding(padding: padding!, child: child)
            else
              child,
          ],
        ),
      ),
    );
  }
}

// ============================================================
//  弹性按钮 — Apple 风格按压弹性 + 触觉 + 光晕
// ============================================================
class SpringButton extends StatefulWidget {
  final Widget child;
  final VoidCallback? onPressed;
  final LinearGradient? gradient;
  final double? width;
  final double height;
  final BorderRadius? borderRadius;

  const SpringButton({
    super.key, required this.child, this.onPressed, this.gradient,
    this.width, this.height = 50, this.borderRadius,
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
    _ctrl = AnimationController(duration: AppTheme.durFast, vsync: this);
    _scale = Tween<double>(begin: 1, end: 0.96).animate(CurvedAnimation(parent: _ctrl, curve: AppTheme.smoothOut));
  }

  @override
  void dispose() { _ctrl.dispose(); super.dispose(); }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTapDown: (_) { _ctrl.forward(); HapticFeedback.lightImpact(); },
      onTapUp: (_) { _ctrl.reverse(); widget.onPressed?.call(); },
      onTapCancel: () => _ctrl.reverse(),
      child: AnimatedBuilder(
        animation: _scale,
        builder: (ctx, child) => Transform.scale(scale: _scale.value, child: child),
        child: Container(
          width: widget.width ?? double.infinity,
          height: widget.height,
          decoration: BoxDecoration(
            gradient: widget.gradient ?? AppTheme.primaryGradient,
            borderRadius: widget.borderRadius ?? BorderRadius.circular(AppTheme.radiusMd),
            boxShadow: widget.onPressed != null ? AppTheme.glowShadow : null,
          ),
          child: Material(color: Colors.transparent, child: Center(child: widget.child)),
        ),
      ),
    );
  }
}

// ============================================================
//  骨架屏 — Apple 风格渐变扫光
// ============================================================
class ShimmerBox extends StatefulWidget {
  final double width, height;
  final BorderRadius? radius;
  const ShimmerBox({super.key, required this.width, required this.height, this.radius});

  @override
  State<ShimmerBox> createState() => _ShimmerBoxState();
}

class _ShimmerBoxState extends State<ShimmerBox> with SingleTickerProviderStateMixin {
  late AnimationController _ctrl;
  late Animation<double> _anim;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(duration: const Duration(milliseconds: 1600), vsync: this)..repeat();
    _anim = Tween<double>(begin: -1.5, end: 2.5).animate(CurvedAnimation(parent: _ctrl, curve: Curves.easeInOut));
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
            borderRadius: widget.radius ?? BorderRadius.circular(AppTheme.radiusMd),
            gradient: LinearGradient(
              begin: Alignment(_anim.value - 0.5, 0),
              end: Alignment(_anim.value + 0.5, 0),
              colors: [
                AppTheme.surfaceLight,
                AppTheme.surfaceLight.withValues(alpha: 0.4),
                AppTheme.surfaceLight,
              ],
            ),
          ),
        );
      },
    );
  }
}

// ============================================================
//  淡入上滑入场动画
// ============================================================
class FadeSlideIn extends StatefulWidget {
  final Widget child;
  final Duration delay;
  const FadeSlideIn({super.key, required this.child, this.delay = Duration.zero});

  @override
  State<FadeSlideIn> createState() => _FadeSlideInState();
}

class _FadeSlideInState extends State<FadeSlideIn> with SingleTickerProviderStateMixin {
  late AnimationController _ctrl;
  late Animation<double> _fade;
  late Animation<Offset> _slide;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(duration: AppTheme.durNormal, vsync: this);
    _fade = CurvedAnimation(parent: _ctrl, curve: AppTheme.smoothOut);
    _slide = Tween<Offset>(begin: const Offset(0, 0.02), end: Offset.zero)
        .animate(CurvedAnimation(parent: _ctrl, curve: AppTheme.smoothOut));
    Future.delayed(widget.delay, () => mounted ? _ctrl.forward() : null);
  }

  @override
  void dispose() { _ctrl.dispose(); super.dispose(); }

  @override
  Widget build(BuildContext context) {
    return FadeTransition(opacity: _fade, child: SlideTransition(position: _slide, child: widget.child));
  }
}