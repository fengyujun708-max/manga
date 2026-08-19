import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:get_it/get_it.dart';
import 'app/app.dart';
import 'core/network/api_client.dart';
import 'core/storage/secure_storage.dart';
import 'features/auth/bloc/auth_bloc.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

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