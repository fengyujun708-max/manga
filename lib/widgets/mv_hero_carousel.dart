/// MangaVerse Hero 轮播 — Netflix 式主视觉
///
/// 特征：
/// - 全宽大幅封面 + 渐变遮罩
/// - 自动轮播 5 秒切换
/// - 底部圆点指示器
/// - 标题 + 简介 + CTA 按钮

import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:mangaverse/config/theme/mangaverse_theme.dart';

class MVHeroCarousel extends StatefulWidget {
  const MVHeroCarousel({
    super.key,
    required this.items,
    this.autoPlayDuration = const Duration(seconds: 5),
    this.height = 320,
  });

  final List<MVHeroData> items;
  final Duration autoPlayDuration;
  final double height;

  @override
  State<MVHeroCarousel> createState() => _MVHeroCarouselState();
}

class _MVHeroCarouselState extends State<MVHeroCarousel> {
  late final PageController _controller;
  int _currentIndex = 0;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _controller = PageController();
    _startAutoPlay();
  }

  @override
  void dispose() {
    _timer?.cancel();
    _controller.dispose();
    super.dispose();
  }

  void _startAutoPlay() {
    if (widget.items.length <= 1) return;
    _timer?.cancel();
    _timer = Timer.periodic(widget.autoPlayDuration, (_) {
      if (!mounted) return;
      final next = (_currentIndex + 1) % widget.items.length;
      _controller.animateToPage(
        next,
        duration: 800.ms,
        curve: Curves.easeOutExpo,
      );
    });
  }

  void _onPageChanged(int index) {
    setState(() => _currentIndex = index);
  }

  @override
  Widget build(BuildContext context) {
    if (widget.items.isEmpty) {
      return SizedBox(
        height: widget.height,
        child: const Center(
          child: CircularProgressIndicator(color: MangaVerseColors.accent),
        ),
      );
    }

    return SizedBox(
      height: widget.height,
      child: Stack(
        children: [
          // 轮播页面
          PageView.builder(
            controller: _controller,
            itemCount: widget.items.length,
            onPageChanged: _onPageChanged,
            physics: const BouncingScrollPhysics(),
            itemBuilder: (context, index) {
              final item = widget.items[index];
              return _HeroSlide(data: item, isActive: index == _currentIndex);
            },
          ),

          // 底部指示器
          Positioned(
            bottom: 12,
            left: 0,
            right: 0,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: List.generate(
                widget.items.length,
                (index) {
                  final isActive = index == _currentIndex;
                  return AnimatedContainer(
                    duration: 300.ms,
                    margin: const EdgeInsets.symmetric(horizontal: 3),
                    width: isActive ? 24 : 6,
                    height: 3,
                    decoration: BoxDecoration(
                      color: isActive
                          ? MangaVerseColors.accent
                          : Colors.white30,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  );
                },
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _HeroSlide extends StatelessWidget {
  const _HeroSlide({required this.data, required this.isActive});

  final MVHeroData data;
  final bool isActive;

  @override
  Widget build(BuildContext context) {
    return Stack(
      fit: StackFit.expand,
      children: [
        // 背景图
        Image.network(
          data.imageUrl,
          fit: BoxFit.cover,
          loadingBuilder: (_, child, __) => child,
          errorBuilder: (_, __, ___) => Container(
            decoration: const BoxDecoration(
              gradient: LinearGradient(
                colors: [MangaVerseColors.primary, MangaVerseColors.accent],
              ),
            ),
          ),
        ),

        // 渐变遮罩
        Positioned.fill(
          child: DecoratedBox(
            decoration: BoxDecoration(gradient: MangaVerseColors.heroGradient),
          ),
        ),

        // 文字内容
        Positioned(
          left: 20,
          right: 20,
          bottom: 40,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              if (data.badge != null)
                Container(
                  margin: const EdgeInsets.only(bottom: 8),
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: MangaVerseColors.accent,
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: Text(
                    data.badge!,
                    style: const TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w700,
                      color: Colors.white,
                      letterSpacing: 0.5,
                    ),
                  ),
                ).animate().fadeIn(duration: 400.ms, delay: 200.ms),
              Text(
                data.title,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: MangaVerseTypography.heroTitle,
              ).animate().fadeIn(duration: 400.ms, delay: 100.ms),
              if (data.description != null) ...[
                const SizedBox(height: 8),
                Text(
                  data.description!,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: MangaVerseTypography.mutedText
                      .copyWith(fontSize: 13),
                ).animate().fadeIn(duration: 400.ms, delay: 300.ms),
              ],
              const SizedBox(height: 16),
              Row(
                children: [
                  _CTAButton(
                    icon: Icons.play_arrow,
                    label: data.ctaLabel ?? '开始阅读',
                    onPressed: data.onTap,
                    primary: true,
                  ).animate().fadeIn(duration: 400.ms, delay: 400.ms),
                  if (data.onSecondaryTap != null) ...[
                    const SizedBox(width: 12),
                    _CTAButton(
                      icon: Icons.bookmark_outline,
                      label: data.secondaryCtaLabel ?? '收藏',
                      onPressed: data.onSecondaryTap,
                      primary: false,
                    ).animate().fadeIn(duration: 400.ms, delay: 500.ms),
                  ],
                ],
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _CTAButton extends StatelessWidget {
  const _CTAButton({
    required this.icon,
    required this.label,
    required this.onPressed,
    required this.primary,
  });

  final IconData icon;
  final String label;
  final VoidCallback? onPressed;
  final bool primary;

  @override
  Widget build(BuildContext context) {
    if (primary) {
      return GestureDetector(
        onTap: onPressed,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 10),
          decoration: BoxDecoration(
            gradient: MangaVerseColors.accentGradient,
            borderRadius: BorderRadius.circular(6),
            boxShadow: [
              BoxShadow(
                color: MangaVerseColors.accent.withValues(alpha: 0.4),
                blurRadius: 12,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon, size: 20, color: Colors.white),
              const SizedBox(width: 6),
              Text(
                label,
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w700,
                  color: Colors.white,
                ),
              ),
            ],
          ),
        ),
      );
    }

    return GestureDetector(
      onTap: onPressed,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
        decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: 0.12),
          borderRadius: BorderRadius.circular(6),
          border: Border.all(color: Colors.white24, width: 1),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 18, color: Colors.white),
            const SizedBox(width: 6),
            Text(
              label,
              style: const TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: Colors.white,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class MVHeroData {
  const MVHeroData({
    required this.imageUrl,
    required this.title,
    this.description,
    this.badge,
    this.ctaLabel,
    this.onTap,
    this.secondaryCtaLabel,
    this.onSecondaryTap,
  });

  final String imageUrl;
  final String title;
  final String? description;
  final String? badge;
  final String? ctaLabel;
  final VoidCallback? onTap;
  final String? secondaryCtaLabel;
  final VoidCallback? onSecondaryTap;
}
