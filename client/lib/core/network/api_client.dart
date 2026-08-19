import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class ApiClient {
  late final Dio _dio;
  final _storage = const FlutterSecureStorage();

  static const String _baseUrlKey = 'api_base_url';
  static const String _accessTokenKey = 'access_token';
  static const String _refreshTokenKey = 'refresh_token';

  ApiClient({String? baseUrl}) {
    _dio = Dio(BaseOptions(
      baseUrl: baseUrl ?? 'http://39.106.192.137:3000/v1',
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 15),
      headers: {'Content-Type': 'application/json'},
    ));

    _dio.interceptors.add(AuthInterceptor(this));
    _dio.interceptors.add(LogInterceptor(responseBody: true));
  }

  Future<String?> get accessToken => _storage.read(key: _accessTokenKey);
  Future<String?> get refreshToken => _storage.read(key: _refreshTokenKey);

  Future<void> setTokens(String access, String refresh) async {
    await _storage.write(key: _accessTokenKey, value: access);
    await _storage.write(key: _refreshTokenKey, value: refresh);
  }

  Future<void> clearTokens() async {
    await _storage.delete(key: _accessTokenKey);
    await _storage.delete(key: _refreshTokenKey);
  }

  Future<Response<T>> get<T>(String path, {Map<String, dynamic>? params}) =>
      _dio.get(path, queryParameters: params);

  Future<Response<T>> post<T>(String path, {dynamic data}) =>
      _dio.post(path, data: data);

  Future<Response<T>> put<T>(String path, {dynamic data}) =>
      _dio.put(path, data: data);

  Future<Response<T>> delete<T>(String path) =>
      _dio.delete(path);
}

class AuthInterceptor extends Interceptor {
  final ApiClient client;
  AuthInterceptor(this.client);

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) async {
    final token = await client.accessToken;
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) async {
    if (err.response?.statusCode == 401) {
      // 尝试刷新 token
      final refreshToken = await client.refreshToken;
      if (refreshToken != null) {
        try {
          final res = await client.post('/auth/refresh', data: {'refreshToken': refreshToken});
          final newAccess = res.data['accessToken'];
          final newRefresh = res.data['refreshToken'];
          await client.setTokens(newAccess, newRefresh);

          // 重试原请求
          err.requestOptions.headers['Authorization'] = 'Bearer $newAccess';
          final retry = await client._dio.fetch(err.requestOptions);
          return handler.resolve(retry);
        } catch (_) {
          await client.clearTokens();
        }
      }
    }
    handler.next(err);
  }
}

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class SecureStorage {
  final _storage = const FlutterSecureStorage();

  Future<void> write(String key, String value) => _storage.write(key: key, value: value);
  Future<String?> read(String key) => _storage.read(key: key);
  Future<void> delete(String key) => _storage.delete(key: key);
  Future<void> clear() => _storage.deleteAll();
}