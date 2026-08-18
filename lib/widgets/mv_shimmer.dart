import 'package:flutter/material.dart';
import 'package:mangaverse/config/theme/mangaverse_theme.dart';

class MVShimmer extends StatefulWidget {
  final Widget child;
  const MVShimmer({super.key, required this.child});
  @override
  State<MVShimmer> createState() => _MVShimmerState();
}

class _MVShimmerState extends State<MVShimmer> with SingleTickerProviderStateMixin {
  late AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(duration: Duration(milliseconds: 1200), vsync: this)..repeat();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, child) {
        return ShaderMask(
          shaderCallback: (bounds) {
            final t = _controller.value;
            return LinearGradient(
              begin: Alignment(-1 + 2 * t - 0.5, 0),
              end: Alignment(-1 + 2 * t + 0.5, 0),
              colors: [const Color(0xFF181818), const Color(0xFF2A2A3E), const Color(0xFF181818)],
            ).createShader(bounds);
          },
          child: child,
        );
      },
      child: widget.child,
    );
  }
}

class SkeletonCard extends StatelessWidget {
  final double width;
  final double height;
  const SkeletonCard({super.key, this.width = 120, this.height = 170});

  @override
  Widget build(BuildContext context) {
    return MVShimmer(
      child: Container(
        width: width,
        margin: EdgeInsets.only(right: 8),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(width: width, height: height,
              decoration: BoxDecoration(color: MangaVerseColors.surfaceVariant, borderRadius: BorderRadius.circular(8))),
            SizedBox(height: 6),
            Container(width: width * 0.7, height: 12,
              decoration: BoxDecoration(color: MangaVerseColors.surfaceVariant, borderRadius: BorderRadius.circular(4))),
            SizedBox(height: 4),
            Container(width: width * 0.4, height: 10,
              decoration: BoxDecoration(color: MangaVerseColors.surfaceVariant, borderRadius: BorderRadius.circular(4))),
          ],
        ),
      ),
    );
  }
}

class SkeletonRow extends StatelessWidget {
  final int itemCount;
  const SkeletonRow({super.key, this.itemCount = 5});
  @override
  Widget build(BuildContext context) {
    return Container(
      height: 200,
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        padding: EdgeInsets.symmetric(horizontal: 12),
        itemCount: itemCount,
        itemBuilder: (_, __) => SkeletonCard(),
      ),
    );
  }
}

class SkeletonHero extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MVShimmer(
      child: Container(
        height: 280, margin: EdgeInsets.only(bottom: 8),
        decoration: BoxDecoration(color: MangaVerseColors.surfaceVariant, borderRadius: BorderRadius.circular(12)),
      ),
    );
  }
}
