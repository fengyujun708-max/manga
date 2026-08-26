import 'dart:ui' show PlatformDispatcher;
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:get_it/get_it.dart';
import 'app/app.dart';
import 'core/network/api_client.dart';
import 'core/network/log_reporter.dart';
import 'core/storage/secure_storage.dart';
import 'features/auth/bloc/auth_bloc.dart';
import 'plugins/source_installer.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // 全局错误捕获 → 上报服务器
  FlutterError.onError = (details) {
    final msg = '${details.exception}\n${details.stack ?? ''}';
    LogReporter.instance.report('error', 'FlutterError', msg.substring(0, msg.length.clamp(0, 2000)));
    FlutterError.dumpErrorToConsole(details);
  };
  PlatformDispatcher.instance.onError = (error, stack) {
    LogReporter.instance.report('error', 'Uncaught', '$error\n$stack');
    return true;
  };

  final getIt = GetIt.instance;
  getIt.registerSingleton<SecureStorage>(SecureStorage());
  getIt.registerSingleton<ApiClient>(ApiClient());

  // ── 自动解压内置源（首次安装 / 更新时触发）───────────────────────
  // 避免发现页首次打开空白灰屏
  try {
    await SourceInstaller.extractBundledSources();
  } catch (e) {
    debugPrint('[Main] extractBundledSources failed: $e');
  }

  runApp(
    BlocProvider(
      create: (_) => AuthBloc(
        apiClient: getIt<ApiClient>(),
        storage: getIt<SecureStorage>(),
      )..add(AuthCheckRequested()),
      child: const ManjieApp(),
    ),
  );
}