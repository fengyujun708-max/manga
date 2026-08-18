import 'package:flutter/material.dart';

/// Trending search terms with rank badges and animated entrance
class TrendingSearchWidget extends StatelessWidget {
  final List<Map<String, dynamic>> trends;
  final Function(String) onTap;

  const TrendingSearchWidget({
    super.key,
    required this.trends,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    if (trends.isEmpty) return SizedBox.shrink();
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: EdgeInsets.only(left: 16, right: 16, top: 20, bottom: 12),
          child: Row(
            children: [
              Container(
                width: 4,
                height: 16,
                decoration: BoxDecoration(
                  color: theme.colorScheme.primary,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              SizedBox(width: 8),
              Text(
                '热门搜索',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w700,
                  color: theme.colorScheme.onSurface,
                ),
              ),
              Spacer(),
              Text(
                '🔥 实时更新',
                style: TextStyle(
                  fontSize: 11,
                  color: theme.colorScheme.onSurface.withOpacity(0.4),
                ),
              ),
            ],
          ),
        ),
        Container(
          margin: EdgeInsets.symmetric(horizontal: 16),
          padding: EdgeInsets.symmetric(vertical: 4),
          decoration: BoxDecoration(
            color: theme.colorScheme.surfaceContainerHighest.withOpacity(0.4),
            borderRadius: BorderRadius.circular(16),
          ),
          child: Wrap(
            spacing: 8,
            runSpacing: 4,
            children: List.generate(trends.length, (i) {
              final trend = trends[i];
              final rank = i + 1;
              final keyword = (trend['keyword'] ?? trend['query'] ?? '').toString();
              final count = (trend['count'] ?? 0).toInt();
              return Padding(
                padding: EdgeInsets.all(4),
                child: _TrendChip(
                  rank: rank,
                  keyword: keyword,
                  count: count,
                  onTap: () => onTap(keyword),
                ),
              );
            }),
          ),
        ),
      ],
    );
  }
}

class _TrendChip extends StatelessWidget {
  final int rank;
  final String keyword;
  final int count;
  final VoidCallback onTap;

  const _TrendChip({
    required this.rank,
    required this.keyword,
    required this.count,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final color = switch (rank) {
      1 => Color(0xFFFF4D4F),
      2 => Color(0xFFFF7A45),
      3 => Color(0xFFFAAD14),
      _ => theme.colorScheme.onSurface.withOpacity(0.6),
    };
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(20),
      child: Container(
        padding: EdgeInsets.symmetric(horizontal: 10, vertical: 5),
        decoration: BoxDecoration(
          color: theme.colorScheme.surface.withOpacity(0.8),
          borderRadius: BorderRadius.circular(20),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 20,
              height: 20,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: color,
                borderRadius: BorderRadius.circular(6),
              ),
              child: Text(
                '$rank',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 11,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
            SizedBox(width: 6),
            Text(
              keyword,
              style: TextStyle(
                color: theme.colorScheme.onSurface,
                fontSize: 13,
              ),
            ),
            if (count > 0) ...[
              SizedBox(width: 6),
              Text(
                '${_formatCount(count)}',
                style: TextStyle(
                  color: theme.colorScheme.onSurface.withOpacity(0.4),
                  fontSize: 11,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  String _formatCount(int count) {
    if (count >= 10000) return '${(count / 10000).toStringAsFixed(1)}w';
    if (count >= 1000) return '${(count / 1000).toStringAsFixed(1)}k';
    return '$count';
  }
}
