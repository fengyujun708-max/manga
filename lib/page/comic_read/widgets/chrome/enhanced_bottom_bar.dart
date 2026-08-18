import 'package:flutter/material.dart';
import 'dart:math';

/// Enhanced bottom bar for reader with reading mode, brightness, prefetch status
class EnhancedReaderBottomBar extends StatelessWidget {
  final int currentPage;
  final int totalPages;
  final int currentChapter;
  final int totalChapters;
  final double progress;
  final String? currentMode; // 'single', 'double', 'webtoon', 'auto'
  final String? readDirection; // 'ltr', 'rtl', 'vertical'
  final bool isPrefetching;
  final int prefetchCount;
  final VoidCallback? onPrevChapter;
  final VoidCallback? onNextChapter;
  final VoidCallback? onChapterSelect;
  final Function(String)? onModeChanged;
  final Function(String)? onDirectionChanged;
  final ValueChanged<double>? onBrightnessChanged;
  final double brightness;

  const EnhancedReaderBottomBar({
    super.key,
    required this.currentPage,
    required this.totalPages,
    required this.currentChapter,
    required this.totalChapters,
    required this.progress,
    this.currentMode,
    this.readDirection,
    this.isPrefetching = false,
    this.prefetchCount = 0,
    this.onPrevChapter,
    this.onNextChapter,
    this.onChapterSelect,
    this.onModeChanged,
    this.onDirectionChanged,
    this.onBrightnessChanged,
    this.brightness = 0.7,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            Colors.black.withOpacity(0.0),
            Colors.black.withOpacity(0.95),
          ],
        ),
      ),
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).padding.bottom + 8,
        left: 16,
        right: 16,
        top: 32,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          _buildToolRow(theme),
          SizedBox(height: 12),
          _buildProgressBar(theme),
          SizedBox(height: 8),
          _buildChapterNav(theme),
        ],
      ),
    );
  }

  Widget _buildToolRow(ThemeData theme) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: [
        _buildIconButton(
          Icons.brightness_6,
          '亮度',
          theme,
          onTap: () => _showBrightnessSlider(theme),
        ),
        _buildModeButton(theme),
        _buildDirectionButton(theme),
        _buildPrefetchIndicator(theme),
        _buildIconButton(
          Icons.more_horiz,
          '更多',
          theme,
          onTap: () {},
        ),
      ],
    );
  }

  Widget _buildModeButton(ThemeData theme) {
    final mode = currentMode ?? 'single';
    final icon = switch (mode) {
      'single' => Icons.looks_one,
      'double' => Icons.looks_two,
      'webtoon' => Icons.view_stream,
      _ => Icons.looks_one,
    };
    final label = switch (mode) {
      'single' => '单页',
      'double' => '双页',
      'webtoon' => '条漫',
      _ => '单页',
    };
    return _buildIconButton(icon, label, theme, onTap: () {
      final next = switch (mode) {
        'single' => 'double',
        'double' => 'webtoon',
        'webtoon' => 'single',
        _ => 'single',
      };
      onModeChanged?.call(next);
    });
  }

  Widget _buildDirectionButton(ThemeData theme) {
    final dir = readDirection ?? 'ltr';
    final icon = switch (dir) {
      'ltr' => Icons.arrow_forward,
      'rtl' => Icons.arrow_back,
      'vertical' => Icons.arrow_downward,
      _ => Icons.arrow_forward,
    };
    final label = switch (dir) {
      'ltr' => 'LTR',
      'rtl' => 'RTL',
      'vertical' => '竖排',
      _ => 'LTR',
    };
    return _buildIconButton(icon, label, theme, onTap: () {
      final next = switch (dir) {
        'ltr' => 'rtl',
        'rtl' => 'vertical',
        'vertical' => 'ltr',
        _ => 'ltr',
      };
      onDirectionChanged?.call(next);
    });
  }

  Widget _buildPrefetchIndicator(ThemeData theme) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Stack(
          children: [
            Icon(
              Icons.download_outlined,
              color: theme.colorScheme.onSurface.withOpacity(0.6),
              size: 22,
            ),
            if (isPrefetching)
              Positioned(
                right: -2,
                top: -2,
                child: Container(
                  width: 8,
                  height: 8,
                  decoration: BoxDecoration(
                    color: theme.colorScheme.primary,
                    shape: BoxShape.circle,
                  ),
                  child: AnimatedOpacity(
                    opacity: 1.0,
                    duration: Duration(milliseconds: 600),
                    child: Container(),
                  ),
                ),
              ),
          ],
        ),
        SizedBox(height: 4),
        Text(
          isPrefetching ? '$prefetchCount' : '预载',
          style: TextStyle(
            fontSize: 10,
            color: theme.colorScheme.onSurface.withOpacity(0.5),
          ),
        ),
      ],
    );
  }

  Widget _buildIconButton(
    IconData icon,
    String label,
    ThemeData theme, {
    VoidCallback? onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            icon,
            color: theme.colorScheme.onSurface.withOpacity(0.7),
            size: 22,
          ),
          SizedBox(height: 4),
          Text(
            label,
            style: TextStyle(
              fontSize: 10,
              color: theme.colorScheme.onSurface.withOpacity(0.5),
            ),
          ),
        ],
      ),
    );
  }

  void _showBrightnessSlider(ThemeData theme) {
    // The brightness slider is shown as a dialog
    // This is handled by the parent widget
    onBrightnessChanged?.call(brightness);
  }

  Widget _buildProgressBar(ThemeData theme) {
    return Row(
      children: [
        Text(
          '$currentPage',
          style: TextStyle(
            color: theme.colorScheme.onSurface.withOpacity(0.6),
            fontSize: 12,
            fontFeatures: [FontFeature.tabularFigures()],
          ),
        ),
        Expanded(
          child: SliderTheme(
            data: SliderThemeData(
              trackHeight: 3,
              thumbShape: RoundSliderThumbShape(enabledThumbRadius: 7),
              overlayShape: RoundSliderOverlayShape(overlayRadius: 14),
              activeTrackColor: theme.colorScheme.primary,
              inactiveTrackColor: Colors.white.withOpacity(0.2),
              thumbColor: theme.colorScheme.primary,
            ),
            child: Slider(
              value: progress.clamp(0.0, 1.0),
              onChanged: (_) {},
            ),
          ),
        ),
        Text(
          '$totalPages',
          style: TextStyle(
            color: theme.colorScheme.onSurface.withOpacity(0.6),
            fontSize: 12,
            fontFeatures: [FontFeature.tabularFigures()],
          ),
        ),
      ],
    );
  }

  Widget _buildChapterNav(ThemeData theme) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        GestureDetector(
          onTap: onPrevChapter,
          behavior: HitTestBehavior.opaque,
          child: Container(
            padding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            decoration: BoxDecoration(
              border: Border.all(
                color: theme.colorScheme.onSurface.withOpacity(0.2),
              ),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Row(
              children: [
                Icon(Icons.skip_previous, size: 16, color: theme.colorScheme.onSurface.withOpacity(0.7)),
                SizedBox(width: 4),
                Text(
                  '上一章',
                  style: TextStyle(
                    fontSize: 12,
                    color: theme.colorScheme.onSurface.withOpacity(0.7),
                  ),
                ),
              ],
            ),
          ),
        ),
        GestureDetector(
          onTap: onChapterSelect,
          behavior: HitTestBehavior.opaque,
          child: Container(
            padding: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            decoration: BoxDecoration(
              color: theme.colorScheme.primary.withOpacity(0.15),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Text(
              '第 $currentChapter / $totalChapters 章',
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: theme.colorScheme.primary,
              ),
            ),
          ),
        ),
        GestureDetector(
          onTap: onNextChapter,
          behavior: HitTestBehavior.opaque,
          child: Container(
            padding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            decoration: BoxDecoration(
              border: Border.all(
                color: theme.colorScheme.onSurface.withOpacity(0.2),
              ),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Row(
              children: [
                Text(
                  '下一章',
                  style: TextStyle(
                    fontSize: 12,
                    color: theme.colorScheme.onSurface.withOpacity(0.7),
                  ),
                ),
                SizedBox(width: 4),
                Icon(Icons.skip_next, size: 16, color: theme.colorScheme.onSurface.withOpacity(0.7)),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
