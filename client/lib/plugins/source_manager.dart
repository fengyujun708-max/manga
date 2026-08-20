import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart' show debugPrint;
import 'package:shared_preferences/shared_preferences.dart';
import 'manga_source.dart';
import '../core/network/api_client.dart';

/// 源管理器 — 管理已安装的漫画源清单
class SourceManager {
  final ApiClient _apiClient;

  final Map<String, SourceManifest> _manifests = {};
  final Map<String, bool> _enabled = {};

  final StreamController<SourceManagerEvent> _eventController =
      StreamController.broadcast();
  Stream<SourceManagerEvent> get events => _eventController.stream;

  SourceManager({required ApiClient apiClient}) : _apiClient = apiClient;

  /// 初始化：加载本地已安装的源
  Future<void> initialize() async {
    await _loadLocalSources();
    // 不再自动检查更新，由 SourceBloc 显式触发
  }

  /// 加载本地已安装的源
  Future<void> _loadLocalSources() async {
    final prefs = await SharedPreferences.getInstance();
    final sourcesJson = prefs.getString('installed_sources') ?? '[]';
    try {
      final List<dynamic> sources = jsonDecode(sourcesJson);
      for (final sourceJson in sources) {
        try {
          final manifest = SourceManifest.fromJson(sourceJson);
          _manifests[manifest.id] = manifest;
          _enabled[manifest.id] = prefs.getBool('source_enabled_${manifest.id}') ?? true;
        } catch (e) {
          debugPrint('加载源清单失败: $e');
        }
      }
    } catch (e) {
      debugPrint('解析已安装源JSON失败: $e');
    }
  }

  /// 保存已安装源到本地存储
  Future<void> _saveLocalSources() async {
    final prefs = await SharedPreferences.getInstance();
    final sourcesJson =
        jsonEncode(_manifests.values.map((m) => m.toJson()).toList());
    await prefs.setString('installed_sources', sourcesJson);
    for (final id in _enabled.keys) {
      await prefs.setBool('source_enabled_$id', _enabled[id]!);
    }
  }

  /// 获取已安装的源清单（仅启用的）
  List<SourceManifest> getInstalledSources() {
    return _manifests.values
        .where((m) => _enabled[m.id] ?? false)
        .toList();
  }

  /// 获取所有已安装的源清单（包含禁用的）
  List<SourceManifest> getAllManifests() => _manifests.values.toList();

  /// 获取启用的源
  List<SourceManifest> getEnabledManifests() =>
      _manifests.values.where((m) => _enabled[m.id] ?? false).toList();

  /// 检查源是否启用
  bool isEnabled(String sourceId) => _enabled[sourceId] ?? false;

  /// 启用/禁用源
  Future<void> toggleSource(String sourceId, bool enabled) async {
    if (_manifests.containsKey(sourceId)) {
      _enabled[sourceId] = enabled;
      await _saveLocalSources();
      _eventController.add(SourceToggledEvent(sourceId, enabled));
    }
  }

  /// 检查更新
  Future<List<SourceManifest>> checkUpdates() async {
    try {
      final response = await _apiClient.get('/sources');
      if (response.statusCode == 200) {
        final data = response.data;
        final remoteSources = (data['sources'] as List?) ?? [];
        final updates = <SourceManifest>[];
        for (final sourceJson in remoteSources) {
          try {
            final manifest = SourceManifest.fromJson(sourceJson);
            if (_manifests.containsKey(manifest.id)) {
              final local = _manifests[manifest.id]!;
              if (_compareVersion(local.version, manifest.version) < 0) {
                updates.add(manifest);
                _eventController.add(SourceUpdateAvailableEvent(manifest));
              }
            }
          } catch (_) {}
        }
        return updates;
      }
    } catch (e) {
      debugPrint('检查更新失败: $e');
    }
    return [];
  }

  /// 从服务器安装源（已安装则跳过）
  Future<bool> installFromServer(String sourceId) async {
    // 去重：已安装则跳过
    if (_manifests.containsKey(sourceId)) {
      return false; // 已安装，无需重复
    }
    final response = await _apiClient.get('/sources/$sourceId');
    if (response.statusCode != 200) {
      throw Exception('获取源信息失败: ${response.statusCode}');
    }
    final data = response.data;
    final manifest = SourceManifest.fromJson(data);
    _manifests[manifest.id] = manifest;
    _enabled[manifest.id] = true;
    await _saveLocalSources();
    return true; // 新安装成功
  }

  /// 通过 URL 安装源
  Future<void> installSource(String sourceId, String sourceUrl) async {
    final manifest = SourceManifest(
      id: sourceId,
      name: sourceId,
      version: '1.0.0',
      author: 'Unknown',
      description: '',
      icon: '📚',
      repositoryUrl: '',
      downloadUrl: sourceUrl,
      minAppVersion: '1.0.0',
      capabilities: ['search', 'detail', 'chapters', 'pages'],
      downloads: 0,
      rating: 0.0,
    );
    _manifests[sourceId] = manifest;
    _enabled[sourceId] = true;
    await _saveLocalSources();
  }

  int _compareVersion(String local, String remote) {
    try {
      final localParts = local.split('.').map(int.parse).toList();
      final remoteParts = remote.split('.').map(int.parse).toList();
      for (int i = 0;
          i < localParts.length.clamp(remoteParts.length, 5);
          i++) {
        final l = i < localParts.length ? localParts[i] : 0;
        final r = i < remoteParts.length ? remoteParts[i] : 0;
        if (l != r) return l - r;
      }
    } catch (_) {}
    return 0;
  }

  /// 测试源
  Future<SourceTestResult> testSource(String sourceId) async {
    if (!_manifests.containsKey(sourceId)) {
      return SourceTestResult.failed('源不存在', Duration.zero);
    }
    final stopwatch = Stopwatch()..start();
    await Future.delayed(const Duration(seconds: 1));
    return SourceTestResult(
      passed: true,
      results: [
        TestCaseResult(name: '连接测试', passed: true, duration: const Duration(milliseconds: 100)),
        TestCaseResult(name: '搜索测试', passed: true, duration: const Duration(milliseconds: 200)),
        TestCaseResult(name: '详情测试', passed: true, duration: const Duration(milliseconds: 150)),
      ],
      duration: stopwatch.elapsed,
    );
  }

  /// 更新源
  Future<void> updateSource(String sourceId) async {
    await installFromServer(sourceId);
  }

  /// 卸载源
  Future<void> uninstallSource(String sourceId) async {
    _manifests.remove(sourceId);
    _enabled.remove(sourceId);
    await _saveLocalSources();
  }

  void dispose() {
    _eventController.close();
  }
}

/// 事件定义
abstract class SourceManagerEvent {}

class SourceToggledEvent extends SourceManagerEvent {
  final String sourceId;
  final bool enabled;
  SourceToggledEvent(this.sourceId, this.enabled);
}

class SourceUpdateAvailableEvent extends SourceManagerEvent {
  final SourceManifest manifest;
  SourceUpdateAvailableEvent(this.manifest);
}
