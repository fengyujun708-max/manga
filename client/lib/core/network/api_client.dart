import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../../app/config/app_config.dart';

class ApiClient {
  late final Dio _dio;
  final _storage = const FlutterSecureStorage();

  static const String _accessTokenKey = 'access_token';
  static const String _refreshTokenKey = 'refresh_token';

  // Public 端点不需要 Authorization
  static const _publicPaths = [
    '/auth/login',
    '/auth/register',
    '/auth/guest',
    '/auth/captcha',
    '/auth/refresh',
    '/sources',
  ];

  ApiClient({String? baseUrl}) {
    _dio = Dio(BaseOptions(
      baseUrl: baseUrl ?? AppConfig.apiBaseUrl,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 30),
      headers: {'Content-Type': 'application/json'},
    ));

    _dio.interceptors.add(AuthInterceptor(this));
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

  bool _isPublicPath(String path) {
    for (final p in _publicPaths) {
      if (path.startsWith(p)) return true;
    }
    return false;
  }

  Future<Response<T>> get<T>(String path, {Map<String, dynamic>? params}) =>
      _dio.get(path, queryParameters: params);

  Future<Response<T>> post<T>(String path, {dynamic data}) =>
      _dio.post(path, data: data);

  Future<Response<T>> put<T>(String path, {dynamic data}) =>
      _dio.put(path, data: data);

  Future<Response<T>> delete<T>(String path) =>
      _dio.delete(path);

  /// 直接用 Dio 发请求（绕过 AuthInterceptor），用于 refresh token
  Future<Response<T>> rawPost<T>(String path, {dynamic data}) async {
    final tempDio = Dio(BaseOptions(
      baseUrl: AppConfig.apiBaseUrl,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 15),
      headers: {'Content-Type': 'application/json'},
    ));
    return tempDio.post<T>(path, data: data);
  }
}

class AuthInterceptor extends Interceptor {
  final ApiClient client;
  bool _isRefreshing = false;

  AuthInterceptor(this.client);

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) async {
    // Public 端点不加 token（避免过期 token 干扰登录/注册）
    if (!client._isPublicPath(options.path)) {
      final token = await client.accessToken;
      if (token != null) {
        options.headers['Authorization'] = 'Bearer $token';
      }
    }
    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) async {
    // 只处理 401 且不在 refresh 中
    if (err.response?.statusCode == 401 && !_isRefreshing) {
      _isRefreshing = true;
      final refreshToken = await client.refreshToken;
      if (refreshToken != null) {
        try {
          // 用独立 Dio 发 refresh 请求，绕过拦截器，避免递归
          final res = await client.rawPost<Map<String, dynamic>>(
            '/auth/refresh',
            data: {'refreshToken': refreshToken},
          );
          final newAccess = res.data?['accessToken'] as String?;
          final newRefresh = res.data?['refreshToken'] as String?;
          if (newAccess != null && newRefresh != null) {
            await client.setTokens(newAccess, newRefresh);
            _isRefreshing = false;
            // 重试原请求
            err.requestOptions.headers['Authorization'] = 'Bearer $newAccess';
            final retry = await client._dio.fetch(err.requestOptions);
            return handler.resolve(retry);
          }
        } catch (_) {
          // refresh 失败 → 清 token，不重试
        }
      }
      // refresh 失败或无 refresh token → 清除登录态
      await client.clearTokens();
      _isRefreshing = false;
    }
    handler.next(err);
  }
}