import 'dart:async';
import 'dart:io';
import 'source_installer.dart';
import 'runtime/venera_engine.dart';

/// 源数据服务 —— 纯本地执行（Venera 模式）
/// 源 JS 在设备上运行，网络由手机直连源站（用户开 VPN 即可访问海外源）
/// 无服务器中转
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

  /// 加载本地源并执行
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

  /// 获取首页板块
  Future<Map<String, dynamic>> explore(String sourceId) async {
    if (await loadLocal(sourceId)) {
      try {
        final raw = await engine.evaluateAwait('globalThis.__exploreAll__("$sourceId")');
        if (raw is Map && raw['error'] != null) {
          return {'sections': [], 'mode': 'local', 'error': raw['error'].toString()};
        } else if (raw is List && raw.isNotEmpty) {
          return {'sections': raw, 'mode': 'local'};
        }
        return {'sections': [], 'mode': 'local', 'error': '板块为空，请检查网络或开 VPN 后重试'};
      } catch (e) {
        return {'sections': [], 'mode': 'local', 'error': e.toString()};
      }
    }
    return {'sections': [], 'mode': 'none', 'error': '未安装源脚本，请到源市场重新安装'};
  }

  /// 搜索
  Future<Map<String, dynamic>> search(String sourceId, String keyword, int page) async {
    if (await loadLocal(sourceId)) {
      try {
        final raw = await engine.evaluateAwait('globalThis.__search__("$sourceId", "${_jsStr(keyword)}", $page)');
        if (raw is Map && raw['error'] == null) {
          return {'items': raw['items'] ?? [], 'hasMore': raw['hasMore'] == true, 'mode': 'local'};
        }
        if (raw is Map) {
          return {'items': [], 'hasMore': false, 'mode': 'local', 'error': raw['error']?.toString()};
        }
      } catch (e) {
        return {'items': [], 'hasMore': false, 'mode': 'local', 'error': e.toString()};
      }
    }
    return {'items': [], 'hasMore': false, 'mode': 'none', 'error': '未安装源脚本'};
  }

  /// 分类列表
  Future<List<Map<String, dynamic>>> categories(String sourceId) async {
    if (await loadLocal(sourceId)) {
      try {
        final raw = await engine.evaluateAwait('globalThis.__categories__("$sourceId")');
        if (raw is List && raw.isNotEmpty) {
          return (raw as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
        }
      } catch (_) {}
    }
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
        if (raw is Map) {
          return {'items': [], 'hasMore': false, 'mode': 'local', 'error': raw['error']?.toString()};
        }
      } catch (e) {
        return {'items': [], 'hasMore': false, 'mode': 'local', 'error': e.toString()};
      }
    }
    return {'items': [], 'hasMore': false, 'mode': 'none', 'error': '未安装源脚本'};
  }

  /// 详情 + 章节
  Future<Map<String, dynamic>> comic(String sourceId, String comicId) async {
    if (await loadLocal(sourceId)) {
      try {
        final raw = await engine.evaluateAwait('globalThis.__comic__("$sourceId", "${_jsStr(comicId)}")');
        if (raw is Map) return {'detail': raw, 'chapters': raw['chapters'] ?? [], 'mode': 'local'};
      } catch (_) {}
    }
    return {'detail': {}, 'chapters': [], 'mode': 'none', 'error': '加载失败：未安装源脚本'};
  }

  /// 图片页
  Future<Map<String, dynamic>> pages(String sourceId, String comicId, String epId) async {
    if (await loadLocal(sourceId)) {
      try {
        final raw = await engine.evaluateAwait('globalThis.__pages__("$sourceId", "${_jsStr(comicId)}", "${_jsStr(epId)}")');
        if (raw is Map && raw['pages'] != null) {
          return {'pages': raw['pages'], 'next': raw['next'] ?? '', 'mode': 'local'};
        }
      } catch (_) {}
    }
    return {'pages': [], 'next': '', 'mode': 'none', 'error': '加载失败'};
  }

  String _jsStr(String s) {
    return s.replaceAll('\\', '\\\\').replaceAll('"', '\\"').replaceAll('\n', '\\n');
  }

  void clearCache() {
    _hasLocalJs.clear();
  }
}
