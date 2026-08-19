import 'package:flutter/material.dart';
import '../theme/theme.dart';

class ManjieShimmer extends StatelessWidget {
  final double? width;
  final double? height;
  final double borderRadius;
  const ManjieShimmer({super.key, this.width, this.height, this.borderRadius = 8});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: width, height: height,
      decoration: BoxDecoration(
        color: AppTheme.divider.withOpacity(0.3),
        borderRadius: BorderRadius.circular(borderRadius),
      ),
    );
  }
}

class ManjieShimmerList extends StatelessWidget {
  final int count; final double height;
  const ManjieShimmerList({super.key, this.count = 5, this.height = 60});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: List.generate(count, (_) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 6, horizontal: 16),
        child: Row(
          children: [
            const ManjieShimmer(width: 50, height: 50, borderRadius: 8),
            const SizedBox(width: 12),
            const Expanded(child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                ManjieShimmer(height: 14, width: double.infinity),
                SizedBox(height: 8),
                ManjieShimmer(height: 12, width: 100),
              ],
            )),
          ],
        ),
      )),
    );
  }
}

class ManjieGridShimmer extends StatelessWidget {
  final int crossAxisCount;
  const ManjieGridShimmer({super.key, this.crossAxisCount = 3});

  @override
  Widget build(BuildContext context) {
    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: crossAxisCount,
        childAspectRatio: 0.6,
        crossAxisSpacing: 8,
        mainAxisSpacing: 8,
      ),
      itemCount: crossAxisCount * 4,
      itemBuilder: (_, __) => Column(
        children: [
          Expanded(child: const ManjieShimmer(borderRadius: 8)),
          const SizedBox(height: 8),
          const ManjieShimmer(height: 12, width: 80),
        ],
      ),
    );
  }
}
