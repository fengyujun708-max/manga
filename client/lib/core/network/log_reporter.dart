import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:device_info_plus/device_info_plus.dart';

/// 客户端日志上报 —— 收集本地日志，批量上报到漫界服务器
class LogReporter {
  static final LogReporter instance = LogReporter._();
  LogReporter._();

  final List<Map<String, dynamic>> _buffer = [];
  bool _uploading = false;

  static const String _endpoint = 'http://39.106.192.137/v1/logs/report';

  String? _deviceModel;
  String? _appVersion;
  String _platform = 'android';

  Future<void> _ensureDeviceInfo() async {
    if (_deviceModel != null) return;
    try {
      final di = DeviceInfoPlugin();
      final info = await di.androidInfo;
      _deviceModel = '${info.brand} ${info.model}';
      _platform = 'android';
    } catch (_) {
      _deviceModel = 'unknown';
    }
  }

  /// 添加一条日志（立即异步上报）
  void report(String level, String title, String content) {
    _buffer.add({
      'level': level,
      'title': title,
      'content': content,
    });
    _flushSoon();
  }

  /// 记录源加载错误（专门通道）
  void reportSourceError(String sourceId, String error) {
    report('error', '源加载失败 [$sourceId]', error);
  }

  void _flushSoon() async {
    if (_uploading) return;
    _uploading = true;
    // 稍微聚合几毫秒，避免每条都单独请求
    await Future.delayed(const Duration(seconds: 2));
    await _flush();
    _uploading = false;
  }

  Future<void> _flush() async {
    if (_buffer.isEmpty) return;
    await _ensureDeviceInfo();
    final logs = List<Map<String, dynamic>>.from(_buffer);
    _buffer.clear();

    try {
      final resp = await http
          .post(
        Uri.parse(_endpoint),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'logs': logs.map((l) {
            l['deviceModel'] = _deviceModel;
            l['appVersion'] = _appVersion ?? '1.0.0';
            l['platform'] = _platform;
            return l;
          }).toList(),
        }),
      )
          .timeout(const Duration(seconds: 10));
      if (resp.statusCode != 200) {
        // 失败重放回缓冲
        _buffer.insertAll(0, logs);
      }
    } catch (_) {
      // 网络失败重放，最多保留 200 条防止无限膨胀
      if (_buffer.length < 200) _buffer.insertAll(0, logs);
    }
  }

  /// 立即同步 flush（应用退出/切后台时调用）
  Future<void> flushNow() async {
    await _flush();
  }
}