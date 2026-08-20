import 'package:flutter/material.dart';
import 'dart:ui';
import 'package:flutter/services.dart';

// ============================================================
//  漫界顶级设计系统 — Liquid Glass × 暗黑沉浸 × 丝滑弹簧动画
// ============================================================

class AppTheme {
  // 品牌色
  static const Color primary = Color(0xFF818CF8);
  static const Color primaryDark = Color(0xFF6366F1);
  static const Color primaryLight = Color(0xFFA5B4FC);
  static const Color accent = Color(0xFFF59E0B);
  static const Color accentWarm = Color(0xFFFB923C);

  // 表面
  static const Color surface = Color(0xFF1B1B30);
  static const Color surfaceLight = Color(0xFF27273B);
  static const Color surfaceDeep = Color(0xFF131326);
  static const Color cardColor = Color(0xFF1B1B30);

  // 背景
  static const Color background = Color(0xFF0F0F23);
  static const Color backgroundDeep = Color(0xFF080814);

  // 文字
  static const Color textPrimary = Color(0xFFF8FAFC);
  static const Color textSecondary = Color(0xFF94A3B8);
  static const Color textTertiary = Color(0xFF64748B);

  // 线条
  static const Color divider = Color(0xFF312E81);
  static const Color glassBorder = Color(0x1AFFFFFF);
  static const Color glassFill = Color(0x0FFFFFFF);

  // 语义
  static const Color success = Color(0xFF22C55E);
  static const Color destructive = Color(0xFFEF4444);
  static const Color ring = Color(0xFF818CF8);

  // 渐变
  static const LinearGradient primaryGradient = LinearGradient(
    colors: [Color(0xFF818CF8), Color(0xFF6366F1)],
    begin: Alignment.topLeft, end: Alignment.bottomRight,
  );
  static const LinearGradient accentGradient = LinearGradient(
    colors: [Color(0xFFF59E0B), Color(0xFFFB923C)],
    begin: Alignment.topLeft, end: Alignment.bottomRight,
  );
  static const LinearGradient heroGradient = LinearGradient(
    colors: [Color(0xFF1B1B30), Color(0xFF0F0F23), Color(0xFF080814)],
    begin: Alignment.topCenter, end: Alignment.bottomCenter,
  );
  static const LinearGradient cardGradient = LinearGradient(
    colors: [Color(0xFF27273B), Color(0xFF1B1B30)],
    begin: Alignment.topLeft, end: Alignment.bottomRight,
  );
  // 新增：更多渐变
  static const LinearGradient sunsetGradient = LinearGradient(
    colors: [Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF818CF8)],
    begin: Alignment.topLeft, end: Alignment.bottomRight,
  );
  static const LinearGradient oceanGradient = LinearGradient(
    colors: [Color(0xFF6366F1), Color(0xFF312E81), Color(0xFF0F0F23)],
    begin: Alignment.topCenter, end: Alignment.bottomCenter,
  );

  // 阴影
  static List<BoxShadow> get cardShadow => [
    BoxShadow(color: Colors.black.withValues(alpha: 0.4), blurRadius: 20, offset: const Offset(0, 8)),
  ];
  static List<BoxShadow> get glowShadow => [
    BoxShadow(color: AppTheme.primary.withValues(alpha: 0.4), blurRadius: 30, offset: const Offset(0, 0)),
  ];
  static List<BoxShadow> get accentGlow => [
    BoxShadow(color: AppTheme.accent.withValues(alpha: 0.35), blurRadius: 24, offset: const Offset(0, 0)),
  ];
  static List<BoxShadow> get softShadow => [
    BoxShadow(color: Colors.black.withValues(alpha: 0.2), blurRadius: 8, offset: const Offset(0, 2)),
  ];

  // 圆角
  static const double radiusSm = 10;
  static const double radiusMd = 16;
  static const double radiusLg = 22;
  static const double radiusXl = 32;

  // 弹簧曲线
  static const Curve springOut = Curves.elasticOut;
  static const Curve springIn = Curves.easeOutBack;
  static const Curve smoothOut = Curves.easeOutCubic;
  static const Curve smoothInOut = Curves.easeInOutCubic;

  // 动画时长
  static const Duration durFast = Duration(milliseconds: 200);
  static const Duration durNormal = Duration(milliseconds: 350);
  static const Duration durSlow = Duration(milliseconds: 600);
  static const Duration durPage = Duration(milliseconds: 500);

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
      titleTextStyle: TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: textPrimary),
    ),
    cardTheme: CardThemeData(
      color: surface, elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(radiusLg)),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true, fillColor: surfaceLight,
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(radiusMd), borderSide: BorderSide.none),
      focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(radiusMd),
        borderSide: const BorderSide(color: primary, width: 1.5)),
      contentPadding: const EdgeInsets.symmetric(horizontal: 18, vertical: 16),
      labelStyle: const TextStyle(color: textSecondary, fontSize: 14),
      hintStyle: const TextStyle(color: textTertiary),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: primary, foregroundColor: Colors.white, elevation: 0,
        padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 24),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(radiusMd)),
      ),
    ),
    textTheme: const TextTheme(
      headlineLarge: TextStyle(fontSize: 28, fontWeight: FontWeight.w800, color: textPrimary, letterSpacing: -0.5),
      headlineMedium: TextStyle(fontSize: 22, fontWeight: FontWeight.w700, color: textPrimary, letterSpacing: -0.3),
      titleLarge: TextStyle(fontSize: 18, fontWeight: FontWeight.w600, color: textPrimary),
      titleMedium: TextStyle(fontSize: 16, fontWeight: FontWeight.w500, color: textPrimary),
      bodyLarge: TextStyle(fontSize: 16, color: textPrimary, height: 1.5),
      bodyMedium: TextStyle(fontSize: 14, color: textSecondary, height: 1.4),
      bodySmall: TextStyle(fontSize: 12, color: textTertiary),
    ),
    iconTheme: const IconThemeData(color: textPrimary, size: 24),
    dividerTheme: const DividerThemeData(color: divider, thickness: 0.5, space: 1),
    snackBarTheme: SnackBarThemeData(
      backgroundColor: surfaceLight, contentTextStyle: const TextStyle(color: textPrimary),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(radiusSm)),
      behavior: SnackBarBehavior.floating,
    ),
  );

  static ThemeData get light => dark;
}

