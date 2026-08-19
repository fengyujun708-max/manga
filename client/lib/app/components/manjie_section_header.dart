import 'package:flutter/material.dart';
import '../theme/theme.dart';

class ManjieSectionHeader extends StatelessWidget {
  final String title;
  final VoidCallback? onSeeAll;
  const ManjieSectionHeader({super.key, required this.title, this.onSeeAll});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 24, 16, 12),
      child: Row(
        children: [
          Container(width: 4, height: 20,
            decoration: BoxDecoration(color: AppTheme.primary, borderRadius: BorderRadius.circular(2))),
          const SizedBox(width: 8),
          Text(title, style: Theme.of(context).textTheme.titleLarge),
          const Spacer(),
          if (onSeeAll != null)
            TextButton(onPressed: onSeeAll, child: Text('查看全部 →', style: TextStyle(color: AppTheme.primary))),
        ],
      ),
    );
  }
}
