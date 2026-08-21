import 'dart:async';
import 'dart:io';
import 'package:get_it/get_it.dart';
import '../core/network/api_client.dart';
import 'source_installer.dart';
import 'runtime/venera_engine.dart';

/// 源数据服务 —— 优先本地执行（Venera 模式），本地无 JS 时回退服务器代理
/// 本地执行时网络直连手机（用户开 VPN 即可访问海外源）
class SourceDataService {
  static SourceDataService? _instance;
  static SourceDataService get instance => _instance ??= SourceDataService();

  final VeneraEngine engine = VeneraEngine.instance;
  final Map<String, bool> _hasLocalJs = {};

  /// 检查本地是否有该源的 JS 文件
  Future<bool> hasLocalJs(String sourceId) async {
    if (_hasLocalJs.containsKey(sourceId)) return _hasLocalJs[sourceId]!;
    try {
      final dir = await SourceInstaller.ensureSourceDir();
      if (dir == null) return false;
      final file = File('$dir/$sourceId.js');
      final ok = await file.exists();
      _hasLocalJs[sourceId] = ok;
      return ok;
    } catch (_) {
      return false;
    }
  }

  /// 加载本地源并执行（可用时返回 true）
  Future<bool> loadLocal(String sourceId) async {
    try {
      final dir = await SourceInstaller.ensureSourceDir();
      if (dir == null) return false;
      final file = File('$dir/$sourceId.js');
      if (!await file.exists()) return false;
      final code = await file.readAsString();
      await engine.executeSource(sourceId, code);
      return true;
    } catch (_) {
      return false;
    }
  }

  /// 获取首页板块（本地优先，服务器兜底）
  Future<Map<String, dynamic>> explore(String sourceId) async {
    // 先尝试本地
    if (await loadLocal(sourceId)) {
      try {
        final raw = await engine.evaluateAwait('globalThis.__exploreAll__("$sourceId")');
        if (raw is Map && raw['error'] != null) {
          // 本地失败，回退服务器
        } else if (raw is List && raw.isNotEmpty) {
          return {'sections': raw, 'mode': 'local'};
        }
      } catch (_) {}
    }
    // 服务器兜底
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/source/$sourceId/explore');
      final data = res.data;
      if (data is Map) {
        return {
          'sections': data['sections'] ?? [],
          'mode': 'server',
        };
      }
    } catch (_) {}
    return {'sections': [], 'mode': 'none', 'error': '源暂不可用'};
  }

  /// 搜索（本地优先）
  Future<Map<String, dynamic>> search(String sourceId, String keyword, int page) async {
    if (await loadLocal(sourceId)) {
      try {
        final raw = await engine.evaluateAwait('globalThis.__search__("$sourceId", "${_jsStr(keyword)}", $page)');
        if (raw is Map && raw['error'] == null) {
          return {'items': raw['items'] ?? [], 'hasMore': raw['hasMore'] == true, 'mode': 'local'};
        }
      } catch (_) {}
    }
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/source/$sourceId/search', params: {'q': keyword, 'page': page});
      final data = res.data;
      return {
        'items': data is Map ? (data['items'] ?? data['comics'] ?? []) : [],
        'hasMore': data is Map ? (data['hasMore'] == true) : false,
        'mode': 'server',
      };
    } catch (_) {
      return {'items': [], 'hasMore': false, 'mode': 'none', 'error': '搜索失败'};
    }
  }

  /// 分类（本地优先）
  Future<List<Map<String, dynamic>>> categories(String sourceId) async {
    if (await loadLocal(sourceId)) {
      try {
        final raw = await engine.evaluateAwait('globalThis.__categories__("$sourceId")');
        if (raw is List && raw.isNotEmpty) {
          return (raw as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
        }
      } catch (_) {}
    }
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/source/$sourceId/categories');
      final data = res.data;
      if (data is Map && data['categories'] is List) {
        return (data['categories'] as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
      }
    } catch (_) {}
    return [];
  }

  /// 分类漫画（分页）
  Future<Map<String, dynamic>> categoryComics(String sourceId, String name, int page,
      [String param = '', List<String> options = const [], bool ranking = false]) async {
    if (await loadLocal(sourceId)) {
      try {
        final raw = await engine.evaluateAwait(
            'globalThis.__categoryComics__("$sourceId", "${_jsStr(name)}", "${_jsStr(param)}", ${options.map(_jsStr).toList()}, $page)');
        if (raw is Map && raw['error'] == null) {
          return {'items': raw['items'] ?? [], 'hasMore': raw['hasMore'] == true, 'mode': 'local'};
        }
      } catch (_) {}
    }
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/source/$sourceId/categoryComics',
          params: {'name': name, 'page': page, 'param': param, 'options': options.join(',')});
      final data = res.data;
      return {
        'items': data is Map ? (data['items'] ?? []) : [],
        'hasMore': data is Map ? (data['hasMore'] == true) : false,
        'mode': 'server',
      };
    } catch (_) {
      return {'items': [], 'hasMore': false, 'mode': 'none', 'error': '加载失败'};
    }
  }

  /// 详情 + 章节（本地优先）
  Future<Map<String, dynamic>> comic(String sourceId, String comicId) async {
    if (await loadLocal(sourceId)) {
      try {
        final raw = await engine.evaluateAwait('globalThis.__comic__("$sourceId", "${_jsStr(comicId)}")');
        if (raw is Map) return {'detail': raw, 'mode': 'local'};
      } catch (_) {}
    }
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/source/$sourceId/comic/$comicId');
      final data = res.data;
      if (data is Map) {
        return {'detail': data, 'chapters': data['chapters'] ?? [], 'mode': 'server'};
      }
    } catch (_) {}
    return {'detail': {}, 'chapters': [], 'mode': 'none', 'error': '加载失败'};
  }

  /// 图片页（本地优先）
  Future<Map<String, dynamic>> pages(String sourceId, String comicId, String epId) async {
    if (await loadLocal(sourceId)) {
      try {
        final raw = await engine.evaluateAwait('globalThis.__pages__("$sourceId", "${_jsStr(comicId)}", "${_jsStr(epId)}")');
        if (raw is Map && raw['pages'] != null) {
          return {'pages': raw['pages'], 'next': raw['next'] ?? '', 'mode': 'local'};
        }
      } catch (_) {}
    }
    try {
      final api = GetIt.instance<ApiClient>();
      final res = await api.get('/source/$sourceId/pages', params: {'comicId': comicId, 'epId': epId});
      final data = res.data;
      if (data is Map) {
        return {'pages': data['pages'] ?? [], 'next': data['next'] ?? '', 'mode': 'server'};
      }
    } catch (_) {}
    return {'pages': [], 'next': '', 'mode': 'none', 'error': '加载失败'};
  }

  String _jsStr(String s) {
    return s.replaceAll('\\', '\\\\').replaceAll('"', '\\"').replaceAll('\n', '\\n');
  }

  void clearCache() {
    _hasLocalJs.clear();
  }
}