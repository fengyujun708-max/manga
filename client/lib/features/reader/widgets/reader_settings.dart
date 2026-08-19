import 'package:flutter/material.dart';
import '../models/reader_models.dart';

class ReaderSettingsSheet extends StatefulWidget {
  final ReaderSettings settings;
  final ValueChanged<ReaderSettings> onChanged;
  final int currentPage;
  final int totalPages;
  final String chapterTitle;

  const ReaderSettingsSheet({
    super.key,
    required this.settings,
    required this.onChanged,
    required this.currentPage,
    required this.totalPages,
    required this.chapterTitle,
  });

  @override
  State<ReaderSettingsSheet> createState() => _ReaderSettingsSheetState();
}

class _ReaderSettingsSheetState extends State<ReaderSettingsSheet> {
  late double _brightness;
  late double _colorTemp;
  late Color _bgColor;
  late ReadingMode _mode;
  late ReadingDirection _direction;

  @override
  void initState() {
    super.initState();
    _brightness = widget.settings.brightness;
    _colorTemp = widget.settings.colorTemperature;
    _bgColor = widget.settings.backgroundColor;
    _mode = widget.settings.mode;
    _direction = widget.settings.direction;
  }

  void _emit() {
    widget.onChanged(widget.settings.copyWith(
      brightness: _brightness,
      colorTemperature: _colorTemp,
      backgroundColor: _bgColor,
      mode: _mode,
      direction: _direction,
    ));
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: const BoxDecoration(
        color: Color(0xFF1A1A2E),
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 顶部拖拽条
          Center(
            child: Container(
              width: 40, height: 4,
              decoration: BoxDecoration(
                color: Colors.white24,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          const SizedBox(height: 16),

          // 标题 + 进度
          Text(widget.chapterTitle, style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.w600)),
          const SizedBox(height: 4),
          Text('${widget.currentPage + 1} / ${widget.totalPages}',
            style: const TextStyle(color: Colors.white54, fontSize: 13)),
          const SizedBox(height: 20),

          // 阅读模式
          _SectionTitle('阅读模式'),
          const SizedBox(height: 8),
          Row(children: [
            _ModeButton('瀑布流', ReadingMode.webtoon, Icons.view_stream),
            const SizedBox(width: 8),
            _ModeButton('单页', ReadingMode.singlePage, Icons.view_column),
            const SizedBox(width: 8),
            _ModeButton('双页', ReadingMode.dualPage, Icons.view_agenda),
          ]),
          const SizedBox(height: 20),

          // 亮度
          _SectionTitle('亮度'),
          _SliderRow(
            value: _brightness,
            icon: Icons.brightness_low,
            onChanged: (v) => setState(() { _brightness = v; _emit(); }),
          ),
          const SizedBox(height: 12),

          // 色温
          _SectionTitle('色温'),
          _SliderRow(
            value: _colorTemp,
            icon: Icons.wb_sunny,
            onChanged: (v) => setState(() { _colorTemp = v; _emit(); }),
          ),
          const SizedBox(height: 12),

          // 背景色
          _SectionTitle('背景'),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            children: List.generate(ReaderTheme.bgColors.length, (i) {
              final isSelected = _bgColor == ReaderTheme.bgColors[i];
              return GestureDetector(
                onTap: () => setState(() { _bgColor = ReaderTheme.bgColors[i]; _emit(); }),
                child: Container(
                  width: 36, height: 36,
                  decoration: BoxDecoration(
                    color: ReaderTheme.bgColors[i],
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(
                      color: isSelected ? Colors.white : Colors.white24,
                      width: isSelected ? 2 : 1,
                    ),
                  ),
                  child: isSelected
                    ? const Center(child: Icon(Icons.check, color: Colors.white, size: 18))
                    : null,
                ),
              );
            }),
          ),
          const SizedBox(height: 20),
        ],
      ),
    );
  }

  Widget _ModeButton(String label, ReadingMode mode, IconData icon) {
    final isSelected = _mode == mode;
    return Expanded(
      child: GestureDetector(
        onTap: () => setState(() { _mode = mode; _emit(); }),
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 12),
          decoration: BoxDecoration(
            color: isSelected ? const Color(0xFF6C5CE7) : Colors.white10,
            borderRadius: BorderRadius.circular(10),
          ),
          child: Column(
            children: [
              Icon(icon, color: isSelected ? Colors.white : Colors.white54, size: 22),
              const SizedBox(height: 4),
              Text(label, style: TextStyle(
                color: isSelected ? Colors.white : Colors.white54,
                fontSize: 12,
              )),
            ],
          ),
        ),
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  final String title;
  const _SectionTitle(this.title);

  @override
  Widget build(BuildContext context) {
    return Text(title, style: const TextStyle(color: Colors.white54, fontSize: 12, fontWeight: FontWeight.w500));
  }
}

