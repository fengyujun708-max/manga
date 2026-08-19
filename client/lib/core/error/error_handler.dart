import 'package:flutter/material.dart';
import '../app/components/manjie_toast.dart';

/// 全局错误处理器
class ErrorHandler {
  /// 处理 API 错误，返回用户友好的中文提示
  static String resolveError(dynamic error) {
    // DioException
    if (error.toString().contains('DioException')) {
      final msg = error.toString();

      if (msg.contains('connectionTimeout') || msg.contains('connectTimeout')) {
        return '网络连接超时，请检查网络设置';
      }
      if (msg.contains('receiveTimeout')) {
        return '服务器响应超时，请稍后重试';
      }
      if (msg.contains('SocketException') || msg.contains('Connection refused')) {
        return '无法连接到服务器';
      }
      if (msg.contains('HttpStatusError')) {
        // 提取状态码
        final statusMatch = RegExp(r'statusCode: (\d+)').firstMatch(msg);
        if (statusMatch != null) {
          return _httpError(int.parse(statusMatch.group(1)!));
        }
      }
      if (msg.contains('No Internet') || msg.contains('Network is unreachable')) {
        return '网络不可用，请检查连接';
      }
      return '网络请求失败，请稍后重试';
    }

    // 一般异常
    if (error is Exception) {
      final msg = error.toString();
      if (msg.contains('timeout')) return '操作超时';
      if (msg.contains('permission')) return '权限不足';
      if (msg.contains('not found') || msg.contains('不存在')) return '请求的资源不存在';
      return '操作失败: ${msg.replaceAll('Exception: ', '')}';
    }

    return '发生未知错误';
  }

  static String _httpError(int statusCode) {
    switch (statusCode) {
      case 400: return '请求参数错误';
      case 401: return '登录已过期，请重新登录';
      case 403: return '没有权限执行此操作';
      case 404: return '请求的资源不存在';
      case 409: return '操作冲突，请刷新后重试';
      case 429: return '请求过于频繁，请稍后重试';
      case 500: return '服务器内部错误';
      case 502: return '服务器暂时不可用';
      case 503: return '服务维护中，请稍后访问';
      default: return '服务器错误 ($statusCode)';
    }
  }

  /// 在界面上显示错误
  static void showError(BuildContext context, dynamic error) {
    final message = resolveError(error);
    ManjieToast.error(context, message);
  }
}

/// 统一数据加载状态
enum LoadState { idle, loading, success, error, empty }

/// 带状态的数据包装器
class DataWrapper<T> {
  final LoadState state;
  final T? data;
  final String? error;
  final String? emptyMessage;

  const DataWrapper({
    this.state = LoadState.idle,
    this.data,
    this.error,
    this.emptyMessage = '暂无数据',
  });

  factory DataWrapper.loading() => const DataWrapper(state: LoadState.loading);
  factory DataWrapper.success(T data) => DataWrapper(state: LoadState.success, data: data);
  factory DataWrapper.error(String message) => DataWrapper(state: LoadState.error, error: message);
  factory DataWrapper.empty([String? msg]) => DataWrapper(state: LoadState.empty, emptyMessage: msg);

  bool get isLoading => state == LoadState.loading;
  bool get isSuccess => state == LoadState.success;
  bool get isError => state == LoadState.error;
  bool get isEmpty => state == LoadState.empty;
}

/// 自动重试工具
class RetryHelper {
  static const int maxRetries = 3;
  static const Duration baseDelay = Duration(seconds: 1);

  /// 带指数退避的重试
  static Future<T> retry<T>({
    required Future<T> Function() action,
    int maxAttempts = maxRetries,
    bool Function(Exception)? shouldRetry,
  }) async {
    int attempt = 0;
    while (true) {
      try {
        return await action();
      } catch (e) {
        attempt++;
        if (attempt >= maxAttempts) rethrow;
        if (shouldRetry != null && !shouldRetry(e as Exception)) rethrow;

        // 指数退避
        await Future.delayed(baseDelay * (1 << attempt));
      }
    }
  }
}

/// 页面状态混合 - 给 StatefulWidget 使用
mixin PageStateMixin<T extends StatefulWidget> on State<T> {
  bool _disposed = false;

  @override
  void dispose() {
    _disposed = true;
    super.dispose();
  }

  bool get isDisposed => _disposed;

  /// 安全 setState，防止 disposed 后调用
  @protected
  void safeSetState(VoidCallback fn) {
    if (!_disposed && mounted) {
      setState(fn);
    }
  }

  /// 安全执行异步操作
  Future<void> safeRun(Future<void> Function() action) async {
    try {
      await action();
    } catch (e) {
      if (!_disposed && mounted) {
        ErrorHandler.showError(context, e);
      }
    }
  }
}