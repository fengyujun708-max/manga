import 'package:flutter/material.dart';
import '../theme/theme.dart';

class ManjieComicCard extends StatelessWidget {
  final String title;
  final String? subtitle;
  final String? imageUrl;
  final String? badge;
  final double width;
  final double aspectRatio;
  final VoidCallback? onTap;

  const ManjieComicCard({
    super.key,
    required this.title,
    this.subtitle,
    this.imageUrl,
    this.badge,
    this.width = 140,
    this.aspectRatio = 0.7,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: SizedBox(
        width: width,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Stack(
                children: [
                  Container(
                    width: double.infinity,
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(12),
                      color: AppTheme.surfaceLight,
                      image: imageUrl != null
                        ? DecorationImage(image: NetworkImage(imageUrl!), fit: BoxFit.cover)
                        : null,
                    ),
                    child: imageUrl == null
                      ? Center(child: Icon(Icons.auto_stories, size: 40, color: AppTheme.textSecondary.withOpacity(0.3)))
                      : null,
                  ),
                  if (badge != null)
                    Positioned(top: 8, right: 8,
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(color: AppTheme.primary, borderRadius: BorderRadius.circular(6)),
                        child: Text(badge!, style: const TextStyle(color: Colors.white, fontSize: 10)),
                      ),
                    ),
                ],
              ),
            ),
            const SizedBox(height: 8),
            Text(title, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 14, fontWeight: FontWeight.w500),
              maxLines: 1, overflow: TextOverflow.ellipsis),
            if (subtitle != null)
              Text(subtitle!, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                maxLines: 1, overflow: TextOverflow.ellipsis),
          ],
        ),
      ),
    );
  }
}
