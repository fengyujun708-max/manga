import 'package:flutter/material.dart';
import 'dart:async';
import 'package:flutter/services.dart';
import '../models/reader_models.dart';
import '../widgets/modes/reader_modes.dart';
import '../widgets/reader_settings.dart';

class ReaderPage extends StatefulWidget {
  final String comicId;
  final String comicTitle;
  final String? coverUrl;
  final List<Chapter> chapters;
  final int initialChapter;

  const ReaderPage({
    super.key,
    required this.comicId,
    required this.comicTitle,
    this.coverUrl,
    this.chapters = const [],
    this.initialChapter = 0,
  });

  @override
  State<ReaderPage> createState() => _ReaderPageState();
}

class _ReaderPageState extends State<ReaderPage> {
  late ReaderState _state;
  late int _currentChapterIndex;
  bool _showOverlay = true;
  Timer? _overlayTimer;

  // 模拟数据 - 实际应从源获取
  final Map<int, List<String>> _chapterPages = {};

  @override
  void initState() {
    super.initState();
    _currentChapterIndex = widget.initialChapter;
    _state = ReaderState(
      chapter: widget.chapters.isNotEmpty ? widget.chapters[_currentChapterIndex] : null,
      isLoading: true,
    );
    _loadChapter(_currentChapterIndex);
    _enterFullScreen();
  }

  @override
  void dispose() {
    _overlayTimer?.cancel();
    _exitFullScreen();
    super.dispose();
  }

  void _enterFullScreen() {
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
    SystemChrome.setPreferredOrientations([
      DeviceOrientation.landscapeLeft,
      DeviceOrientation.landscapeRight,
      DeviceOrientation.portraitUp,
    ]);
  }

  void _exitFullScreen() {
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
  }

  Future<void> _loadChapter(int index) async {
    if (index < 0 || index >= widget.chapters.length) return;

    setState(() => _state = _state.copyWith(isLoading: true, chapter: widget.chapters[index]));

    // 模拟加载页面（实际从源获取）
    final pages = List.generate(20, (i) => 'https://example.com/${widget.comicId}/ch${index}/p${i + 1}.jpg');
    _chapterPages[index] = pages;

    setState(() {
      _state = _state.copyWith(
        isLoading: false,
        currentPage: 0,
        progress: 0.0,
      );
    });
  }

  void _toggleOverlay() {
    setState(() {
      _showOverlay = !_showOverlay;
      if (_showOverlay) {
        _overlayTimer?.cancel();
        _overlayTimer = Timer(const Duration(seconds: 3), () {
          if (mounted) setState(() => _showOverlay = false);
        });
      }
    });
  }

  void _onPageChanged(int page) {
    final pages = _chapterPages[_currentChapterIndex] ?? [];
    setState(() {
      _state = _state.copyWith(
        currentPage: page,
        progress: pages.isNotEmpty ? (page + 1) / pages.length : 0.0,
      );
    });
  }

  void _onReachEnd() {
    if (_currentChapterIndex < widget.chapters.length - 1) {
      _currentChapterIndex++;
      _loadChapter(_currentChapterIndex);
    }
  }

  void _onChapterSelected(int index) {
    _currentChapterIndex = index;
    _loadChapter(index);
  }

  void _onSettingsChanged(ReaderSettings settings) {
    setState(() => _state = _state.copyWith(settings: settings));
  }

