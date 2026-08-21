import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../../../app/theme/theme.dart';

// ============================================================
//  漫画卡片 — 封面 + 标题 + 副标题 + 标签
//  支持 3 种尺寸：小(横滑用) / 中(网格用) / 大(Hero用)
// ============================================================
enum ComicCardSize { small, medium, large }

class ComicCard extends StatefulWidget {
  final String id;
  final String title;
  final String? subtitle;
  final String? cover;
  final String? tag;
  final ComicCardSize size;
  final VoidCallback? onTap;
  final int? index;

  const ComicCard({
    super.key,
    required this.id,
    required this.title,
    this.subtitle,
    this.cover,
    this.tag,
    this.size = ComicCardSize.medium,
    this.onTap,
    this.index,
  });

  @override
  State<ComicCard> createState() => _ComicCardState();
}

class _ComicCardState extends State<ComicCard> with SingleTickerProviderStateMixin {
  late AnimationController _ctrl;
  late Animation<double> _scale;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(duration: AppTheme.durFast, vsync: this);
    _scale = Tween<double>(begin: 1, end: 0.95).animate(CurvedAnimation(parent: _ctrl, curve: AppTheme.smoothOut));
  }

  @override
  void dispose() { _ctrl.dispose(); super.dispose(); }

  double get _cardW {
    switch (widget.size) {
      case ComicCardSize.small: return 130;
      case ComicCardSize.medium: return double.infinity;
      case ComicCardSize.large: return double.infinity;
    }
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTapDown: (_) { _ctrl.forward(); HapticFeedback.selectionClick(); },
      onTapUp: (_) { _ctrl.reverse(); widget.onTap?.call(); },
      onTapCancel: () => _ctrl.reverse(),
      child: AnimatedBuilder(
        animation: _scale,
        builder: (ctx, child) => Transform.scale(scale: _scale.value, child: child),
        child: Container(
          width: _cardW,
          clipBehavior: Clip.antiAlias,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(widget.size == ComicCardSize.small ? 14 : AppTheme.radiusMd),
            color: AppTheme.surface,
            boxShadow: AppTheme.softShadow,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              // 封面
              Stack(
                children: [
                  AspectRatio(
                    aspectRatio: 0.72,
                    child: widget.cover != null && widget.cover!.isNotEmpty
                        ? CachedNetworkImage(
                            imageUrl: widget.cover!,
                            fit: BoxFit.cover,
                            placeholder: (_, __) => ShimmerBox(width: double.infinity, height: 200),
                            errorWidget: (_, __, ___) => _coverPlaceholder(),
                          )
                        : _coverPlaceholder(),
                  ),
                  // 渐变遮罩底部
                  Positioned(
                    bottom: 0, left: 0, right: 0,
                    child: Container(
                      height: 60,
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          begin: Alignment.topCenter, end: Alignment.bottomCenter,
                          colors: [Colors.transparent, AppTheme.background.withValues(alpha: 0.9)],
                        ),
                      ),
                    ),
                  ),
                  // 标签
                  if (widget.tag != null && widget.tag!.isNotEmpty)
                    Positioned(
                      top: 8, left: 8,
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(
                          gradient: AppTheme.accentGradient,
                          borderRadius: BorderRadius.circular(6),
                          boxShadow: [BoxShadow(color: AppTheme.accent.withValues(alpha: 0.3), blurRadius: 8)],
                        ),
                        child: Text(widget.tag!, style: const TextStyle(fontSize: 10, fontWeight: FontWeight.w700, color: Colors.white)),
                      ),
                    ),
                ],
              ),
              // 文字
              Padding(
                padding: const EdgeInsets.fromLTRB(8, 8, 8, 10),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(widget.title, maxLines: 1, overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: AppTheme.textPrimary)),
                    if (widget.subtitle != null && widget.subtitle!.isNotEmpty)
                      Padding(
                        padding: const EdgeInsets.only(top: 2),
                        child: Text(widget.subtitle!, maxLines: 1, overflow: TextOverflow.ellipsis,
                            style: const TextStyle(fontSize: 11, color: AppTheme.textTertiary)),
                      ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _coverPlaceholder() {
    return Container(
      color: AppTheme.surfaceLight,
      child: Center(child: Icon(Icons.menu_book_rounded, size: 32, color: AppTheme.textTertiary.withValues(alpha: 0.4))),
    );
  }
}

