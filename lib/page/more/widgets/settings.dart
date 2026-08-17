import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import 'package:mangaverse/config/router/router.gr.dart';
import 'package:mangaverse/config/theme/mangaverse_theme.dart';
import 'package:mangaverse/i18n/strings.g.dart';
import 'package:mangaverse/page/comic_follow/cubit/comic_follow_cubit.dart';

class SettingsWidget extends StatelessWidget {
  const SettingsWidget({super.key});

  @override
  Widget build(BuildContext context) {
    final commonItems = <_SettingsItem>[
      _SettingsItem(
        icon: Icons.download_outlined,
        title: t.more.downloadTasks,
        onTap: () => context.pushRoute(DownloadTaskRoute()),
      ),
      _SettingsItem(
        icon: Icons.sync_outlined,
        title: t.more.sync,
        onTap: () => context.pushRoute(SyncSettingRoute()),
      ),
      _SettingsItem(
        icon: Icons.notifications_active_outlined,
        title: t.more.comicFollow,
        badge: _BuildSettingsBadge(),
        onTap: () => context.pushRoute(ComicFollowRoute()),
      ),
      _SettingsItem(
        icon: Icons.settings_outlined,
        title: t.settings.globalTitle,
        onTap: () => context.pushRoute(GlobalSettingRoute()),
      ),
    ];

    final otherItems = <_SettingsItem>[
      _SettingsItem(
        icon: Icons.history,
        title: t.more.changelog,
        onTap: () => context.pushRoute(ChangelogRoute()),
      ),
      _SettingsItem(
        icon: Icons.info_outline,
        title: t.about.title,
        onTap: () => context.pushRoute(AboutRoute()),
      ),
    ];

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSectionTitle(context, t.more.common, Icons.widgets_outlined),
          _SettingsCard(items: commonItems),
          const SizedBox(height: 16),
          _buildSectionTitle(context, t.more.others, Icons.more_horiz),
          _SettingsCard(items: otherItems),
          const SizedBox(height: 24),
        ],
      ),
    );
  }

  Widget _buildSectionTitle(BuildContext context, String title, IconData icon) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(4, 12, 4, 10),
      child: Row(
        children: [
          Icon(icon, size: 16, color: MangaVerseColors.accent),
          const SizedBox(width: 8),
          Text(
            title,
            style: const TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: MangaVerseColors.accent,
              letterSpacing: 0.5,
            ),
          ),
        ],
      ),
    );
  }
}

class _SettingsCard extends StatelessWidget {
  const _SettingsCard({required this.items});

  final List<_SettingsItem> items;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: MangaVerseColors.surfaceVariant.withValues(alpha: 0.4),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: MangaVerseColors.border),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        children: List.generate(items.length, (index) {
          final item = items[index];
          final isLast = index == items.length - 1;
          return Column(
            children: [
              _SettingsListTile(item: item),
              if (!isLast)
                const Divider(
                  height: 1,
                  thickness: 0.3,
                  indent: 52,
                  endIndent: 12,
                  color: MangaVerseColors.border,
                ),
            ],
          );
        }),
      ),
    );
  }
}

class _SettingsListTile extends StatelessWidget {
  const _SettingsListTile({required this.item});

  final _SettingsItem item;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: item.onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
          child: Row(
            children: [
              Container(
                width: 34,
                height: 34,
                decoration: BoxDecoration(
                  color: MangaVerseColors.accent.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(
                  item.icon,
                  size: 18,
                  color: MangaVerseColors.accent,
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Text(
                  item.title,
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w500,
                    color: MangaVerseColors.foreground,
                  ),
                ),
              ),
              if (item.badge != null) item.badge!,
              const Icon(
                Icons.chevron_right,
                size: 20,
                color: MangaVerseColors.mutedForeground,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SettingsItem {
  const _SettingsItem({
    required this.icon,
    required this.title,
    this.onTap,
    this.badge,
  });

  final IconData icon;
  final String title;
  final VoidCallback? onTap;
  final Widget? badge;
}

class _BuildSettingsBadge extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return BlocSelector<ComicFollowCubit, ComicFollowState, int>(
      selector: (state) => state.updateCount,
      builder: (context, updateCount) {
        if (updateCount <= 0) return const SizedBox.shrink();
        return Container(
          margin: const EdgeInsets.only(right: 6),
          padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
          decoration: BoxDecoration(
            color: MangaVerseColors.accent,
            borderRadius: BorderRadius.circular(999),
          ),
          child: Text(
            updateCount >= 99 ? '99+' : '$updateCount',
            style: const TextStyle(
              fontSize: 11,
              color: Colors.white,
              fontWeight: FontWeight.w800,
            ),
          ),
        );
      },
    );
  }
}