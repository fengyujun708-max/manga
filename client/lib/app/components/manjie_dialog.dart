import 'package:flutter/material.dart';
import '../theme/theme.dart';

enum ManjieDialogType { alert, confirm, input }

Future<T?> showManjieDialog<T>({
  required BuildContext context,
  required String title,
  String? content,
  ManjieDialogType type = ManjieDialogType.alert,
  String confirmText = '确定',
  String cancelText = '取消',
  String? hintText,
  ValueChanged<String>? onConfirm,
}) {
  String? inputValue;
  return showDialog<T>(
    context: context,
    builder: (ctx) => AlertDialog(
      backgroundColor: AppTheme.surface,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      title: Text(title, style: const TextStyle(color: AppTheme.textPrimary)),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (content != null)
            Text(content, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 14)),
          if (type == ManjieDialogType.input) ...[
            const SizedBox(height: 16),
            TextField(
              decoration: InputDecoration(
                hintText: hintText,
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
              ),
              onChanged: (v) => inputValue = v,
            ),
          ],
        ],
      ),
      actions: [
        if (type != ManjieDialogType.alert)
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: Text(cancelText, style: const TextStyle(color: AppTheme.textSecondary)),
          ),
        TextButton(
          onPressed: () {
            if (type == ManjieDialogType.input && onConfirm != null) onConfirm(inputValue ?? '');
            Navigator.of(ctx).pop(true);
          },
          child: Text(confirmText, style: const TextStyle(color: AppTheme.primary)),
        ),
      ],
    ),
  );
}
