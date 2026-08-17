import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:mangaverse/config/global/global_setting.dart';
import 'package:mangaverse/config/theme/mangaverse_theme.dart';
import 'package:mangaverse/i18n/strings.g.dart';
import 'package:mangaverse/page/search/method/on_search.dart';

class HistoryWidget extends StatefulWidget {
  const HistoryWidget({super.key, this.aggregateMode = true});

  final bool aggregateMode;

  @override
  State<HistoryWidget> createState() => _HistoryWidgetState();
}

class _HistoryWidgetState extends State<HistoryWidget> {
  bool _isNewestFirst = true;

  @override
  Widget build(BuildContext context) {
    final globalSettingState = context.watch<GlobalSettingCubit>().state;

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 8, 0),
          child: Row(
            children: [
              Text(
                t.search.history,
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: MangaVerseColors.foreground,
                ),
              ),
              const Spacer(),

              // 排序按钮
              if (globalSettingState.searchHistory.isNotEmpty) ...[
                _buildSortButton(),

                IconButton(
                  icon: const Icon(Icons.delete_outline, size: 20),
                  tooltip: t.search.clearHistory,
                  color: MangaVerseColors.mutedForeground,
                  onPressed: _resetHistory,
                ),
              ],
            ],
          ),
        ),

        // --- 3. 历史记录内容区域 ---
        Expanded(
          child: globalSettingState.searchHistory.isEmpty
              ? _buildEmpty()
              : _buildHistoryList(globalSettingState.searchHistory),
        ),
      ],
    );
  }

  Widget _buildSortButton() {
    return Tooltip(
      message: _isNewestFirst ? t.search.newestFirst : t.search.oldestFirst,
      child: TextButton.icon(
        style: TextButton.styleFrom(
          padding: const EdgeInsets.symmetric(horizontal: 8),
          minimumSize: const Size(0, 36),
        ),
        icon: Icon(
          _isNewestFirst ? Icons.history : Icons.history_toggle_off,
          size: 18,
          color: MangaVerseColors.accent,
        ),
        label: Text(
          _isNewestFirst ? t.search.descending : t.search.ascending,
          style: TextStyle(fontSize: 12, color: MangaVerseColors.accent),
        ),
        onPressed: () {
          setState(() {
            _isNewestFirst = !_isNewestFirst;
          });
        },
      ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.manage_search,
            size: 64,
            color: MangaVerseColors.surfaceVariant,
          ),
          const SizedBox(height: 16),
          Text(
            t.search.noHistory,
            style: TextStyle(color: MangaVerseColors.mutedForeground),
          ),
        ],
      ),
    );
  }

  Widget _buildHistoryList(List<String> historyList) {
    final sortedHistory = _isNewestFirst
        ? historyList
        : historyList.reversed.toList();
    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: SizedBox(
        width: double.infinity,
        child: Wrap(
          spacing: 12.0,
          runSpacing: 12.0,
          alignment: WrapAlignment.start,
          children: sortedHistory.map((historyItem) {
            final keyword = historyItem;

            return GestureDetector(
              onLongPress: () => _deleteSingle(historyItem),
              child: InputChip(
                label: Text(keyword),
                labelStyle: const TextStyle(
                  color: MangaVerseColors.foreground,
                  fontSize: 14,
                ),
                backgroundColor: MangaVerseColors.surfaceVariant.withValues(alpha: 0.5),
                side: BorderSide(
                  color: MangaVerseColors.border,
                  width: 0.5,
                ),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
                padding: const EdgeInsets.symmetric(horizontal: 2, vertical: 0),
                onPressed: () => onSearch(
                  context,
                  keyword,
                  aggregateMode: widget.aggregateMode,
                ),
                materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
              ),
            );
          }).toList(),
        ),
      ),
    );
  }

  void _deleteSingle(String historyItem) {
    final globalSettingCubit = context.read<GlobalSettingCubit>();
    final List<String> newHistory = List.from(
      globalSettingCubit.state.searchHistory,
    );
    newHistory.remove(historyItem);
    globalSettingCubit.updateState(
      (current) => current.copyWith(searchHistory: newHistory),
    );
  }

  void _resetHistory() {
    final globalSettingCubit = context.read<GlobalSettingCubit>();
    globalSettingCubit.resetState(
      (current, defaults) =>
          current.copyWith(searchHistory: defaults.searchHistory),
    );
  }
}