// ============================================================
//  液态玻璃容器 — 多层模糊 + 高光边框 + 内发光
// ============================================================
class LiquidGlass extends StatefulWidget {
  final Widget child;
  final double blur;
  final BorderRadius? radius;
  final EdgeInsets? padding;
  final EdgeInsets? margin;
  final Color? fillColor;
  final Color? borderColor;
  final List<BoxShadow>? shadows;
  final bool enableHaptic;

  const LiquidGlass({
    super.key, required this.child,
    this.blur = 30, this.radius, this.padding, this.margin,
    this.fillColor, this.borderColor, this.shadows, this.enableHaptic = false,
  });

  @override
  State<LiquidGlass> createState() => _LiquidGlassState();
}

class _LiquidGlassState extends State<LiquidGlass> with SingleTickerProviderStateMixin {
  late AnimationController _hapticCtrl;
  late Animation<double> _haptic;

  @override
  void initState() {
    super.initState();
    _hapticCtrl = AnimationController(duration: AppTheme.durFast, vsync: this);
    _haptic = Tween<double>(begin: 1, end: 0.97).animate(CurvedAnimation(parent: _hapticCtrl, curve: AppTheme.smoothOut));
  }

  @override
  void dispose() {
    _hapticCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final r = widget.radius ?? BorderRadius.circular(AppTheme.radiusLg);
    return AnimatedBuilder(
      animation: _haptic,
      builder: (ctx, child) => Transform.scale(scale: _haptic.value, child: child),
      child: Container(
        margin: widget.margin,
        child: ClipRRect(
          borderRadius: r,
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: widget.blur, sigmaY: widget.blur),
            child: Container(
              padding: widget.padding ?? const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: widget.fillColor ?? AppTheme.glassFill,
                borderRadius: r,
                border: Border.all(color: widget.borderColor ?? AppTheme.glassBorder, width: 0.5),
                boxShadow: widget.shadows,
              ),
              child: widget.child,
            ),
          ),
        ),
      ),
    );
  }
}

