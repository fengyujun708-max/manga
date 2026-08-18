import 'package:flutter/material.dart';
import 'package:mangaverse/config/theme/mangaverse_theme.dart';
import 'package:mangaverse/service/recommend_client.dart';

/// 推荐阅读行 - Netflix 风格
class RecommendedRow extends StatelessWidget {
  final List<RecommendItem> items;
  final String title;
  final String? subtitle;
  final Function(String mangaId, String source)? onTap;
  final bool isLoading;

  const RecommendedRow({
    super.key,
    required this.items,
    this.title = '为你推荐',
    this.subtitle,
    this.onTap,
    this.isLoading = false,
  });

  @override
  Widget build(BuildContext context) {
    if (isLoading) return _buildLoading(context);
    if (items.isEmpty) return SizedBox.shrink();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: EdgeInsets.only(left: 16, right: 16, top: 16, bottom: 8),
          child: Row(
            children: [
              Container(
                width: 3,
                height: 18,
                decoration: BoxDecoration(
                  color: MangaVerseColors.accent,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              SizedBox(width: 8),
              Text(
                title,
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: MangaVerseColors.foreground,
                ),
              ),
              Spacer(),
              if (subtitle != null)
                Text(
                  subtitle!,
                  style: TextStyle(
                    fontSize: 12,
                    color: MangaVerseColors.mutedForeground,
                  ),
                ),
            ],
          ),
        ),
        Container(
          height: 220,
          margin: EdgeInsets.only(left: 4),
          child: ListView.builder(
            scrollDirection: Axis.horizontal,
            padding: EdgeInsets.symmetric(horizontal: 12),
            itemCount: items.length,
            itemBuilder: (context, index) {
              final item = items[index];
              return _RecommendCard(
                item: item,
                rank: index < 3 ? index + 1 : null,
                onTap: onTap,
              );
            },
          ),
        ),
      ],
    );
  }

  Widget _buildLoading(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: EdgeInsets.only(left: 16, right: 16, top: 16, bottom: 8),
          child: Container(
            width: 100,
            height: 18,
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.1),
              borderRadius: BorderRadius.circular(4),
            ),
          ),
        ),
        Container(
          height: 220,
          child: ListView.builder(
            scrollDirection: Axis.horizontal,
            padding: EdgeInsets.symmetric(horizontal: 12),
            itemCount: 5,
            itemBuilder: (context, index) => _ShimmerCard(),
          ),
        ),
      ],
    );
  }
}

class _RecommendCard extends StatelessWidget {
  final RecommendItem item;
  final int? rank;
  final Function(String mangaId, String source)? onTap;

  const _RecommendCard({
    required this.item,
    this.rank,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => onTap?.call(item.mangaId, item.source),
      child: Container(
        width: 140,
        margin: EdgeInsets.only(right: 8),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Stack(
              children: [
                Container(
                  width: 140,
                  height: 190,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(8),
                    color: MangaVerseColors.surface,
                    image: item.coverUrl.isNotEmpty
                        ? DecorationImage(
                            image: NetworkImage(item.coverUrl),
                            fit: BoxFit.cover,
                          )
                        : null,
                  ),
                  child: item.coverUrl.isEmpty
                      ? Center(
                          child: Icon(
                            Icons.auto_stories,
                            color: MangaVerseColors.mutedForeground,
                            size: 32,
                          ),
                        )
                      : null,
                ),
                if (rank != null)
                  Positioned(
                    top: 4,
                    left: 4,
                    child: Container(
                      width: 24,
                      height: 24,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        color: rank == 1
                            ? Color(0xFFFF4D4F)
                            : rank == 2
                                ? Color(0xFFFF7A45)
                                : Color(0xFFFAAD14),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(
                        '$rank',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ),
                if (item.reason.isNotEmpty)
                  Positioned(
                    bottom: 4,
                    right: 4,
                    child: Container(
                      padding: EdgeInsets.symmetric(horizontal: 4, vertical: 2),
                      decoration: BoxDecoration(
                        color: MangaVerseColors.accent.withOpacity(0.8),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: Text(
                        item.reason == 'content'
                            ? '偏好'
                            : item.reason == 'collab'
                                ? '相似'
                                : '热门',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 9,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ),
              ],
            ),
            SizedBox(height: 6),
            Text(
              item.title,
              style: TextStyle(
                color: MangaVerseColors.foreground,
                fontSize: 13,
                fontWeight: FontWeight.w500,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }
}

class _ShimmerCard extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      width: 140,
      margin: EdgeInsets.only(right: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 140,
            height: 190,
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.05),
              borderRadius: BorderRadius.circular(8),
            ),
          ),
          SizedBox(height: 6),
          Container(
            width: 100,
            height: 12,
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.05),
              borderRadius: BorderRadius.circular(4),
            ),
          ),
        ],
      ),
    );
  }
}
