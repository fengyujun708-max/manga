import 'package:flutter/material.dart';
import '../theme/theme.dart';

class ManjieHeroBanner extends StatelessWidget {
  final String title;
  final String? subtitle;
  final String? badge;
  final List<String>? tags;
  final VoidCallback? onRead;
  final VoidCallback? onFavorite;

  const ManjieHeroBanner({
    super.key, required this.title, this.subtitle, this.badge, this.tags,
    this.onRead, this.onFavorite,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 400, margin: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        gradient: const LinearGradient(
          begin: Alignment.topCenter, end: Alignment.bottomCenter,
          colors: [Color(0xFF6C5CE7), Color(0xFF0F3460)],
        ),
      ),
      child: Stack(
        children: [
          Positioned(right: -40, top: -40,
            child: Container(width: 200, height: 200,
              decoration: BoxDecoration(shape: BoxShape.circle, color: Colors.white.withOpacity(0.05)))),
          Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start, mainAxisAlignment: MainAxisAlignment.end,
              children: [
                if (badge != null)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(color: Colors.white.withOpacity(0.2), borderRadius: BorderRadius.circular(20)),
                    child: Text(badge!, style: const TextStyle(fontSize: 12, color: Colors.white))),
                const SizedBox(height: 12),
                Text(title, style: Theme.of(context).textTheme.headlineLarge?.copyWith(
                  fontWeight: FontWeight.bold, shadows: [Shadow(blurRadius: 10, color: Colors.black.withOpacity(0.5))])),
                if (tags != null) ...[
                  const SizedBox(height: 8),
                  Row(children: tags!.map((t) => Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(color: Colors.black.withOpacity(0.3), borderRadius: BorderRadius.circular(12)),
                      child: Text(t, style: const TextStyle(fontSize: 12, color: Colors.white70)),
                    ),
                  )).toList()),
                ],
                if (subtitle != null) ...[
                  const SizedBox(height: 12),
                  Text(subtitle!, style: const TextStyle(color: Colors.white70, fontSize: 14), maxLines: 2, overflow: TextOverflow.ellipsis),
                ],
                const SizedBox(height: 16),
                Row(children: [
                  if (onRead != null)
                    ElevatedButton.icon(
                      onPressed: onRead, icon: const Icon(Icons.play_arrow), label: const Text('开始阅读'),
                      style: ElevatedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      )),
                  if (onFavorite != null) ...[
                    const SizedBox(width: 12),
                    OutlinedButton.icon(
                      onPressed: onFavorite, icon: const Icon(Icons.bookmark_border), label: const Text('收藏'),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: Colors.white, side: const BorderSide(color: Colors.white54),
                        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      )),
                  ],
                ]),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
