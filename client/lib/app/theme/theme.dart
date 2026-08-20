import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class AppTheme {
  // ===== 漫界沉浸暗黑配色（Theater/Cinema + Glassmorphism 融合）=====
  static const Color primary = Color(0xFF818CF8);       // 柔紫 — 品牌主色
  static const Color primaryDark = Color(0xFF6366F1);   // 深紫
  static const Color accent = Color(0xFFF59E0B);        // 琥珀金 — 聚光灯强调
  static const Color accentWarm = Color(0xFFFB923C);    // 暖橙 — CTA
  static const Color surface = Color(0xFF1B1B30);       // 卡片底色
  static const Color surfaceLight = Color(0xFF27273B);  // 次级面板
  static const Color cardColor = Color(0xFF1B1B30);     // 同 surface
  static const Color background = Color(0xFF0F0F23);    // 深空蓝黑 — 主背景
  static const Color backgroundDeep = Color(0xFF0A0A1A); // 更深的底
  static const Color textPrimary = Color(0xFFF8FAFC);    // 近白
  static const Color textSecondary = Color(0xFF94A3B8);  // 冷灰
  static const Color textTertiary = Color(0xFF64748B);   // 更淡
  static const Color divider = Color(0xFF312E81);        // 深紫边线
  static const Color glassBorder = Color(0x33FFFFFF);    // 玻璃边框 20%白
  static const Color glassFill = Color(0x1AFFFFFF);      // 玻璃填充 10%白
  static const Color success = Color(0xFF22C55E);
  static const Color destructive = Color(0xFFEF4444);
  static const Color ring = Color(0xFF818CF8);

  // 渐变色
  static const LinearGradient primaryGradient = LinearGradient(
    colors: [Color(0xFF818CF8), Color(0xFF6366F1)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );
  static const LinearGradient accentGradient = LinearGradient(
    colors: [Color(0xFFF59E0B), Color(0xFFFB923C)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );
  static const LinearGradient backgroundGradient = LinearGradient(
    colors: [Color(0xFF0F0F23), Color(0xFF1B1B30), Color(0xFF0F0F23)],
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
  );
  static const LinearGradient heroGradient = LinearGradient(
    colors: [Color(0x006366F1), Color(0xFF0F0F23)],
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
  );

  // 阴影
  static List<BoxShadow> get cardShadow => [
    BoxShadow(color: Color(0x40000000), blurRadius: 12, offset: Offset(0, 4)),
  ];
  static List<BoxShadow> get glowShadow => [
    BoxShadow(color: Color(0x33818CF8), blurRadius: 20, offset: Offset(0, 0)),
  ];

  // 圆角
  static const double radiusSm = 8;
  static const double radiusMd = 12;
  static const double radiusLg = 16;
  static const double radiusXl = 24;

  static ThemeData get dark => ThemeData(
    brightness: Brightness.dark,
    scaffoldBackgroundColor: background,
    primaryColor: primary,
    colorScheme: const ColorScheme.dark(
      primary: primary,
      secondary: accent,
      surface: surface,
      error: destructive,
    ),
    appBarTheme: const AppBarTheme(
      backgroundColor: Colors.transparent,
      elevation: 0,
      centerTitle: true,
      systemOverlayStyle: SystemUiOverlayStyle.light,
      titleTextStyle: TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: textPrimary),
    ),
    bottomNavigationBarTheme: const BottomNavigationBarThemeData(
      backgroundColor: Colors.transparent,
      selectedItemColor: primary,
      unselectedItemColor: textSecondary,
      type: BottomNavigationBarType.fixed,
      selectedLabelStyle: TextStyle(fontSize: 11, fontWeight: FontWeight.w600),
      unselectedLabelStyle: TextStyle(fontSize: 11),
    ),
    cardTheme: CardThemeData(
      color: surface,
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(radiusLg)),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: surfaceLight,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(radiusMd),
        borderSide: BorderSide.none,
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(radiusMd),
        borderSide: const BorderSide(color: primary, width: 1.5),
      ),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      labelStyle: const TextStyle(color: textSecondary),
      hintStyle: const TextStyle(color: textTertiary),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: primary,
        foregroundColor: Colors.white,
        elevation: 0,
        padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 24),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(radiusMd)),
        textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: primary,
        side: const BorderSide(color: primary, width: 1),
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
      backgroundColor: surfaceLight,
      contentTextStyle: const TextStyle(color: textPrimary),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(radiusSm)),
      behavior: SnackBarBehavior.floating,
    ),
  );

  static ThemeData get light => dark; // 漫界只用暗色
}

// 玻璃拟态容器组件
class GlassContainer extends StatelessWidget {
  final Widget child;
  final double blurRadius;
  final BorderRadius? borderRadius;
  final EdgeInsets? padding;
  final EdgeInsets? margin;
  final Color? borderColor;
  final List<BoxShadow>? shadows;

  const GlassContainer({
    super.key,
    required this.child,
    this.blurRadius = 15,
    this.borderRadius,
    this.padding,
    this.margin,
    this.borderColor,
    this.shadows,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: margin,
      padding: padding ?? const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppTheme.glassFill,
        borderRadius: borderRadius ?? BorderRadius.circular(AppTheme.radiusLg),
        border: Border.all(color: borderColor ?? AppTheme.glassBorder, width: 0.5),
        boxShadow: shadows ?? AppTheme.cardShadow,
      ),
      child: child,
    );
  }
}

// 发光按钮
class GlowButton extends StatelessWidget {
  final Widget child;
  final VoidCallback? onPressed;
  final LinearGradient? gradient;
  final double? width;
  final double height;

  const GlowButton({
    super.key,
    required this.child,
    this.onPressed,
    this.gradient,
    this.width,
    this.height = 50,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: width ?? double.infinity,
      height: height,
      decoration: BoxDecoration(
        gradient: gradient ?? AppTheme.primaryGradient,
        borderRadius: BorderRadius.circular(AppTheme.radiusMd),
        boxShadow: onPressed != null ? AppTheme.glowShadow : null,
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onPressed,
          borderRadius: BorderRadius.circular(AppTheme.radiusMd),
          child: Center(child: child),
        ),
      ),
    );
  }
}