  void _showSettingsSheet() {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (_) => ReaderSettingsSheet(
        settings: _state.settings,
        onChanged: _onSettingsChanged,
        currentPage: _state.currentPage,
        totalPages: (_chapterPages[_currentChapterIndex] ?? []).length,
        chapterTitle: _state.chapter?.title ?? '',
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final pages = _chapterPages[_currentChapterIndex] ?? [];

    return Scaffold(
      backgroundColor: _state.settings.backgroundColor,
      body: GestureDetector(
        onTap: _toggleOverlay,
        child: Stack(
          children: [
            // 阅读内容
            if (_state.isLoading)
              const Center(child: CircularProgressIndicator(color: Colors.white))
            else if (pages.isEmpty)
              const Center(child: Text('暂无内容', style: TextStyle(color: Colors.white54)))
            else
              _buildReader(pages),

            // 覆盖层（顶部栏 + 底部进度条）
            if (_showOverlay)
              Positioned.fill(
                child: _buildOverlay(pages.length),
              ),

            // 中间点击区域提示（只在 overlay 隐藏时显示）
            if (!_showOverlay && _state.settings.mode == ReadingMode.singlePage)
              Positioned.fill(
                child: Row(
                  children: [
                    // 左区指示
                    if (_state.currentPage > 0)
                      const Expanded(
                        child: Center(
                          child: Icon(Icons.chevron_left, color: Colors.white12, size: 40),
                        ),
                      ),
                    // 中区（空 - 点击显示 overlay）
                    const Expanded(child: SizedBox()),
                    // 右区指示
                    if (_state.currentPage < pages.length - 1)
                      const Expanded(
                        child: Center(
                          child: Icon(Icons.chevron_right, color: Colors.white12, size: 40),
                        ),
                      ),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildReader(List<String> pages) {
    switch (_state.settings.mode) {
      case ReadingMode.webtoon:
        return WebtoonMode(
          pageUrls: pages,
          initialPage: _state.currentPage,
          onPageChanged: _onPageChanged,
          onReachEnd: _onReachEnd,
          settings: _state.settings,
        );
      case ReadingMode.singlePage:
        return SinglePageMode(
          pageUrls: pages,
          initialPage: _state.currentPage,
          onPageChanged: _onPageChanged,
          onReachEnd: _onReachEnd,
          settings: _state.settings,
        );
      case ReadingMode.dualPage:
        // 双页模式暂用单页代替
        return SinglePageMode(
          pageUrls: pages,
          initialPage: _state.currentPage,
          onPageChanged: _onPageChanged,
          onReachEnd: _onReachEnd,
          settings: _state.settings,
        );
    }
  }

  Widget _buildOverlay(int totalPages) {
    return Column(
      children: [
        // 顶部栏
        SafeArea(
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            color: Colors.black.withOpacity(0.7),
            child: Row(
              children: [
                IconButton(
                  icon: const Icon(Icons.arrow_back, color: Colors.white),
                  onPressed: () => Navigator.of(context).pop(),
                ),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(widget.comicTitle, style: const TextStyle(color: Colors.white70, fontSize: 12)),
                      Text(_state.chapter?.title ?? '', style: const TextStyle(color: Colors.white, fontSize: 14),
                        overflow: TextOverflow.ellipsis),
                    ],
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.settings_outlined, color: Colors.white),
                  onPressed: _showSettingsSheet,
                ),
                IconButton(
                  icon: const Icon(Icons.list_alt_outlined, color: Colors.white),
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
              Text('${_state.currentPage + 1}', style: const TextStyle(color: Colors.white, fontSize: 12)),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(2),
                    child: LinearProgressIndicator(
                      value: totalPages > 0 ? (_state.currentPage + 1) / totalPages : 0,
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
        // 章节导航
        if (_currentChapterIndex > 0 || _currentChapterIndex < widget.chapters.length - 1)
          Container(
            color: Colors.black.withOpacity(0.7),
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                if (_currentChapterIndex > 0)
                  TextButton.icon(
                    onPressed: () => _onChapterSelected(_currentChapterIndex - 1),
                    icon: const Icon(Icons.skip_previous, color: Colors.white, size: 18),
                    label: const Text('上一章', style: TextStyle(color: Colors.white, fontSize: 12)),
                  )
                else
                  const SizedBox.shrink(),
                if (_currentChapterIndex < widget.chapters.length - 1)
                  TextButton.icon(
                    onPressed: () => _onChapterSelected(_currentChapterIndex + 1),
                    icon: const Icon(Icons.skip_next, color: Colors.white, size: 18),
                    label: const Text('下一章', style: TextStyle(color: Colors.white, fontSize: 12)),
                  )
                else
                  const SizedBox.shrink(),
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
                itemCount: widget.chapters.length,
                itemBuilder: (_, i) {
                  final isCurrent = i == _currentChapterIndex;
                  return ListTile(
                    selected: isCurrent,
                    selectedTileColor: const Color(0xFF6C5CE7).withOpacity(0.2),
                    title: Text(
                      widget.chapters[i].title,
                      style: TextStyle(
                        color: isCurrent ? Colors.white : Colors.white70,
                        fontWeight: isCurrent ? FontWeight.w600 : FontWeight.normal,
                      ),
                    ),
                    subtitle: Text('${widget.chapters[i].pageCount}页',
                      style: const TextStyle(color: Colors.white38, fontSize: 11)),
                    trailing: isCurrent
                      ? const Icon(Icons.check, color: Color(0xFF6C5CE7), size: 18)
                      : null,
                    onTap: () {
                      Navigator.of(context).pop();
                      _onChapterSelected(i);
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