// ============================================================
//  板块标题行 — 装饰竖线 + 标题 + 更多按钮
// ============================================================
class SectionHeader extends StatelessWidget {
  final String title;
  final String? subtitle;
  final VoidCallback? onMore;

  const SectionHeader({super.key, required this.title, this.subtitle, this.onMore});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: Row(
        children: [
          Container(width: 4, height: 18, decoration: BoxDecoration(gradient: AppTheme.primaryGradient, borderRadius: BorderRadius.circular(2))),
          const SizedBox(width: 8),
          Text(title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: AppTheme.textPrimary, letterSpacing: -0.3)),
          if (subtitle != null) ...[
            const SizedBox(width: 6),
            Text(subtitle!, style: const TextStyle(fontSize: 12, color: AppTheme.textTertiary)),
          ],
          const Spacer(),
          if (onMore != null)
            GestureDetector(
              onTap: onMore,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: AppTheme.primary.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Row(mainAxisSize: MainAxisSize.min, children: [
                  Text('更多', style: TextStyle(fontSize: 12, color: AppTheme.primary, fontWeight: FontWeight.w500)),
                  const SizedBox(width: 2),
                  Icon(Icons.chevron_right_rounded, size: 16, color: AppTheme.primary),
                ]),
              ),
            ),
        ],
      ),
    );
  }
}

// ============================================================
//  源类型标签
// ============================================================
class SourceBadge extends StatelessWidget {
  final String text;
  final SourceBadgeType type;
  final double size;
  const SourceBadge({super.key, required this.text, this.type = SourceBadgeType.neutral, this.size = 10});

  factory SourceBadge.fromMeta(Map<String, dynamic> meta) {
    final locale = meta['locale']?.toString() ?? '';
    final type = meta['type']?.toString() ?? '';
    String label;
    SourceBadgeType badgeType;
    if (type == 'hentai') { label = '成人'; badgeType = SourceBadgeType.danger; }
    else if (locale == 'zh') { label = '中文'; badgeType = SourceBadgeType.primary; }
    else if (locale == 'ja') { label = '日本'; badgeType = SourceBadgeType.accent; }
    else { label = '海外'; badgeType = SourceBadgeType.neutral; }
    return SourceBadge(text: label, type: badgeType);
  }

  @override
  Widget build(BuildContext context) {
    Color bg, fg;
    switch (type) {
      case SourceBadgeType.primary: bg = AppTheme.primary.withValues(alpha: 0.15); fg = AppTheme.primary;
      case SourceBadgeType.accent: bg = AppTheme.accent.withValues(alpha: 0.15); fg = AppTheme.accent;
      case SourceBadgeType.danger: bg = AppTheme.destructive.withValues(alpha: 0.15); fg = AppTheme.destructive;
      case SourceBadgeType.neutral: bg = AppTheme.textTertiary.withValues(alpha: 0.15); fg = AppTheme.textSecondary;
    }
    return Container(
      padding: EdgeInsets.symmetric(horizontal: size * 0.8, vertical: size * 0.3),
      decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(size * 0.6)),
      child: Text(text, style: TextStyle(fontSize: size, fontWeight: FontWeight.w600, color: fg)),
    );
  }
}

enum SourceBadgeType { primary, accent, danger, neutral }

// ============================================================
//  加载状态 / 空状态
// ============================================================
class LoadingState extends StatelessWidget {
  final String? text;
  const LoadingState({super.key, this.text});
  @override
  Widget build(BuildContext context) {
    return Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
      const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2, color: AppTheme.primary)),
      if (text != null) ...[const SizedBox(height: 12), Text(text!, style: const TextStyle(fontSize: 13, color: AppTheme.textTertiary))],
    ]));
  }
}

