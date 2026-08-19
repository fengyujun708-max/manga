import 'package:flutter/material.dart';
import '../theme/theme.dart';

class ManjieAvatar extends StatelessWidget {
  final String? imageUrl;
  final String? name;
  final double size;
  final bool showBorder;
  const ManjieAvatar({super.key, this.imageUrl, this.name, this.size = 40, this.showBorder = false});

  @override
  Widget build(BuildContext context) {
    if (imageUrl != null) {
      return Container(
        width: size, height: size,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          image: DecorationImage(image: NetworkImage(imageUrl!), fit: BoxFit.cover),
          border: showBorder ? Border.all(color: AppTheme.primary, width: 2) : null,
        ),
      );
    }
    final initial = (name ?? '?').isNotEmpty ? (name![0]).toUpperCase() : '?';
    return Container(
      width: size, height: size,
      decoration: BoxDecoration(shape: BoxShape.circle, color: AppTheme.primary,
        border: showBorder ? Border.all(color: Colors.white, width: 2) : null),
      child: Center(child: Text(initial, style: TextStyle(color: Colors.white, fontSize: size * 0.4, fontWeight: FontWeight.bold))),
    );
  }
}
