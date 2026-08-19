import 'package:flutter/material.dart';
import '../theme/theme.dart';

class ManjieTextField extends StatefulWidget {
  final String label;
  final String? hint;
  final String? error;
  final IconData? prefixIcon;
  final IconData? suffixIcon;
  final bool obscureText;
  final bool readOnly;
  final TextInputType? keyboardType;
  final int? maxLength;
  final int? maxLines;
  final TextEditingController? controller;
  final ValueChanged<String>? onChanged;
  final VoidCallback? onSuffixTap;
  final String? Function(String?)? validator;

  const ManjieTextField({
    super.key,
    required this.label,
    this.hint,
    this.error,
    this.prefixIcon,
    this.suffixIcon,
    this.obscureText = false,
    this.readOnly = false,
    this.keyboardType,
    this.maxLength,
    this.maxLines = 1,
    this.controller,
    this.onChanged,
    this.onSuffixTap,
    this.validator,
  });

  @override
  State<ManjieTextField> createState() => _ManjieTextFieldState();
}

class _ManjieTextFieldState extends State<ManjieTextField> {
  bool _obscured = false;

  @override
  void initState() {
    super.initState();
    _obscured = widget.obscureText;
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        TextField(
          controller: widget.controller,
          obscureText: _obscured,
          readOnly: widget.readOnly,
          keyboardType: widget.keyboardType,
          maxLength: widget.maxLength,
          maxLines: widget.maxLines,
          onChanged: widget.onChanged,
          style: const TextStyle(color: AppTheme.textPrimary, fontSize: 16),
          decoration: InputDecoration(
            labelText: widget.label,
            hintText: widget.hint,
            errorText: widget.error,
            prefixIcon: widget.prefixIcon != null ? Icon(widget.prefixIcon, color: AppTheme.textSecondary) : null,
            suffixIcon: widget.suffixIcon != null
              ? IconButton(
                  icon: Icon(widget.suffixIcon, color: AppTheme.textSecondary),
                  onPressed: widget.onSuffixTap,
                )
              : widget.obscureText
                ? IconButton(
                    icon: Icon(_obscured ? Icons.visibility_off : Icons.visibility, color: AppTheme.textSecondary),
                    onPressed: () => setState(() => _obscured = !_obscured),
                  )
                : null,
            filled: true,
            fillColor: AppTheme.surface,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide(color: AppTheme.divider),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide(color: AppTheme.divider),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: const BorderSide(color: AppTheme.primary, width: 2),
            ),
            errorBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: const BorderSide(color: Colors.red),
            ),
            labelStyle: const TextStyle(color: AppTheme.textSecondary),
            hintStyle: TextStyle(color: AppTheme.textSecondary.withOpacity(0.5)),
          ),
        ),
        if (widget.validator != null)
          FormField<String>(
            validator: widget.validator,
            builder: (state) {
              if (state.hasError) {
                return Padding(
                  padding: const EdgeInsets.only(top: 4, left: 12),
                  child: Text(state.errorText!, style: const TextStyle(color: Colors.red, fontSize: 12)),
                );
              }
              return const SizedBox.shrink();
            },
          ),
      ],
    );
  }
}