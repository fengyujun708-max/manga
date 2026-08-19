import 'package:flutter/material.dart';

enum ReadingMode {
  webtoon,    // 瀑布流连续滚动
  singlePage, // 单页翻页
  dualPage,   // 双页模式
}

enum ReadingDirection {
  ltr, // 左到右
  rtl, // 右到左
  ttb, // 上到下
}

class ReaderSettings {
  final ReadingMode mode;
  final ReadingDirection direction;
  final double brightness;
  final double colorTemperature;
  final Color backgroundColor;
  final bool volumeButtons;
  final bool autoRead;
  final double autoReadInterval;
  final bool splitDualPage;

  const ReaderSettings({
    this.mode = ReadingMode.webtoon,
    this.direction = ReadingDirection.ltr,
    this.brightness = 1.0,
    this.colorTemperature = 0.5,
    this.backgroundColor = Colors.black,
    this.volumeButtons = false,
    this.autoRead = false,
    this.autoReadInterval = 3.0,
    this.splitDualPage = false,
  });

  ReaderSettings copyWith({
    ReadingMode? mode,
    ReadingDirection? direction,
    double? brightness,
    double? colorTemperature,
    Color? backgroundColor,
    bool? volumeButtons,
    bool? autoRead,
    double? autoReadInterval,
    bool? splitDualPage,
  }) {
    return ReaderSettings(
      mode: mode ?? this.mode,
      direction: direction ?? this.direction,
      brightness: brightness ?? this.brightness,
      colorTemperature: colorTemperature ?? this.colorTemperature,
      backgroundColor: backgroundColor ?? this.backgroundColor,
      volumeButtons: volumeButtons ?? this.volumeButtons,
      autoRead: autoRead ?? this.autoRead,
      autoReadInterval: autoReadInterval ?? this.autoReadInterval,
      splitDualPage: splitDualPage ?? this.splitDualPage,
    );
  }
}

class Chapter {
  final String id;
  final String title;
  final int pageCount;
  final List<String> pageUrls;
  final double chapterNumber;

  const Chapter({
    required this.id,
    required this.title,
    this.pageCount = 0,
    this.pageUrls = const [],
    this.chapterNumber = 0,
  });
}

class ReaderState {
  final Chapter? chapter;
  final int currentPage;
  final bool isLoading;
  final bool isError;
  final String? errorMessage;
  final double progress; // 0.0 - 1.0
  final ReaderSettings settings;

  const ReaderState({
    this.chapter,
    this.currentPage = 0,
    this.isLoading = true,
    this.isError = false,
    this.errorMessage,
    this.progress = 0.0,
    this.settings = const ReaderSettings(),
  });

  ReaderState copyWith({
    Chapter? chapter,
    int? currentPage,
    bool? isLoading,
    bool? isError,
    String? errorMessage,
    double? progress,
    ReaderSettings? settings,
  }) {
    return ReaderState(
      chapter: chapter ?? this.chapter,
      currentPage: currentPage ?? this.currentPage,
      isLoading: isLoading ?? this.isLoading,
      isError: isError ?? this.isError,
      errorMessage: errorMessage ?? this.errorMessage,
      progress: progress ?? this.progress,
      settings: settings ?? this.settings,
    );
  }
}


class ReaderTheme {
  static const bgColors = [
    Color(0xFF000000), // 纯黑
    Color(0xFF1A1A2E), // 深灰蓝
    Color(0xFFF5F0E8), // 米黄
    Color(0xFFE8E8E8), // 浅灰
    Color(0xFF2D2D2D), // 深灰
    Color(0xFF1B2838), // 暗蓝
  ];

  static const bgNames = ['纯黑', '深蓝', '米黄', '浅灰', '深灰', '暗蓝'];

  static const List<Color> gradientColors = [
    Color(0xFFFF6B6B),
    Color(0xFFFFA94D),
    Color(0xFFFFD93D),
    Color(0xFF6BCB77),
    Color(0xFF4D96FF),
    Color(0xFF9B59B6),
  ];
}