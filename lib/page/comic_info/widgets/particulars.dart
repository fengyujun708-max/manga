import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:path/path.dart' as p;
import 'package:mangaverse/config/theme/mangaverse_theme.dart';
import 'package:mangaverse/cubit/string_select.dart';
import 'package:mangaverse/i18n/strings.g.dart';
import 'package:mangaverse/page/comic_info/comic_info.dart';
import 'package:mangaverse/page/comic_info/json/normal/normal_comic_all_info.dart'
    show ComicInfo;
import 'package:mangaverse/plugin/plugin_registry_service.dart';
import 'package:mangaverse/src/rust/api/simple.dart';
import 'package:mangaverse/type/enum.dart';
import 'package:mangaverse/type/pipe.dart';
import 'package:mangaverse/util/context/context_extensions.dart';
import 'package:mangaverse/util/get_path.dart';
import 'package:mangaverse/util/text/chinese_convert.dart';
import 'package:mangaverse/widgets/picture_bloc/models/picture_info.dart';
import 'package:mangaverse/widgets/toast.dart';

class ComicParticularsWidget extends StatelessWidget {
  final ComicInfo comicInfo;
  final String from;
  final ComicEntryType type;
  final VoidCallback? onContinueRead;
  final VoidCallback? onCoverTap;

  const ComicParticularsWidget({
    super.key,
    required this.comicInfo,
    required this.from,
    required this.type,
    this.onContinueRead,
    this.onCoverTap,
  });

  @override
  Widget build(BuildContext context) {
    final stringSelectDate = context.watch<StringSelectCubit>().state;
    final pictureInfo = PictureInfo(
      from: from,
      url: comicInfo.cover.url,
      path: comicInfo.cover.path,
      chapterId: '',
      pictureType: PictureType.cover,
      cartoonId: comicInfo.id,
    );

    return SizedBox(
      width: double.infinity,
      child: LayoutBuilder(
        builder: (context, constraints) {
          final compact = constraints.maxWidth < 520;
          final info = _InfoColumn(
            comicInfo: comicInfo,
            from: from,
            type: type,
            stringSelectDate: stringSelectDate,
            onContinueRead: onContinueRead,
          );

          if (compact) {
            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Align(
                  child: Cover(
                    pictureInfo: pictureInfo,
                    height: 220,
                    borderRadius: 14,
                    onTap: onCoverTap,
                  ),
                ),
                const SizedBox(height: 16),
                info,
              ],
            );
          }

          return Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Cover(
                pictureInfo: pictureInfo,
                height: 230,
                borderRadius: 14,
                onTap: onCoverTap,
              ),
              const SizedBox(width: 16),
              Expanded(child: info),
            ],
          );
        },
      ),
    );
  }
}

class _InfoColumn extends StatefulWidget {
  const _InfoColumn({
    required this.comicInfo,
    required this.from,
    required this.type,
    required this.stringSelectDate,
    required this.onContinueRead,
  });

  final ComicInfo comicInfo;
  final String from;
  final ComicEntryType type;
  final String stringSelectDate;
  final VoidCallback? onContinueRead;

  @override
  State<_InfoColumn> createState() => _InfoColumnState();
}

class _InfoColumnState extends State<_InfoColumn> {
  int? _storageSize;

  @override
  void initState() {
    super.initState();
    if (widget.type == ComicEntryType.download) {
      _calculateStorageSize();
    }
  }

  Future<void> _calculateStorageSize() async {
    try {
      final downloadPath = await getDownloadPath();
      final storagePath = p.join(
        downloadPath,
        widget.from,
        'original',
        encodePath(path: widget.comicInfo.id),
      );
      final dir = Directory(storagePath);
      if (!await dir.exists()) return;

      int totalSize = 0;
      await for (final entity in dir.list(
        recursive: true,
        followLinks: false,
      )) {
        if (entity is File) {
          try {
            totalSize += await entity.length();
          } on FileSystemException {
            continue;
          }
        }
      }
      if (mounted) setState(() => _storageSize = totalSize);
    } catch (_) {
      // ignore errors
    }
  }

