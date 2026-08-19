import 'package:flutter/material.dart';
import '../theme/theme.dart';

class ManjieToast {
  static void show(BuildContext context, String message, {bool isError = false}) {
    final overlay = Overlay.of(context);
    late OverlayEntry entry;
    entry = OverlayEntry(
      builder: (_) => Positioned(
        top: MediaQuery.of(context).padding.top + 60,
        left: 16, right: 16,
        child: Material(
          color: Colors.transparent,
          child: AnimatedOpacity(
            opacity: 1,
            duration: const Duration(milliseconds: 300),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              decoration: BoxDecoration(
                color: isError ? Colors.red.shade800 : AppTheme.surface,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(
                  color: isError ? Colors.red.shade600 : AppTheme.primary.withOpacity(0.3),
                ),
              ),
              child: Row(
                children: [
                  Icon(isError ? Icons.error_outline : Icons.check_circle_outline,
                    color: isError ? Colors.red.shade300 : AppTheme.accent, size: 20),
                  const SizedBox(width: 8),
                  Expanded(child: Text(message, style: const TextStyle(color: Colors.white, fontSize: 14))),
                ],
              ),
            ),
          ),
        ),
      ),
    );
    overlay.insert(entry);
    Future.delayed(const Duration(seconds: 2), () => entry.remove());
  }

  static void success(BuildContext context, String message) => show(context, message);
  static void error(BuildContext context, String message) => show(context, message, isError: true);
}
