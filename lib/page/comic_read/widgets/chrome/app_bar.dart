import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:mangaverse/config/theme/mangaverse_theme.dart';
import 'package:mangaverse/page/comic_read/cubit/reader_cubit.dart';
import 'package:mangaverse/i18n/strings.g.dart';
import 'package:mangaverse/page/comments/widgets/title.dart';
import 'package:mangaverse/util/context/context_extensions.dart';

class ComicReadAppBar extends StatelessWidget {
  final String title;
  final ValueChanged<int> changePageIndex;
  final bool isDesktopFullscreen;
  final VoidCallback? onToggleFullscreen;

  const ComicReadAppBar({
    super.key,
    required this.title,
    required this.changePageIndex,
    this.isDesktopFullscreen = false,
    this.onToggleFullscreen,
  });

  @override
  Widget build(BuildContext context) {
    final isMenuVisible = context.select(
      (ReaderCubit cubit) => cubit.state.isMenuVisible,
    );
    const appBarRadius = 14.0;

    return Positioned(
      top: 0,
      left: 0,
      right: 0,
      child: IgnorePointer(
        ignoring: !isMenuVisible,
        child: AnimatedSlide(
          duration: const Duration(milliseconds: 320),
          curve: Curves.easeOutCubic,
          offset: isMenuVisible ? Offset.zero : const Offset(0, -1),
          child: ClipRRect(
            borderRadius: const BorderRadius.vertical(
              bottom: Radius.circular(appBarRadius),
            ),
            child: BackdropFilter(
              filter: ImageFilter.blur(sigmaX: 10.0, sigmaY: 10.0),
              child: AppBar(
                title: ScrollableTitle(text: title),
                titleSpacing: 6,
                actions: [
                  if (onToggleFullscreen != null)
                    IconButton(
                      tooltip: isDesktopFullscreen
                          ? t.reader.exitFullscreen
                          : t.reader.enterFullscreen,
                      onPressed: onToggleFullscreen,
                      icon: Icon(
                        isDesktopFullscreen
                            ? Icons.fullscreen_exit_rounded
                            : Icons.fullscreen_rounded,
                        color: MangaVerseColors.foreground,
                      ),
                    ),
                ],
                backgroundColor: MangaVerseColors.background.withValues(alpha: 0.85),
                surfaceTintColor: Colors.transparent,
                elevation: isMenuVisible ? 8.0 : 0.0,
                shadowColor: MangaVerseColors.accent.withValues(alpha: 0.15),
                shape: const RoundedRectangleBorder(
                  borderRadius: BorderRadius.vertical(
                    bottom: Radius.circular(appBarRadius),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