  String _formatFileSize(int bytes) {
    if (bytes < 1024) return '$bytes B';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)} KB';
    if (bytes < 1024 * 1024 * 1024) {
      return '${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB';
    }
    return '${(bytes / (1024 * 1024 * 1024)).toStringAsFixed(1)} GB';
  }

  @override
  Widget build(BuildContext context) {
    final displaySource = _resolvePluginDisplayName(widget.from);
    final titleStyle = const TextStyle(
      fontSize: 24,
      fontWeight: FontWeight.w800,
      height: 1.15,
      color: MangaVerseColors.foreground,
    );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
              decoration: BoxDecoration(
                color: MangaVerseColors.accent.withValues(alpha: 0.15),
                borderRadius: BorderRadius.circular(999),
                border: Border.all(
                  color: MangaVerseColors.accent.withValues(alpha: 0.3),
                ),
              ),
              child: Text(
                displaySource,
                style: const TextStyle(
                  fontSize: 12,
                  letterSpacing: 0.8,
                  fontWeight: FontWeight.w700,
                  color: MangaVerseColors.accent,
                ),
              ),
            ),
            if (_storageSize != null) ...[
              const SizedBox(width: 8),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 10,
                  vertical: 5,
                ),
                decoration: BoxDecoration(
                  color: MangaVerseColors.surfaceVariant.withValues(alpha: 0.5),
                  borderRadius: BorderRadius.circular(999),
                  border: Border.all(
                    color: MangaVerseColors.border,
                  ),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(
                      Icons.storage_rounded,
                      size: 14,
                      color: MangaVerseColors.mutedForeground,
                    ),
                    const SizedBox(width: 4),
                    Text(
                      _formatFileSize(_storageSize!),
                      style: const TextStyle(
                        fontSize: 12,
                        letterSpacing: 0.8,
                        fontWeight: FontWeight.w700,
                        color: MangaVerseColors.mutedForeground,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
        const SizedBox(height: 12),
        SelectableText(
          widget.comicInfo.title.let(convertChineseForDisplay),
          style: titleStyle,
        ),
        const SizedBox(height: 12),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: widget.comicInfo.titleMeta
              .map(
                (item) =>
                    _MetaPill(label: item.name.let(convertChineseForDisplay)),
              )
              .toList(),
        ),
        if (widget.stringSelectDate.isNotEmpty) ...[
          const SizedBox(height: 14),
          Material(
            color: Colors.transparent,
            child: InkWell(
              onTap: widget.onContinueRead,
              borderRadius: BorderRadius.circular(12),
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  horizontal: 14,
                  vertical: 12,
                ),
                decoration: BoxDecoration(
                  color: MangaVerseColors.accent.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: MangaVerseColors.accent.withValues(alpha: 0.3),
                  ),
                ),
                child: Row(
                  children: [
                    Container(
                      width: 34,
                      height: 34,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        color: MangaVerseColors.accent.withValues(alpha: 0.2),
                        borderRadius: BorderRadius.circular(9),
                      ),
                      child: const Icon(
                        Icons.history_rounded,
                        size: 18,
                        color: MangaVerseColors.accent,
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Text(
                            t.comicInfo.readHistory,
                            style: const TextStyle(
                              color: MangaVerseColors.accent,
                              fontWeight: FontWeight.w800,
                              fontSize: 12,
                              letterSpacing: 0.2,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            widget.stringSelectDate,
                            style: const TextStyle(
                              color: MangaVerseColors.foreground,
                              fontWeight: FontWeight.w700,
                              fontSize: 14,
                            ),
                          ),
                        ],
                      ),
                    ),
                    if (widget.onContinueRead != null) ...[
                      const SizedBox(width: 8),
                      Text(
                        t.comicInfo.continueRead,
                        style: const TextStyle(
                          color: MangaVerseColors.accent,
                          fontWeight: FontWeight.w800,
                          fontSize: 14,
                        ),
                      ),
                      const SizedBox(width: 4),
                      const Icon(
                        Icons.play_arrow_rounded,
                        size: 20,
                        color: MangaVerseColors.accent,
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ),
        ],
      ],
    );
  }

  String _resolvePluginDisplayName(String pluginIdRaw) {
    final pluginId = _normalizePluginId(pluginIdRaw);
    if (pluginId.isEmpty) {
      return pluginIdRaw.trim();
    }
    final info = PluginRegistryService.I.getCachedPluginInfo(pluginId);
    final name = info?['name']?.toString().trim() ?? '';
    return name.isNotEmpty ? name : pluginId;
  }

  String _normalizePluginId(String raw) {
    var value = raw.trim();
    while (value.length >= 2 && value.startsWith('(') && value.endsWith(')')) {
      value = value.substring(1, value.length - 1).trim();
    }
    return value;
  }
}

class _MetaPill extends StatelessWidget {
  const _MetaPill({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    final pill = Container(
      constraints: const BoxConstraints(minHeight: 38),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
      decoration: BoxDecoration(
        color: MangaVerseColors.surfaceVariant.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: MangaVerseColors.border,
        ),
      ),
      child: Text(
        label,
        style: const TextStyle(
          fontWeight: FontWeight.w600,
          fontSize: 12,
          height: 1.15,
          color: MangaVerseColors.mutedForeground,
        ),
      ),
    );

    return GestureDetector(
      onLongPress: () async {
        await Clipboard.setData(ClipboardData(text: label));
        showSuccessToast(t.comicInfo.copied(label: label));
      },
      child: pill,
    );
  }
}
