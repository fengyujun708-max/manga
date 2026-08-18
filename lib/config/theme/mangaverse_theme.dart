/// MangaVerse 主题系统 — 灵感来自 Netflix / OLED Dark Cinema
///
/// 设计原则：
/// 1. 纯黑背景 (#000000)，OLED 省电 + 沉浸感
/// 2. 玫红强调色 (#E11D48)，网飞式 CTA 视觉冲击
/// 3. 卡片用极深灰 (#0C0C0D) 浮于黑底，层次分明
/// 4. 大字号 + 粗标题 + 宽行距，影院级排版
/// 5. 动画 300-800ms，expo.inOut 缓动

import 'package:flutter/material.dart';

/// 动画时长常量
class MangaVerseAnimations {
  MangaVerseAnimations._();

  static const Duration fast = Duration(milliseconds: 200);
  static const Duration normal = Duration(milliseconds: 300);
  static const Duration slow = Duration(milliseconds: 500);
  static const Duration slower = Duration(milliseconds: 800);

  // Cubic easing curves
  static const Curve easeOutExpo = Cubic(0.16, 1, 0.3, 1);
  static const Curve easeInOutExpo = Cubic(0.87, 0, 0.13, 1);
  static const Curve easeOutBack = Cubic(0.34, 1.56, 0.64, 1);
}

class MangaVerseColors {
  MangaVerseColors._();

  // ── 核心色板 ──
  static const Color background = Color(0xFF000000); // 纯黑 OLED
  static const Color surface = Color(0xFF0C0C0D); // 卡片/面板
  static const Color surfaceVariant = Color(0xFF181818); // 次级面板
  static const Color primary = Color(0xFF0F0F23); // 深蓝黑
  static const Color accent = Color(0xFFE11D48); // 玫红 CTA
  static const Color accentHover = Color(0xFFFB7185); // 浅玫红
  static const Color foreground = Color(0xFFF8FAFC); // 主文字
  static const Color mutedForeground = Color(0xFF94A3B8); // 次文字
  static const Color border = Color(0xFF1A1A2E); // 分割线
  static const Color overlay = Color(0xE6000000); // 90% 黑遮罩

  // ── 功能色 ──
  static const Color success = Color(0xFF22C55E);
  static const colorWarning = Color(0xFFF59E0B);
  static const Color error = Color(0xFFEF4444);
  static const Color info = Color(0xFF3B82F6);

  // ── 渐变 ──
  /// 卡片底部渐变（文字可读性）
  static const Gradient cardGradient = LinearGradient(
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
    colors: [Colors.transparent, Color(0xCC000000)],
  );

  /// Hero 渐变遮罩
  static const Gradient heroGradient = LinearGradient(
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
    colors: [
      Color(0x33000000),
      Colors.transparent,
      Color(0x66000000),
      Color(0xE6000000),
    ],
    stops: [0.0, 0.3, 0.6, 1.0],
  );

  /// 强调按钮渐变
  static const Gradient accentGradient = LinearGradient(
    begin: Alignment.centerLeft,
    end: Alignment.centerRight,
    colors: [Color(0xFFE11D48), Color(0xFFBE123C)],
  );

  /// 侧边渐变（水平卡片）
  static const Gradient sideGradient = LinearGradient(
    begin: Alignment.centerLeft,
    end: Alignment.centerRight,
    colors: [Color(0xE6000000), Colors.transparent],
  );
}

/// Netflix 式排版
class MangaVerseTypography {
  MangaVerseTypography._();

  static const String? _fontFamily = null; // 使用系统字体

  static const TextStyle heroTitle = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 28,
    fontWeight: FontWeight.w800,
    color: MangaVerseColors.foreground,
    height: 1.2,
    letterSpacing: -0.5,
  );

  static const TextStyle sectionTitle = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 20,
    fontWeight: FontWeight.w700,
    color: MangaVerseColors.foreground,
    height: 1.3,
  );

  static const TextStyle cardTitle = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 14,
    fontWeight: FontWeight.w600,
    color: MangaVerseColors.foreground,
    height: 1.3,
  );

  static const TextStyle bodyText = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 14,
    fontWeight: FontWeight.w400,
    color: MangaVerseColors.foreground,
    height: 1.5,
  );

  static const TextStyle mutedText = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 12,
    fontWeight: FontWeight.w400,
    color: MangaVerseColors.mutedForeground,
    height: 1.4,
  );

  static const TextStyle caption = TextStyle(
    fontFamily: _fontFamily,
    fontSize: 11,
    fontWeight: FontWeight.w500,
    color: MangaVerseColors.mutedForeground,
    height: 1.3,
    letterSpacing: 0.3,
  );
}

