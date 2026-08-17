/// MangaVerse 内容卡片 — Netflix 式设计
///
/// 特征：
/// - 圆角竖图 + 底部渐变遮罩
/// - 标题叠加在图片底部
/// - 长按预览 / 点击进入详情
/// - 加载骨架屏 shimmer

import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:mangaverse/config/theme/mangaverse_theme.dart';

/// Netflix 式漫画卡片
class MVCard extends StatelessWidget {
  const MVCard({
    super.key,
    required this.imageUrl,
    required this.title,
    this.subtitle,
    this.badge,
    this.onTap,
    this.width = 120,
    this.height = 180,
  });

  final String imageUrl;
  final String title;
  final String? subtitle;
  final String? badge;
  final VoidCallback? onTap;
  final double width;
  final double height;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: width,
        height: height,
        clipBehavior: Clip.antiAlias,
        decoration: BoxDecoration(
          color: MangaVerseColors.surface,
          borderRadius: BorderRadius.circular(8),
        ),
        child: Stack(
          fit: StackFit.expand,
          children: [
            // 封面图
            _buildImage(),

            // 底部渐变遮罩
            Positioned.fill(
              child: DecoratedBox(
                decoration: BoxDecoration(gradient: MangaVerseColors.cardGradient),
              ),
            ),

            // 徽标
            if (badge != null)
              Positioned(
                top: 6,
                left: 6,
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                  decoration: BoxDecoration(
                    color: MangaVerseColors.accent,
                    borderRadius: BorderRadius.circular(3),
                  ),
                  child: Text(
                    badge!,
                    style: const TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.w700,
                      color: Colors.white,
                    ),
                  ),
                ),
              ),

            // 底部文字
            Positioned(
              left: 8,
              right: 8,
              bottom: 8,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    title,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: MangaVerseTypography.cardTitle.copyWith(fontSize: 12),
                  ),
                  if (subtitle != null) ...[
                    const SizedBox(height: 2),
                    Text(
                      subtitle!,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: MangaVerseTypography.caption,
                    ),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    ).animate(
      onPlay: (c) => c.forward(),
      target: onTap != null ? 1.0 : 0.0,
    ).scale(
      begin: const Offset(1.0, 1.0),
      end: const Offset(0.97, 0.97),
      duration: 150.ms,
    );
  }

  Widget _buildImage() {
    if (imageUrl.isEmpty) {
      return _SkeletonBox(width: width, height: height);
    }
    return Image.network(
      imageUrl,
      fit: BoxFit.cover,
      loadingBuilder: (context, child, progress) {
        if (progress == null) return child;
        return _SkeletonBox(width: width, height: height);
      },
      errorBuilder: (context, error, stack) {
        return const ColoredBox(
          color: MangaVerseColors.surfaceVariant,
          child: Center(child: Icon(Icons.broken_image, color: Colors.white24)),
        );
      },
    );
  }
}

/// 横向滚动内容行 — Netflix 式 "Row"
class MVContentRow extends StatelessWidget {
  const MVContentRow({
    super.key,
    required this.title,
    required this.items,
    this.onSeeAll,
    this.cardWidth = 120,
    this.cardHeight = 180,
  });

  final String title;
  final List<MVCardData> items;
  final VoidCallback? onSeeAll;
  final double cardWidth;
  final double cardHeight;

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) return const SizedBox.shrink();

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 行标题
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    title,
                    style: MangaVerseTypography.sectionTitle,
                  ),
                ),
                if (onSeeAll != null)
                  GestureDetector(
                    onTap: onSeeAll,
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          '查看全部',
                          style: MangaVerseTypography.caption.copyWith(
                            color: MangaVerseColors.accent,
                          ),
                        ),
                        const Icon(Icons.chevron_right,
                            size: 16, color: MangaVerseColors.accent),
                      ],
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 8),
          // 横向滚动列表
          SizedBox(
            height: cardHeight + 8,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 16),
              itemCount: items.length,
              separatorBuilder: (_, __) => const SizedBox(width: 8),
              itemBuilder: (context, index) {
                final item = items[index];
                return MVCard(
                  imageUrl: item.imageUrl,
                  title: item.title,
                  subtitle: item.subtitle,
                  badge: item.badge,
                  onTap: item.onTap,
                  width: cardWidth,
                  height: cardHeight,
                ).animate(delay: (index * 40).ms).fadeIn(duration: 300.ms).slideX(
                      begin: 0.1,
                      duration: 300.ms,
                    );
              },
            ),
          ),
        ],
      ),
    );
  }
}

/// 卡片数据模型
class MVCardData {
  const MVCardData({
    required this.imageUrl,
    required this.title,
    this.subtitle,
    this.badge,
    this.onTap,
  });

  final String imageUrl;
  final String title;
  final String? subtitle;
  final String? badge;
  final VoidCallback? onTap;
}

/// 骨架屏
class _SkeletonBox extends StatelessWidget {
  const _SkeletonBox({required this.width, required this.height});

  final double width;
  final double height;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: width,
      height: height,
      color: MangaVerseColors.surfaceVariant,
      child: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              MangaVerseColors.surfaceVariant,
              MangaVerseColors.surface,
              MangaVerseColors.surfaceVariant,
            ],
          ),
        ),
      ).animate(onPlay: (c) => c.repeat()).shimmer(
            duration: 1200.ms,
            color: Colors.white10,
          ),
    );
  }
}
