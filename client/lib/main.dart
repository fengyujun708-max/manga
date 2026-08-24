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