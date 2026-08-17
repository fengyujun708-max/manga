import 'package:flutter/material.dart';
import 'package:mangaverse/config/theme/mangaverse_theme.dart';
import 'package:mangaverse/util/context/context_extensions.dart';

class SectionHeader extends StatelessWidget {
  const SectionHeader({
    super.key,
    required this.title,
    this.subtitle = '',
    this.onTap,
    this.margin = const EdgeInsets.all(5),
  });

  final String title;
  final String subtitle;
  final VoidCallback? onTap;
  final EdgeInsetsGeometry margin;

  @override
  Widget build(BuildContext context) {
    final canNavigate = onTap != null;

    return Padding(
      padding: margin,
      child: Container(
        decoration: BoxDecoration(
          color: MangaVerseColors.surfaceVariant.withValues(alpha: 0.3),
          borderRadius: BorderRadius.circular(8),
        ),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
        child: GestureDetector(
          onTap: canNavigate ? onTap : null,
          child: Row(
            children: [
              Expanded(
                child: Text(
                  title,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: MangaVerseColors.foreground,
                  ),
                ),
              ),
              if (subtitle.trim().isNotEmpty)
                Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: Text(
                    subtitle,
                    style: const TextStyle(
                      color: MangaVerseColors.mutedForeground,
                      fontSize: 12,
                    ),
                  ),
                ),
              if (canNavigate)
                const Icon(
                  Icons.arrow_forward_ios,
                  size: 16,
                  color: MangaVerseColors.mutedForeground,
                ),

              if (canNavigate) const SizedBox(width: 5),
            ],
          ),
        ),
      ),
    );
  }
}
