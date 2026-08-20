import 'package:flutter/material.dart';
import 'dart:ui';
import 'package:flutter/services.dart';

class AppTheme {
  // ===== 漫界沉浸暗黑配色（Theater/Cinema + Liquid Glass 融合）=====
  static const Color primary = Color(0xFF818CF8);
  static const Color primaryDark = Color(0xFF6366F1);
  static const Color accent = Color(0xFFF59E0B);
  static const Color accentWarm = Color(0xFFFB923C);
  static const Color surface = Color(0xFF1B1B30);
  static const Color surfaceLight = Color(0xFF27273B);
  static const Color cardColor = Color(0xFF1B1B30);
  static const Color background = Color(0xFF0F0F23);
  static const Color backgroundDeep = Color(0xFF080814);
  static const Color textPrimary = Color(0xFFF8FAFC);
  static const Color textSecondary = Color(0xFF94A3B8);
  static const Color textTertiary = Color(0xFF64748B);
  static const Color divider = Color(0xFF312E81);
  static const Color glassBorder = Color(0x22FFFFFF);
  static const Color glassFill = Color(0x14FFFFFF);
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

  // 阴影
  static List<BoxShadow> get cardShadow => [
    BoxShadow(color: Colors.black.withValues(alpha: 0.35), blurRadius: 16, offset: const Offset(0, 6)),
  ];
  static List<BoxShadow> get glowShadow => [
    BoxShadow(color: AppTheme.primary.withValues(alpha: 0.35), blurRadius: 24, offset: const Offset(0, 0)),
  ];
  static List<BoxShadow> get accentGlow => [
    BoxShadow(color: AppTheme.accent.withValues(alpha: 0.3), blurRadius: 20, offset: const Offset(0, 0)),
  ];

  static const double radiusSm = 8;
  static const double radiusMd = 14;
  static const double radiusLg = 20;
  static const double radiusXl = 28;

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
      focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(radiusMd), borderSide: const BorderSide(color: primary, width: 1.5)),
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

/// 液态玻璃容器 — BackdropFilter + 渐变边框 + 光泽
class GlassContainer extends StatelessWidget {
  final Widget child;
  final double blur;
  final BorderRadius? radius;
  final EdgeInsets? padding;
  final EdgeInsets? margin;
  final Color? fillColor;

  const GlassContainer({
    super.key, required this.child,
    this.blur = 25, this.radius, this.padding, this.margin, this.fillColor,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: margin,
      decoration: BoxDecoration(
        borderRadius: radius ?? BorderRadius.circular(AppTheme.radiusLg),
      ),
      child: ClipRRect(
        borderRadius: radius ?? BorderRadius.circular(AppTheme.radiusLg),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: blur, sigmaY: blur),
          child: Container(
            padding: padding ?? const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: fillColor ?? AppTheme.glassFill,
              borderRadius: radius ?? BorderRadius.circular(AppTheme.radiusLg),
              border: Border.all(color: AppTheme.glassBorder, width: 0.5),
            ),
            child: child,
          ),
        ),
      ),
    );
  }
}

/// 发光渐变按钮 — 按下时缩放 + 光晕
class GlowButton extends StatefulWidget {
  final Widget child;
  final VoidCallback? onPressed;
  final LinearGradient? gradient;
  final double? width;
  final double height;

  const GlowButton({
    super.key, required this.child, this.onPressed, this.gradient, this.width, this.height = 52,
  });

  @override
  State<GlowButton> createState() => _GlowButtonState();
}

class _GlowButtonState extends State<GlowButton> with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _scale;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(duration: const Duration(milliseconds: 150), vsync: this);
    _scale = Tween<double>(begin: 1.0, end: 0.96).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTapDown: (_) => _controller.forward(),
      onTapUp: (_) { _controller.reverse(); widget.onPressed?.call(); },
      onTapCancel: () => _controller.reverse(),
      child: AnimatedBuilder(
        animation: _scale,
        builder: (context, child) {
          return Transform.scale(scale: _scale.value, child: child);
        },
        child: Container(
          width: widget.width ?? double.infinity,
          height: widget.height,
          decoration: BoxDecoration(
            gradient: widget.gradient ?? AppTheme.primaryGradient,
            borderRadius: BorderRadius.circular(AppTheme.radiusMd),
            boxShadow: widget.onPressed != null ? AppTheme.glowShadow : null,
          ),
          child: Material(
            color: Colors.transparent,
            child: Center(child: widget.child),
          ),
        ),
      ),
    );
  }
}
