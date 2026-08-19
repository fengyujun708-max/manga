import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../../models/reader_models.dart';

class WebtoonMode extends StatefulWidget {
  final List<String> pageUrls;
  final int initialPage;
  final ValueChanged<int> onPageChanged;
  final VoidCallback onReachEnd;
  final ReaderSettings settings;

  const WebtoonMode({
    super.key,
    required this.pageUrls,
    required this.initialPage,
    required this.onPageChanged,
    required this.onReachEnd,
    required this.settings,
  });

  @override
  State<WebtoonMode> createState() => _WebtoonModeState();
}

class _WebtoonModeState extends State<WebtoonMode> {
  late ScrollController _scrollController;
  final Map<int, double> _pagePositions = {};
  int _currentPage = 0;
  bool _nearEnd = false;

  @override
  void initState() {
    super.initState();
    _currentPage = widget.initialPage;
    _scrollController = ScrollController();
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (!_scrollController.hasClients) return;

    final offset = _scrollController.offset;
    final maxScroll = _scrollController.position.maxScrollExtent;

    // 检测当前视口中间的页面
    final viewportCenter = offset + (_scrollController.position.viewportDimension / 2);
    int closestPage = 0;
    double closestDist = double.infinity;

    for (final entry in _pagePositions.entries) {
      final dist = (entry.value - viewportCenter).abs();
      if (dist < closestDist) {
        closestDist = dist;
        closestPage = entry.key;
      }
    }

    if (closestPage != _currentPage) {
      _currentPage = closestPage;
      widget.onPageChanged(closestPage);
    }

    // 检测是否接近末尾
    if (!_nearEnd && maxScroll - offset < 1500) {
      _nearEnd = true;
      widget.onReachEnd();
    } else if (maxScroll - offset >= 1500) {
      _nearEnd = false;
    }
  }

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      controller: _scrollController,
      itemCount: widget.pageUrls.length,
      cacheExtent: 2000,
      itemExtentBuilder: (_, __) => null,
      itemBuilder: (context, index) {
        return _PageImage(
          url: widget.pageUrls[index],
          index: index,
          onLayout: (size) {
            if (!_pagePositions.containsKey(index)) {
              WidgetsBinding.instance.addPostFrameCallback((_) {
                if (_scrollController.hasClients) {
                  final renderBox = context.findRenderObject() as RenderBox?;
                  if (renderBox != null) {
                    final position = renderBox.localToGlobal(Offset.zero).dy;
                    _pagePositions[index] = position;
                  }
                }
              });
            }
          },
        );
      },
    );
  }
}

class _PageImage extends StatefulWidget {
  final String url;
  final int index;
  final ValueChanged<Size> onLayout;

  const _PageImage({
    required this.url,
    required this.index,
    required this.onLayout,
  });

  @override
  State<_PageImage> createState() => _PageImageState();
}

class _PageImageState extends State<_PageImage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        final renderBox = context.findRenderObject() as RenderBox?;
        if (renderBox != null && renderBox.hasSize) {
          widget.onLayout(renderBox.size);
        }
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return CachedNetworkImage(
      imageUrl: widget.url,
      fit: BoxFit.contain,
      width: double.infinity,
      placeholder: (_, __) => Container(
        height: 300,
        color: Colors.grey.withOpacity(0.1),
        child: const Center(child: CircularProgressIndicator(strokeWidth: 2)),
      ),
      errorWidget: (_, __, ___) => Container(
        height: 200,
        color: Colors.red.withOpacity(0.1),
        child: const Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.broken_image, color: Colors.grey),
              SizedBox(height: 4),
              Text('加载失败', style: TextStyle(color: Colors.grey, fontSize: 12)),
            ],
          ),
        ),
      ),
    );
  }
}


class SinglePageMode extends StatefulWidget {
  final List<String> pageUrls;
  final int initialPage;
  final ValueChanged<int> onPageChanged;
  final VoidCallback onReachEnd;
  final ReaderSettings settings;

  const SinglePageMode({
    super.key,
    required this.pageUrls,
    required this.initialPage,
    required this.onPageChanged,
    required this.onReachEnd,
    required this.settings,
  });

  @override
  State<SinglePageMode> createState() => _SinglePageModeState();
}

class _SinglePageModeState extends State<SinglePageMode> {
  late PageController _pageController;
  int _currentPage = 0;

  @override
  void initState() {
    super.initState();
    _currentPage = widget.initialPage;
    _pageController = PageController(initialPage: widget.initialPage);
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  void _onPageChanged(int page) {
    setState(() => _currentPage = page);
    widget.onPageChanged(page);

    if (page >= widget.pageUrls.length - 3) {
      widget.onReachEnd();
    }
  }

  void goToPage(int page) {
    if (page >= 0 && page < widget.pageUrls.length) {
      _pageController.animateToPage(
        page,
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTapUp: (details) {
        final screenWidth = MediaQuery.of(context).size.width;
        final tapX = details.localPosition.dx;

        if (tapX < screenWidth * 0.33) {
          // 左区：上一页
          goToPage(_currentPage - 1);
        } else if (tapX > screenWidth * 0.66) {
          // 右区：下一页
          goToPage(_currentPage + 1);
        }
        // 中间区域：显示设置（由外部处理）
      },
      child: PageView.builder(
        controller: _pageController,
        onPageChanged: _onPageChanged,
        itemCount: widget.pageUrls.length,
        itemBuilder: (_, index) {
          return InteractiveViewer(
            minScale: 1.0,
            maxScale: 3.0,
            child: CachedNetworkImage(
              imageUrl: widget.pageUrls[index],
              fit: BoxFit.contain,
              width: double.infinity,
              height: double.infinity,
              placeholder: (_, __) => Container(
                color: Colors.grey.withOpacity(0.1),
                child: const Center(child: CircularProgressIndicator(strokeWidth: 2)),
              ),
              errorWidget: (_, __, ___) => Container(
                color: Colors.red.withOpacity(0.1),
                child: const Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.broken_image, size: 48, color: Colors.grey),
                      SizedBox(height: 8),
                      Text('图片加载失败', style: TextStyle(color: Colors.grey)),
                    ],
                  ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}