/// 构建深色主题
ThemeData buildMangaVerseDarkTheme({Color? seedColor}) {
  final accent = seedColor ?? MangaVerseColors.accent;
  final colorScheme = ColorScheme.fromSeed(
    seedColor: accent,
    brightness: Brightness.dark,
  ).copyWith(
    surface: MangaVerseColors.background,
    primary: accent,
    secondary: MangaVerseColors.accentHover,
  );

  return ThemeData.dark().copyWith(
    scaffoldBackgroundColor: MangaVerseColors.background,
    colorScheme: colorScheme,
    appBarTheme: const AppBarTheme(
      backgroundColor: Colors.transparent,
      elevation: 0,
      centerTitle: false,
      titleTextStyle: MangaVerseTypography.sectionTitle,
      iconTheme: IconThemeData(color: MangaVerseColors.foreground),
    ),
    cardTheme: const CardThemeData(
      color: MangaVerseColors.surface,
      elevation: 0,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(8)),
      ),
    ),
    bottomNavigationBarTheme: const BottomNavigationBarThemeData(
      backgroundColor: Color(0xE6000000),
      selectedItemColor: MangaVerseColors.accent,
      unselectedItemColor: MangaVerseColors.mutedForeground,
      type: BottomNavigationBarType.fixed,
      elevation: 0,
    ),
    chipTheme: ChipThemeData(
      backgroundColor: MangaVerseColors.surfaceVariant,
      labelStyle: MangaVerseTypography.caption,
      side: BorderSide.none,
    ),
    dividerTheme: const DividerThemeData(
      color: MangaVerseColors.border,
      thickness: 0.5,
      space: 1,
    ),
    iconTheme: const IconThemeData(color: MangaVerseColors.foreground),
    textTheme: const TextTheme(
      displayLarge: MangaVerseTypography.heroTitle,
      headlineMedium: MangaVerseTypography.sectionTitle,
      titleMedium: MangaVerseTypography.cardTitle,
      bodyMedium: MangaVerseTypography.bodyText,
      bodySmall: MangaVerseTypography.mutedText,
      labelSmall: MangaVerseTypography.caption,
    ),
    splashColor: Colors.transparent,
    highlightColor: Colors.transparent,
    splashFactory: NoSplash.splashFactory,
    pageTransitionsTheme: PageTransitionsTheme(
      builders: {
        TargetPlatform.android: _FadeSlideTransitionBuilder(),
        TargetPlatform.iOS: _FadeSlideTransitionBuilder(),
      },
    ),
    listTileTheme: const ListTileThemeData(
      contentPadding: EdgeInsets.symmetric(horizontal: 16),
    ),
    snackBarTheme: SnackBarThemeData(
      behavior: SnackBarBehavior.floating,
      backgroundColor: MangaVerseColors.surfaceVariant,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
    ),
  );
}

/// 自定义页面过渡: 淡入+轻微上滑
class _FadeSlideTransitionBuilder extends PageTransitionsBuilder {
  @override
  Widget buildTransitions<T>(
    PageRoute<T> route,
    BuildContext context,
    Animation<double> animation,
    Animation<double> secondaryAnimation,
    Widget child,
  ) {
    return FadeTransition(
      opacity: CurvedAnimation(
        parent: animation,
        curve: MangaVerseAnimations.easeOutExpo,
      ),
      child: SlideTransition(
        position: Tween<Offset>(
          begin: const Offset(0, 0.03),
          end: Offset.zero,
        ).animate(CurvedAnimation(
          parent: animation,
          curve: MangaVerseAnimations.easeOutExpo,
        )),
        child: child,
      ),
    );
  }
}
