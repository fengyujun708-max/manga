import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:mangaverse/config/theme/mangaverse_theme.dart';
import 'package:mangaverse/page/more/more.dart';

@RoutePage()
class MorePage extends StatelessWidget {
  const MorePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: MangaVerseColors.background,
      body: SafeArea(
        child: Align(
          alignment: Alignment.topCenter,
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 768),
            child: ListView(
              children: [
                const SizedBox(height: 24),
                _buildHeader(context),
                const SettingsWidget(),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildHeader(BuildContext context) {
    return Column(
      children: [
        Container(
          padding: const EdgeInsets.all(3),
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [
                MangaVerseColors.accent.withValues(alpha: 0.7),
                MangaVerseColors.accent.withValues(alpha: 0.15),
              ],
            ),
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(24),
            child: Image.asset('asset/image/app-icon.png', width: 84, height: 84),
          ),
        ),
        const SizedBox(height: 14),
        const Text(
          'MangaVerse',
          style: TextStyle(
            fontSize: 26,
            fontWeight: FontWeight.w800,
            color: MangaVerseColors.foreground,
            letterSpacing: 0.5,
          ),
        ),
        const SizedBox(height: 4),
        const Text(
          '漫无止境',
          style: TextStyle(
            fontSize: 13,
            color: MangaVerseColors.mutedForeground,
            letterSpacing: 6,
          ),
        ),
        const SizedBox(height: 8),
      ],
    );
  }
}