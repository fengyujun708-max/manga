// (c) 2026 漫界 — CloudflareInterceptor 简化版
// 检测到 CF 挑战时抛 CloudflareException；交互式验证（webview）后续补
import 'dart:io' as io;

import 'package:dio/dio.dart';
import 'package:manjie/venera/foundation/log.dart';

import 'cookie_jar.dart';

class CloudflareException implements DioException {
  @override
  Object? error = "Cloudflare challenge detected";

  @override
  String get message => "Cloudflare challenge detected";

  @override
  RequestOptions get requestOptions => RequestOptions(path: '');
  @override
  Response<dynamic>? get response => null;
  @override
  DioExceptionType get type => DioExceptionType.unknown;
  @override
  StackTrace? get stackTrace => null;

  @override
  Object? get unknown => null;

  @override
  String toString() => "Cloudflare challenge detected";
}

class CloudflareInterceptor extends Interceptor {
  static const List<String> _titles = [
    "cf-chl",
    "challenge-platform",
    "Just a moment",
    "Checking your browser",
    "cf-browser-verification",
    "cf-chl-",
  ];

  final Uri uri;

  CloudflareInterceptor(this.uri);

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    if (response.statusCode == 503 && _isCloudflare(response.data)) {
      Log.warning("Cloudflare", "Detected Cloudflare challenge at $uri");
      handler.reject(CloudflareException());
    } else {
      handler.next(response);
    }
  }

  bool _isCloudflare(dynamic data) {
    if (data is! String) return false;
    return _titles.any((title) => data.contains(title));
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    handler.next(err);
  }
}

// ignore: avoid_classes_with_only_static_members
class CloudflareUtil {
  static bool isCloudflareIp(String ip) => false;
}
// ignore_for_file: unused_element
Future<void> _unusedSaveCookies(CookieJar jar, Uri uri) async {}