class _SliderRow extends StatelessWidget {
  final double value;
  final IconData icon;
  final ValueChanged<double> onChanged;

  const _SliderRow({required this.value, required this.icon, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, color: Colors.white38, size: 18),
        Expanded(
          child: SliderTheme(
            data: SliderThemeData(
              activeTrackColor: const Color(0xFF6C5CE7),
              inactiveTrackColor: Colors.white12,
              thumbColor: Colors.white,
              overlayColor: const Color(0xFF6C5CE7).withOpacity(0.2),
            ),
            child: Slider(value: value, onChanged: onChanged),
          ),
        ),
      ],
    );
  }
}


class ReaderOverlay extends StatelessWidget {
  final int currentPage;
  final int totalPages;
  final String chapterTitle;
  final List<Chapter> chapters;
  final int currentChapterIndex;
  final ValueChanged<int> onChapterSelected;
  final VoidCallback onSettings;
  final VoidCallback onClose;
  final bool visible;

  const ReaderOverlay({
    super.key,
    required this.currentPage,
    required this.totalPages,
    required this.chapterTitle,
    required this.chapters,
    required this.currentChapterIndex,
    required this.onChapterSelected,
    required this.onSettings,
    required this.onClose,
    this.visible = true,
  });

  @override
  Widget build(BuildContext context) {
    if (!visible) return const SizedBox.shrink();

    return Column(
      children: [
        // 顶部栏
        SafeArea(
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            color: Colors.black.withOpacity(0.7),
            child: Row(
              children: [
                IconButton(
                  icon: const Icon(Icons.arrow_back, color: Colors.white),
                  onPressed: onClose,
                ),
                Expanded(
                  child: Text(
                    chapterTitle,
                    style: const TextStyle(color: Colors.white, fontSize: 14),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.settings, color: Colors.white),
                  onPressed: onSettings,
                ),
                IconButton(
                  icon: const Icon(Icons.list, color: Colors.white),
                  onPressed: () => _showChapterList(context),
                ),
              ],
            ),
          ),
        ),
        const Spacer(),
        // 底部进度条
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          color: Colors.black.withOpacity(0.7),
          child: Row(
            children: [
              Text('${currentPage + 1}', style: const TextStyle(color: Colors.white, fontSize: 12)),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(2),
                    child: LinearProgressIndicator(
                      value: (currentPage + 1) / totalPages,
                      backgroundColor: Colors.white12,
                      valueColor: const AlwaysStoppedAnimation<Color>(Color(0xFF6C5CE7)),
                      minHeight: 3,
                    ),
                  ),
                ),
              ),
              Text('$totalPages', style: const TextStyle(color: Colors.white54, fontSize: 12)),
            ],
          ),
        ),
      ],
    );
  }

  void _showChapterList(BuildContext context) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (_) => Container(
        height: MediaQuery.of(context).size.height * 0.6,
        decoration: const BoxDecoration(
          color: Color(0xFF1A1A2E),
          borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
        ),
        child: Column(
          children: [
            Container(
              margin: const EdgeInsets.only(top: 12),
              width: 40, height: 4,
              decoration: BoxDecoration(
                color: Colors.white24,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const Padding(
              padding: EdgeInsets.all(16),
              child: Text('章节列表', style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.w600)),
            ),
            const Divider(color: Colors.white12),
            Expanded(
              child: ListView.builder(
                itemCount: chapters.length,
                itemBuilder: (_, i) {
                  final isCurrent = i == currentChapterIndex;
                  return ListTile(
                    selected: isCurrent,
                    selectedTileColor: const Color(0xFF6C5CE7).withOpacity(0.2),
                    title: Text(
                      chapters[i].title,
                      style: TextStyle(
                        color: isCurrent ? Colors.white : Colors.white70,
                        fontWeight: isCurrent ? FontWeight.w600 : FontWeight.normal,
                      ),
                    ),
                    trailing: isCurrent
                      ? const Icon(Icons.check, color: Color(0xFF6C5CE7), size: 18)
                      : null,
                    onTap: () {
                      Navigator.of(context).pop();
                      onChapterSelected(i);
                    },
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}