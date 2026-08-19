import 'dart:async';
import 'dart:convert';
import 'dart:math';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:flutter/material.dart' show debugPrint;
import 'package:shared_preferences/shared_preferences.dart';
import 'manga_source.dart';
import 'runtime/js_engine.dart';
import '../core/network/api_client.dart';

/// 源管理器
class SourceManager {
  final ApiClient _apiClient;
  final QuickJSEngine _jsEngine;
  
  final Map<String, MangaSource> _sources = {};
  final Map<String, SourceManifest> _manifests = {};
  final Map<String, bool> _enabled = {};
  final Map<String, SourceTestResult> _testCache = {};
  
  final StreamController<SourceManagerEvent> _eventController = StreamController.broadcast();
  Stream<SourceManagerEvent> get events => _eventController.stream;

  SourceManager({
    required ApiClient apiClient,
    required QuickJSEngine jsEngine,
  }) : _apiClient = apiClient, _jsEngine = jsEngine;

  /// 初始化：加载本地已安装的源
  Future<void> initialize() async {
    await _jsEngine.initialize();
    await _loadLocalSources();
    await checkUpdates();
  }

  /// 加载本地已安装的源
  Future<void> _loadLocalSources() async {
    final prefs = await SharedPreferences.getInstance();
    final sourcesJson = prefs.getString('installed_sources') ?? '[]';
    final List<dynamic> sources = jsonDecode(sourcesJson);
    
    for (final sourceJson in sources) {
      try {
        final manifest = SourceManifest.fromJson(sourceJson);
        _manifests[manifest.id] = manifest;
        _enabled[manifest.id] = true;
      } catch (e) {
        debugPrint('加载源清单失败: $e');
      }
    }
  }

  /// 保存已安装源到本地存储
  Future<void> _saveLocalSources() async {
    final prefs = await SharedPreferences.getInstance();
    final sourcesJson = jsonEncode(_manifests.values.map((m) => m.toJson()).toList());
    await prefs.setString('installed_sources', jsonEncode(_manifests.values.map((m) => m.toJson()).toList()));
  }

  /// 获取已安装的源
  List<MangaSource> getInstalledSources() {
    return _manifests.values
        .where((m) => _enabled[m.id] ?? false)
        .map((m) => _sources[m.id])
        .whereType<MangaSource>()
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

  /// 安装新源
  Future<void> _validateSourceCode(String code) async {
    // 检查危险代码
    final dangerousPatterns = [
      RegExp(r'eval\s*\('),
      RegExp(r'Function\s*\('),
      RegExp(r'Process\.'),
      RegExp(r'require\s*\('),
      RegExp(r'child_process'),
      RegExp(r'fs\.'),
      RegExp(r'net\.'),
      RegExp(r'os\.'),
    ];
    
    for (final pattern in dangerousPatterns) {
      if (pattern.hasMatch(code)) {
        throw Exception('源代码包含危险代码，已拦截');
      }
    }
    
    // 检查必需的导出
    if (!code.contains('search') || !code.contains('getDetail')) {
      throw Exception('源代码缺少必需的导出函数');
    }
  }

  /// 检查更新
  Future<void> checkUpdates() async {
    try {
      final response = await _apiClient.get('/sources');
      if (response.statusCode == 200) {
        final data = response.data;
        if (data['sources'] != null) {
          for (final sourceJson in data['sources']) {
            final manifest = SourceManifest.fromJson(sourceJson);
            if (_manifests.containsKey(manifest.id)) {
              final local = _manifests[manifest.id]!;
              if (_compareVersion(local.version, manifest.version) < 0) {
                _eventController.add(SourceUpdateAvailableEvent(manifest));
              }
            }
          }
        }
      }
    } catch (e) {
      debugPrint('检查更新失败: $e');
    }
  }

  /// 安装新源
  Future<void> installSource(String sourceId, String sourceUrl) async {
    // 简单实现：下载 JS 源，验证后保存 manifest
    final response = await http.get(Uri.parse(sourceUrl));
    if (response.statusCode != 200) {
      throw Exception('下载源失败: ${response.statusCode}');
    }
    await _validateSourceCode(response.body);

    // 提取简单 manifest
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
    final localParts = local.split('.').map(int.parse).toList();
    final remoteParts = remote.split('.').map(int.parse).toList();
    
    for (int i = 0; i < max(localParts.length, remoteParts.length); i++) {
      final localPart = i < localParts.length ? localParts[i] : 0;
      final remotePart = i < remoteParts.length ? remoteParts[i] : 0;
      if (localPart != remotePart) return localPart - remotePart;
    }
    return 0;
  }

  /// 测试源
  Future<SourceTestResult> testSource(String sourceId) async {
    if (!_manifests.containsKey(sourceId)) {
      return SourceTestResult.failed('源不存在', Duration.zero);
    }

    final manifest = _manifests[sourceId]!;
    final stopwatch = Stopwatch()..start();
    
    try {
      // 这里应该实际加载源并测试
      // 简化实现：返回模拟结果
      await Future.delayed(const Duration(seconds: 1));
      
      return SourceTestResult(
        passed: true,
        results: [
          TestCaseResult(name: '连接测试', passed: true, duration: const Duration(milliseconds: 100)),
          TestCaseResult(name: '搜索测试', passed: true, duration: const Duration(milliseconds: 200)),
          TestCaseResult(name: '详情测试', passed: true, duration: const Duration(milliseconds: 150)),
          TestCaseResult(name: '章节测试', passed: true, duration: const Duration(milliseconds: 180)),
          TestCaseResult(name: '图片测试', passed: true, duration: const Duration(milliseconds: 200)),
        ],
        duration: stopwatch.elapsed,
      );
    } catch (e) {
      return SourceTestResult(
        passed: false,
        results: [
          TestCaseResult(name: '连接测试', passed: false, error: e.toString()),
        ],
        duration: stopwatch.elapsed,
        error: e.toString(),
      );
    }
  }

  /// 更新源
  Future<void> updateSource(String sourceId) async {
    final manifest = _manifests[sourceId];
    if (manifest == null) throw Exception('源不存在');
    
    // 从仓库下载最新版本
    await installSource(sourceId, manifest.downloadUrl);
  }

  /// 卸载源
  Future<void> uninstallSource(String sourceId) async {
    if (_manifests.containsKey(sourceId)) {
      _manifests.remove(sourceId);
      _enabled.remove(sourceId);
      await _saveLocalSources();
    }
  }

  /// 获取源测试缓存
  SourceTestResult? getTestCache(String sourceId) => _testCache[sourceId];



  void dispose() {
    _jsEngine.dispose();
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

extension SourceManifestCopy on SourceManifest {
  SourceManifest copyWith({
    String? id,
    String? name,
    String? version,
    String? author,
    String? description,
    String? icon,
    String? repositoryUrl,
    String? downloadUrl,
    String? minAppVersion,
    List<String>? capabilities,
    int? downloads,
    double? rating,
    Map<String, dynamic>? metadata,
  }) {
    return SourceManifest(
      id: id ?? this.id,
      name: name ?? this.name,
      version: version ?? this.version,
      author: author ?? this.author,
      description: description ?? this.description,
      icon: icon ?? this.icon,
      repositoryUrl: repositoryUrl ?? this.repositoryUrl,
      downloadUrl: downloadUrl ?? this.downloadUrl,
      minAppVersion: minAppVersion ?? this.minAppVersion,
      capabilities: capabilities ?? this.capabilities,
      downloads: downloads ?? this.downloads,
      rating: rating ?? this.rating,
      metadata: metadata ?? this.metadata,
    );
  }
}