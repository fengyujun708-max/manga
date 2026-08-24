/// 桩文件 — 替代 Venera 的 cache_manager 模块
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';

class CacheManager implements Interceptor {
  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) => handler.next(options);
  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) => handler.next(response);
  @override
  void onError(DioException err, ErrorInterceptorHandler handler) => handler.next(err);
}

class NetworkCacheManager extends CacheManager {}