class EmptyState extends StatelessWidget {
  final IconData icon;
  final String title;
  final String? subtitle;
  final String? actionLabel;
  final VoidCallback? onAction;
  const EmptyState({super.key, required this.icon, required this.title, this.subtitle, this.actionLabel, this.onAction});

  @override
  Widget build(BuildContext context) {
    return Center(child: Padding(
      padding: const EdgeInsets.all(32),
      child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
        Container(width: 72, height: 72,
          decoration: BoxDecoration(shape: BoxShape.circle, color: AppTheme.primary.withValues(alpha: 0.08)),
          child: Icon(icon, size: 32, color: AppTheme.textTertiary)),
        const SizedBox(height: 20),
        Text(title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600, color: AppTheme.textSecondary)),
        if (subtitle != null) ...[const SizedBox(height: 8), Text(subtitle!, textAlign: TextAlign.center, style: const TextStyle(fontSize: 13, color: AppTheme.textTertiary))],
        if (actionLabel != null && onAction != null) ...[
          const SizedBox(height: 24),
          SpringButton(onPressed: onAction, height: 46, width: 200,
            child: Text(actionLabel!, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: Colors.white))),
        ],
      ]),
    ));
  }
}

// ============================================================
//  横滑漫画板块 — 标题 + 横向滚动卡片
// ============================================================
class HorizontalComicSection extends StatelessWidget {
  final String title;
  final List<Map<String, dynamic>> comics;
  final VoidCallback? onMore;
  final ValueChanged<String> onComicTap;
  const HorizontalComicSection({super.key, required this.title, required this.comics, this.onMore, required this.onComicTap});

  @override
  Widget build(BuildContext context) {
    if (comics.isEmpty) return const SizedBox.shrink();
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      SectionHeader(title: title, onMore: onMore),
      const SizedBox(height: 8),
      SizedBox(
        height: 230,
        child: ListView.separated(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          scrollDirection: Axis.horizontal,
          physics: const BouncingScrollPhysics(),
          itemCount: comics.length,
          separatorBuilder: (_, __) => const SizedBox(width: 12),
          itemBuilder: (ctx, i) {
            final c = comics[i];
            return SizedBox(width: 130, child: ComicCard(
              id: (c['id'] ?? '').toString(),
              title: (c['title'] ?? c['name'] ?? '').toString(),
              subtitle: (c['subtitle'] ?? c['subTitle'] ?? c['author'] ?? '').toString(),
              cover: (c['cover'] ?? c['coverUrl'] ?? '').toString(),
              tag: c['tag']?.toString(),
              size: ComicCardSize.small,
              onTap: () => onComicTap((c['id'] ?? '').toString()),
            ));
          },
        ),
      ),
    ]);
  }
}

// ============================================================
//  漫画网格（Sliver）
// ============================================================
class ComicGrid extends StatelessWidget {
  final List<Map<String, dynamic>> comics;
  final int crossAxisCount;
  final ValueChanged<String> onComicTap;
  final Widget? footer;
  const ComicGrid({super.key, required this.comics, this.crossAxisCount = 3, required this.onComicTap, this.footer});

  @override
  Widget build(BuildContext context) {
    return SliverPadding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 100),
      sliver: SliverGrid(
        gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: crossAxisCount, childAspectRatio: 0.62,
          crossAxisSpacing: 12, mainAxisSpacing: 14,
        ),
        delegate: SliverChildBuilderDelegate(
          (ctx, i) {
            if (i >= comics.length) return footer;
            final c = comics[i];
            return ComicCard(
              id: (c['id'] ?? '').toString(),
              title: (c['title'] ?? c['name'] ?? '').toString(),
              subtitle: (c['subtitle'] ?? c['subTitle'] ?? c['author'] ?? '').toString(),
              cover: (c['cover'] ?? c['coverUrl'] ?? '').toString(),
              size: ComicCardSize.medium,
              onTap: () => onComicTap((c['id'] ?? '').toString()),
            );
          },
          childCount: comics.length + (footer != null ? 1 : 0),
        ),
      ),
    );
  }
}