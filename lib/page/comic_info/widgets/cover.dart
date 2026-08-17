import 'dart:io';

import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:loading_animation_widget/loading_animation_widget.dart';
import 'package:mangaverse/config/theme/mangaverse_theme.dart';
import 'package:mangaverse/util/context/context_extensions.dart';
import 'package:mangaverse/widgets/picture_bloc/models/picture_info.dart';

import 'package:mangaverse/config/router/router.gr.dart';
import 'package:mangaverse/widgets/picture_bloc/bloc/picture_bloc.dart';

class Cover extends StatelessWidget {
  final PictureInfo pictureInfo;
  final double height;
  final double borderRadius;
  final VoidCallback? onTap;

  const Cover({
    super.key,
    required this.pictureInfo,
    this.height = 180,
    this.borderRadius = 14,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final width = height / 4 * 3;
    final radius = BorderRadius.circular(borderRadius);

    return SizedBox(
      height: height,
      width: width,
      child: BlocProvider(
        create: (context) => PictureBloc()..add(GetPicture(pictureInfo)),
        child: BlocBuilder<PictureBloc, PictureLoadState>(
          builder: (context, state) {
            switch (state.status) {
              case PictureLoadStatus.initial:
                return DecoratedBox(
                  decoration: BoxDecoration(
                    borderRadius: radius,
                    color: MangaVerseColors.surfaceVariant.withValues(alpha: 0.3),
                    border: Border.all(color: MangaVerseColors.border),
                  ),
                  child: Center(
                    child: LoadingAnimationWidget.waveDots(
                      color: MangaVerseColors.accent,
                      size: 40,
                    ),
                  ),
                );
              case PictureLoadStatus.success:
                return InkWell(
                  onTap:
                      onTap ??
                      () {
                        context.pushRoute(
                          FullRouteImageRoute(imagePath: state.imagePath!),
                        );
                      },
                  child: ClipRRect(
                    borderRadius: radius,
                    child: Image.file(
                      File(state.imagePath!),
                      fit: BoxFit.cover,
                      width: width,
                      height: height,
                    ),
                  ),
                );
              case PictureLoadStatus.failure:
                if (state.result.toString().contains('404')) {
                  return ClipRRect(
                    borderRadius: radius,
                    child: Image.asset(
                      'asset/image/error_image/404.png',
                      fit: BoxFit.cover,
                    ),
                  );
                } else {
                  return InkWell(
                    onTap: () {
                      context.read<PictureBloc>().add(GetPicture(pictureInfo));
                    },
                    child: DecoratedBox(
                      decoration: BoxDecoration(
                        borderRadius: radius,
                        color: MangaVerseColors.surfaceVariant.withValues(alpha: 0.3),
                        border: Border.all(color: MangaVerseColors.border),
                      ),
                      child: const Center(
                        child: Icon(Icons.refresh, color: MangaVerseColors.accent),
                      ),
                    ),
                  );
                }
            }
          },
        ),
      ),
    );
  }
}
