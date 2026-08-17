// 通用的标签/分类 Widget
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:mangaverse/config/theme/mangaverse_theme.dart';
import 'package:mangaverse/i18n/strings.g.dart';
import 'package:mangaverse/page/comic_info/json/normal/normal_comic_all_info.dart';
import 'package:mangaverse/page/comic_info/models/comic_info_action.dart';
import 'package:mangaverse/platform/desktop/window_logic.dart';
import 'package:mangaverse/type/pipe.dart';
import 'package:mangaverse/util/context/context_extensions.dart';
import 'package:mangaverse/util/text/chinese_convert.dart';
import 'package:mangaverse/widgets/toast.dart';

class AllChipWidget extends StatefulWidget {
  final String comicId;
  final ComicInfoMetadata metadata;
  final String from;

  const AllChipWidget({
    super.key,
    required this.comicId,
    required this.metadata,
    required this.from,
  });

  @override
  State<AllChipWidget> createState() => _AllChipWidgetState();
}

class _AllChipWidgetState extends State<AllChipWidget> {
  List<ComicInfoActionItem> get items => widget.metadata.value;
  String get title => widget.metadata.name;

  @override
  Widget build(BuildContext context) {
    final runSpacings = isDesktop ? 5.0 : 8.0;
    final processedTitle = processText(title).let(convertChineseForDisplay);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(height: runSpacings),
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _LabelChip(label: processedTitle),
            const SizedBox(width: 10),
            Expanded(
              child: Wrap(
                spacing: 10,
                runSpacing: runSpacings,
                children: items
                    .map(
                      (item) => _ClickableChip(
                        label: processText(
                          item.name,
                        ).let(convertChineseForDisplay),
                        onTap: () => _onTap(item),
                        onLongPress: () {
                          Clipboard.setData(
                            ClipboardData(text: processText(item.name)),
                          );
                          showSuccessToast(
                            t.comicInfo.copiedToClipboard(
                              name: item.name.let(convertChineseForDisplay),
                            ),
                          );
                        },
                      ),
                    )
                    .toList(),
              ),
            ),
          ],
        ),
      ],
    );
  }

  String processText(String text) {
    if (text.contains('\r')) {
      text = text.replaceAll('\r', '');
    }

    if (text.contains(' ')) {
      text = text.replaceAll(' ', '');
    }

    return text;
  }

  void _onTap(ComicInfoActionItem item) {
    if (item.onTap.isNotEmpty) {
      handleComicInfoAction(context, item.onTap, fallbackPluginId: widget.from);
    }
  }
}

class _LabelChip extends StatelessWidget {
  final String label;

  const _LabelChip({required this.label});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: MangaVerseColors.surfaceVariant.withValues(alpha: 0.4),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: MangaVerseColors.border),
      ),
      child: const Text(
        label,
        style: TextStyle(
          fontSize: 12,
          color: MangaVerseColors.mutedForeground,
          fontWeight: FontWeight.w500,
        ),
      ),
    );
  }
}

class _ClickableChip extends StatefulWidget {
  final String label;
  final VoidCallback onTap;
  final VoidCallback onLongPress;

  const _ClickableChip({
    required this.label,
    required this.onTap,
    required this.onLongPress,
  });

  @override
  State<_ClickableChip> createState() => _ClickableChipState();
}

class _ClickableChipState extends State<_ClickableChip> {
  bool _hovering = false;

  @override
  Widget build(BuildContext context) {
    return MouseRegion(
      cursor: SystemMouseCursors.click,
      onEnter: (_) => setState(() => _hovering = true),
      onExit: (_) => setState(() => _hovering = false),
      child: GestureDetector(
        onTap: widget.onTap,
        onLongPress: widget.onLongPress,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 140),
          curve: Curves.easeOut,
          decoration: BoxDecoration(
            color: _hovering
                ? MangaVerseColors.accent.withValues(alpha: 0.15)
                : MangaVerseColors.surfaceVariant.withValues(alpha: 0.4),
            borderRadius: BorderRadius.circular(10),
            border: Border.all(
              color: _hovering
                  ? MangaVerseColors.accent
                  : MangaVerseColors.border,
            ),
            boxShadow: _hovering
                ? [
                    BoxShadow(
                      color: MangaVerseColors.accent.withValues(alpha: 0.2),
                      blurRadius: 10,
                      offset: const Offset(0, 3),
                    ),
                  ]
                : [],
          ),
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          child: Text(
            widget.label,
            style: TextStyle(
              fontSize: 12,
              color: _hovering
                  ? MangaVerseColors.accent
                  : MangaVerseColors.foreground,
              fontWeight: FontWeight.w500,
            ),
          ),
        ),
      ),
    );
  }
}
                ),
                blurRadius: _hovering ? 10 : 6,
                offset: Offset(0, _hovering ? 3 : 2),
                spreadRadius: _hovering ? 0.5 : 0,
              ),
            ],
          ),
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          child: Text(
            widget.label,
            style: TextStyle(fontSize: 12, color: primary),
          ),
        ),
      ),
    );
  }
}
