import 'package:flutter/material.dart';

/// Enhanced reader top bar with back button, comic title, chapter, and options
class EnhancedReaderTopBar extends StatelessWidget {
  final String comicTitle;
  final String? chapterTitle;
  final int currentChapter;
  final int totalChapters;
  final bool isFollowing;
  final VoidCallback? onBack;
  final VoidCallback? onFavorite;
  final VoidCallback? onShare;
  final VoidCallback? onComment;
  final VoidCallback? onSettings;

  const EnhancedReaderTopBar({
    super.key,
    required this.comicTitle,
    this.chapterTitle,
    required this.currentChapter,
    required this.totalChapters,
    this.isFollowing = false,
    this.onBack,
    this.onFavorite,
    this.onShare,
    this.onComment,
    this.onSettings,
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
            Colors.black.withOpacity(0.95),
            Colors.black.withOpacity(0.0),
          ],
        ),
      ),
      padding: EdgeInsets.only(
        top: MediaQuery.of(context).padding.top + 8,
        bottom: 16,
        left: 8,
        right: 8,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Row(
            children: [
              _buildBackButton(theme),
              SizedBox(width: 8),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      comicTitle,
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    if (chapterTitle != null)
                      Text(
                        chapterTitle!,
                        style: TextStyle(
                          color: Colors.white.withOpacity(0.6),
                          fontSize: 12,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                  ],
                ),
              ),
              _buildActionButton(
                Icons.bookmark_outline,
                isFollowing ? theme.colorScheme.primary : Colors.white70,
                '收藏',
                onFavorite,
                theme,
              ),
              _buildActionButton(
                Icons.chat_outlined,
                Colors.white70,
                '评论',
                onComment,
                theme,
              ),
              _buildActionButton(
                Icons.ios_share,
                Colors.white70,
                '分享',
                onShare,
                theme,
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildBackButton(ThemeData theme) {
    return GestureDetector(
      onTap: onBack,
      behavior: HitTestBehavior.opaque,
      child: Container(
        padding: EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: Colors.white.withOpacity(0.1),
          borderRadius: BorderRadius.circular(10),
        ),
        child: Icon(
          Icons.arrow_back_ios_new,
          color: Colors.white,
          size: 18,
        ),
      ),
    );
  }

  Widget _buildActionButton(
    IconData icon,
    Color color,
    String tooltip,
    VoidCallback? onTap,
    ThemeData theme,
  ) {
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Container(
        padding: EdgeInsets.all(8),
        margin: EdgeInsets.only(left: 4),
        child: Icon(icon, color: color, size: 20),
      ),
    );
  }
}
