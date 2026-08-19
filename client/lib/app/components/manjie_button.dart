import 'package:flutter/material.dart';
import '../theme/theme.dart';

enum ManjieButtonVariant { filled, outlined, text, icon }

class ManjieButton extends StatelessWidget {
  final String label;
  final IconData? icon;
  final ManjieButtonVariant variant;
  final bool loading;
  final bool disabled;
  final double? width;
  final double height;
  final VoidCallback? onPressed;

  const ManjieButton({
    super.key,
    required this.label,
    this.icon,
    this.variant = ManjieButtonVariant.filled,
    this.loading = false,
    this.disabled = false,
    this.width,
    this.height = 50,
    this.onPressed,
  });

  @override
  Widget build(BuildContext context) {
    final isDisabled = disabled || loading;
    final effectiveOnPressed = isDisabled ? null : onPressed;

    return switch (variant) {
      ManjieButtonVariant.filled => _FilledButton(
        label: label, icon: icon, loading: loading,
        width: width, height: height, onPressed: effectiveOnPressed,
      ),
      ManjieButtonVariant.outlined => _OutlinedButton(
        label: label, icon: icon, loading: loading,
        width: width, height: height, onPressed: effectiveOnPressed,
      ),
      ManjieButtonVariant.text => _TextButton(
        label: label, icon: icon, loading: loading,
        onPressed: effectiveOnPressed,
      ),
      ManjieButtonVariant.icon => _IconOnlyButton(
        icon: icon ?? Icons.circle, onPressed: effectiveOnPressed,
      ),
    };
  }
}

class _FilledButton extends StatelessWidget {
  final String label; final IconData? icon; final bool loading;
  final double? width; final double height; final VoidCallback? onPressed;

  const _FilledButton({required this.label, this.icon, required this.loading, this.width, required this.height, this.onPressed});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: width ?? double.infinity,
      height: height,
      child: ElevatedButton(
        onPressed: onPressed,
        style: ElevatedButton.styleFrom(
          backgroundColor: AppTheme.primary,
          foregroundColor: Colors.white,
          disabledBackgroundColor: AppTheme.primary.withOpacity(0.4),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          elevation: 0,
        ),
        child: loading
          ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2.5, color: Colors.white))
          : Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                if (icon != null) ...[Icon(icon, size: 20), const SizedBox(width: 8)],
                Flexible(child: Text(label, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600), overflow: TextOverflow.ellipsis)),
              ],
            ),
      ),
    );
  }
}

class _OutlinedButton extends StatelessWidget {
  final String label; final IconData? icon; final bool loading;
  final double? width; final double height; final VoidCallback? onPressed;

  const _OutlinedButton({required this.label, this.icon, required this.loading, this.width, required this.height, this.onPressed});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: width ?? double.infinity,
      height: height,
      child: OutlinedButton(
        onPressed: onPressed,
        style: OutlinedButton.styleFrom(
          foregroundColor: AppTheme.primary,
          side: BorderSide(color: AppTheme.primary.withOpacity(0.5)),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
        child: loading
          ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2.5))
          : Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                if (icon != null) ...[Icon(icon, size: 20), const SizedBox(width: 8)],
                Flexible(child: Text(label, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600), overflow: TextOverflow.ellipsis)),
              ],
            ),
      ),
    );
  }
}

class _TextButton extends StatelessWidget {
  final String label; final IconData? icon; final bool loading; final VoidCallback? onPressed;

  const _TextButton({required this.label, this.icon, required this.loading, this.onPressed});

  @override
  Widget build(BuildContext context) {
    return TextButton(
      onPressed: onPressed,
      child: loading
        ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
        : Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (icon != null) ...[Icon(icon, size: 18), const SizedBox(width: 6)],
              Text(label),
            ],
          ),
    );
  }
}

class _IconOnlyButton extends StatelessWidget {
  final IconData icon; final VoidCallback? onPressed;

  const _IconOnlyButton({required this.icon, this.onPressed});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppTheme.surface,
        shape: BoxShape.circle,
      ),
      child: IconButton(icon: Icon(icon), onPressed: onPressed, color: AppTheme.textPrimary),
    );
  }
}