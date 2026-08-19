import 'package:flutter/material.dart';
import '../theme/theme.dart';
import 'manjie_button.dart';

class ManjieErrorState extends StatelessWidget {
  final String message;
  final VoidCallback? onRetry;
  const ManjieErrorState({super.key, required this.message, this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.error_outline, size: 64, color: Colors.red.withOpacity(0.6)),
            const SizedBox(height: 16),
            Text(message, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 16), textAlign: TextAlign.center),
            if (onRetry != null) ...[
              const SizedBox(height: 24),
              ManjieButton(label: '重试', icon: Icons.refresh, onPressed: onRetry, variant: ManjieButtonVariant.outlined),
            ],
          ],
        ),
      ),
    );
  }
}
