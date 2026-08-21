import 'package:flutter/material.dart';
import '../theme/theme.dart';

class ManjieChip extends StatelessWidget {
  final String label;
  final bool selected;
  final bool removable;
  final Color? color;
  final VoidCallback? onTap;
  final VoidCallback? onRemove;

  const ManjieChip({
    super.key,
    required this.label,
    this.selected = false,
    this.removable = false,
    this.color,
    this.onTap,
    this.onRemove,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: selected ? (color ?? AppTheme.primary) : AppTheme.surface,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: selected ? Colors.transparent : Color(0xFF312E81)),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(label, style: TextStyle(
              color: selected ? Colors.white : AppTheme.textPrimary,
              fontSize: 13,
              fontWeight: selected ? FontWeight.w600 : FontWeight.normal,
            )),
            if (removable) ...[
              const SizedBox(width: 4),
              GestureDetector(
                onTap: onRemove,
                child: Icon(Icons.close, size: 14,
                  color: selected ? Colors.white70 : AppTheme.textSecondary),
              ),
            ],
          ],
        ),
      ),
    );
  }
}