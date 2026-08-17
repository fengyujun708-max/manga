import 'package:flutter/material.dart';
import 'package:mangaverse/config/theme/mangaverse_theme.dart';
import 'package:mangaverse/util/context/context_extensions.dart';

class Delimiter extends StatelessWidget {
  const Delimiter({super.key});

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.center,
      child: SizedBox(
        width: context.screenWidth * (48 / 50),
        child: const Divider(
          color: MangaVerseColors.border,
          thickness: 1,
          height: 15,
        ),
      ),
    );
  }
}
