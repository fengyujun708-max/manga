import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:mangaverse/config/router/router.gr.dart';
import 'package:mangaverse/page/comic_list/models/comic_list_scene.dart';
import 'package:mangaverse/page/search/cubit/search_cubit.dart';
import 'package:mangaverse/page/search_result/bloc/search_bloc.dart';
import 'package:mangaverse/util/json/json_value.dart';

Future<void> handleComicInfoAction(
  BuildContext context,
  Map<String, dynamic> action, {
  required String fallbackPluginId,
}) async {
  final type = action['type']?.toString().trim() ?? '';
  final payload = asJsonMap(action['payload']);

  if (type.isEmpty || type == 'none') {
    return;
  }

  if (type == 'openSearch') {
    final pluginId = _sourceIdFromString(
      payload['source']?.toString(),
      fallbackPluginId,
    );
    final keyword = payload['keyword']?.toString() ?? '';
    final externPatch = asJsonMap(payload['extern']);

    final searchStates = SearchStates.initial().copyWith(
      from: pluginId,
      searchKeyword: keyword,
      pluginExtern: externPatch,
    );

    context.pushRoute(
      SearchResultRoute(
        searchEvent: SearchEvent().copyWith(searchStates: searchStates),
      ),
    );
    return;
  }

  if (type == 'openWeb') {
    final title = payload['title']?.toString() ?? '';
    final url = payload['url']?.toString() ?? '';
    if (url.isEmpty) {
      return;
    }

    context.pushRoute(WebViewRoute(info: [title, url]));

    return;
  }

  if (type == 'openComicList') {
    final sceneMap = Map<String, dynamic>.from(asJsonMap(payload['scene']));
    if ((sceneMap['source']?.toString().trim() ?? '').isEmpty) {
      sceneMap['source'] = fallbackPluginId;
    }
    final scene = ComicListScene.fromMap(sceneMap);
    context.pushRoute(ComicListRoute(scene: scene, title: scene.title));
  }
}

String _sourceIdFromString(String? source, String fallbackPluginId) {
  final resolved = (source ?? '').trim();
  return resolved.isEmpty ? fallbackPluginId : resolved;
}
