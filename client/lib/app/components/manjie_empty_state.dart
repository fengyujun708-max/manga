import 'package:flutter/material.dart';
import '../theme/theme.dart';
import 'manjie_button.dart';

class ManjieEmptyState extends StatelessWidget {
  final IconData icon;
  final String title;
  final String? subtitle;
  final String? actionLabel;
  final VoidCallback? onAction;

  const ManjieEmptyState({
    super.key,
    required this.icon,
    required this.title,
    this.subtitle,
    this.actionLabel,
    this.onAction,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, size: 80, color: AppTheme.primary.withOpacity(0.4)),
            const SizedBox(height: 16),
            Text(title, style: Theme.of(context).textTheme.headlineMedium?.copyWith(color: AppTheme.textSecondary)),
            if (subtitle != null) ...[
              const SizedBox(height: 8),
              Text(subtitle!, style: Theme.of(context).textTheme.bodyMedium, textAlign: TextAlign.center),
            ],
            if (actionLabel != null && onAction != null) ...[
              const SizedBox(height: 24),
              ManjieButton(label: actionLabel!, onPressed: onAction),
            ],
          ],
        ),
      ),
    );
  }
}
