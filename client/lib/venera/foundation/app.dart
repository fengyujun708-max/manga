import 'dart:io';
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';

/// 漫界 App 全局（移植自 venera foundation/app.dart 的最小子集）
class _App {
  final version = "1.2.0";

  bool get isAndroid => Platform.isAndroid;
  bool get isIOS => Platform.isIOS;
  bool get isWindows => Platform.isWindows;
  bool get isLinux => Platform.isLinux;
  bool get isMacOS => Platform.isMacOS;
  bool get isDesktop => Platform.isWindows || Platform.isLinux || Platform.isMacOS;
  bool get isMobile => Platform.isAndroid || Platform.isIOS;

  bool isInitialized = false;

  Locale get locale {
    Locale deviceLocale = PlatformDispatcher.instance.locale;
    if (deviceLocale.languageCode == "zh" && deviceLocale.scriptCode == "Hant") {
      deviceLocale = const Locale("zh", "TW");
    }
    return deviceLocale;
  }

  late String dataPath;
  late String cachePath;
  String? externalStoragePath;

  final rootNavigatorKey = GlobalKey<NavigatorState>();

  GlobalKey<NavigatorState>? mainNavigatorKey;

  BuildContext get rootContext => rootNavigatorKey.currentContext!;

  Future<void> function() async {
    final docs = await getApplicationDocumentsDirectory();
    dataPath = docs.path;
    cachePath = '${docs.path}/cache';
    isInitialized = true;
  }

  void rootPop() {
    if (rootNavigatorKey.currentContext != null) {
      Navigator.of(rootNavigatorKey.currentContext!, rootNavigator: true).pop();
    }
  }
}

final App = _App();