// ============================================================
//  弹簧按钮 — 按下弹性缩放 + 触觉反馈 + 光晕呼吸
// ============================================================
class SpringButton extends StatefulWidget {
  final Widget child;
  final VoidCallback? onPressed;
  final LinearGradient? gradient;
  final double? width;
  final double height;
  final BorderRadius? borderRadius;
  final List<BoxShadow>? shadows;
  final bool haptic;

  const SpringButton({
    super.key, required this.child, this.onPressed, this.gradient,
    this.width, this.height = 52, this.borderRadius, this.shadows,
    this.haptic = true,
  });

  @override
  State<SpringButton> createState() => _SpringButtonState();
}

class _SpringButtonState extends State<SpringButton> with TickerProviderStateMixin {
  late AnimationController _scaleCtrl;
  late Animation<double> _scale;
  late AnimationController _glowCtrl;
  late Animation<double> _glow;

  @override
  void initState() {
    super.initState();
    _scaleCtrl = AnimationController(duration: AppTheme.durFast, vsync: this);
    _scale = Tween<double>(begin: 1.0, end: 0.93).animate(
      CurvedAnimation(parent: _scaleCtrl, curve: AppTheme.smoothOut));
    _glowCtrl = AnimationController(duration: AppTheme.durSlow, vsync: this);
    _glow = Tween<double>(begin: 0, end: 1).animate(
      CurvedAnimation(parent: _glowCtrl, curve: AppTheme.smoothInOut));
  }

  @override
  void dispose() {
    _scaleCtrl.dispose(); _glowCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTapDown: (_) {
        _scaleCtrl.forward();
        if (widget.haptic) HapticFeedback.lightImpact();
      },
      onTapUp: (_) {
        _scaleCtrl.reverse();
        _glowCtrl.forward();
        widget.onPressed?.call();
        Future.delayed(AppTheme.durSlow, () => _glowCtrl.reverse());
      },
      onTapCancel: () => _scaleCtrl.reverse(),
      child: AnimatedBuilder(
        animation: Listenable.merge([_scale, _glow]),
        builder: (ctx, child) {
          return Transform.scale(scale: _scale.value, child: child);
        },
        child: AnimatedBuilder(
          animation: _glow,
          builder: (ctx, child) {
            return Container(
              width: widget.width ?? double.infinity,
              height: widget.height,
              decoration: BoxDecoration(
                gradient: widget.gradient ?? AppTheme.primaryGradient,
                borderRadius: widget.borderRadius ?? BorderRadius.circular(AppTheme.radiusMd),
                boxShadow: widget.onPressed != null
                  ? [
                    ...widget.shadows ?? AppTheme.glowShadow,
                    BoxShadow(
                      color: AppTheme.primary.withValues(alpha: _glow.value * 0.2),
                      blurRadius: 40, spreadRadius: 2,
                    ),
                  ]
                  : null,
              ),
              child: Material(color: Colors.transparent, child: Center(child: widget.child)),
            );
          },
        ),
      ),
    );
  }
}

// ============================================================
//  骨架屏 — 渐变扫光效果
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
    _ctrl = AnimationController(duration: const Duration(milliseconds: 1400), vsync: this)..repeat();
    _anim = Tween<double>(begin: -1, end: 2).animate(CurvedAnimation(parent: _ctrl, curve: Curves.sineInOut));
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

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
                AppTheme.surfaceLight.withValues(alpha: 0.5),
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
//  淡入上滑动画 — 页面/列表项入场
// ============================================================
class FadeSlideIn extends StatefulWidget {
  final Widget child;
  final Duration delay;
  final double offset;
  const FadeSlideIn({super.key, required this.child, this.delay = Duration.zero, this.offset = 20});

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
    _slide = Tween<Offset>(begin: Offset(0, widget.offset * 0.01), end: Offset.zero)
        .animate(CurvedAnimation(parent: _ctrl, curve: AppTheme.smoothOut));
    Future.delayed(widget.delay, () => mounted ? _ctrl.forward() : null);
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return FadeTransition(
      opacity: _fade,
      child: SlideTransition(position: _slide, child: widget.child),
    );
  }
}
