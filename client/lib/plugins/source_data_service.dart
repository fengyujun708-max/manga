import 'dart:async';
import 'dart:io';
import 'package:flutter/material.dart' show debugPrint;
import 'source_installer.dart';
import 'source_routes_service.dart';
import '../venera/foundation/js_engine.dart';
import '../core/network/log_reporter.dart';

/// 源数据服务 —— 纯本地执行（Venera 模式）
/// 源 JS 在设备上运行，网络由手机直连源站（用户开 VPN 即可访问海外源）
/// 无服务器中转
class SourceDataService {
  static SourceDataService? _instance;
  static SourceDataService get instance => _instance ??= SourceDataService();

  final JsEngine engine = JsEngine();
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
      var file = File('$dir/$sourceId.js');
      if (!await file.exists()) {
        final lower = File('$dir/${sourceId.toLowerCase()}.js');
        if (!await lower.exists()) return false;
        file = lower;
      }
      final code = await file.readAsString();
      final err = await engine.executeSource(sourceId, code, settings: SourceRoutesService.instance.getOverrides(sourceId))
          .timeout(const Duration(seconds: 15), onTimeout: () { throw Exception('源加载超时'); });
      if (err != null) {
        debugPrint('[SourceLoad] $sourceId failed: $err');
        LogReporter.instance.reportSourceError(sourceId, err);
        return false;
      }
      // 缓存源 baseUrl（供封面相对路径修复）
      try {
        final u = await engine.evaluateAwait('globalThis.__sources__["$sourceId"].url', timeoutMs: 3000);
        final us = u?.toString() ?? '';
        if (us.startsWith('http')) _sourceBaseUrls[sourceId] = us;
      } catch (_) {}
      return true;
    } catch (e) {
      LogReporter.instance.reportSourceError(sourceId, e.toString());
      return false;
    }
  }

  /// 深度规范化：把 flutter_qjs 返回的 JS 包装类型转成纯 Dart Map/List/基础类型
  static dynamic _deepNormalize(dynamic v) {
    if (v == null) return null;
    if (v is Map) {
      // 显式构造 Map<String, dynamic>，避免 Map<dynamic,dynamic> 强转崩溃
      final out = <String, dynamic>{};
      v.forEach((k, val) {
        out[k.toString()] = _deepNormalize(val);
      });
      return out;
    }
    if (v is List) {
      return v.map(_deepNormalize).toList();
    }
    if (v is num || v is bool || v is String) return v;
    return v.toString();
  }

  /// 获取首页板块
  Future<Map<String, dynamic>> explore(String sourceId) async {
    if (await loadLocal(sourceId)) {
      try {
        final raw = await engine.evaluateAwait('globalThis.__exploreAll__("$sourceId")').timeout(const Duration(seconds: 20), onTimeout: () => throw Exception('探索超时（网络不通或源站被墙，请开 VPN）'));
        if (raw is Map && raw['error'] != null) {
          LogReporter.instance.report('error', 'explore失败[$sourceId]', raw['error'].toString());
          return {'sections': [], 'mode': 'local', 'error': raw['error'].toString()};
        } else if (raw is List && raw.isNotEmpty) {
          // 深度规范化，确保 UI 渲染时 as Map/as List 不崩溃
          final normalized = _deepNormalize(raw) as List;
          // 统一修复封面相对路径
          for (final s in normalized) {
            if (s is Map) {
              final items = s['items'];
              if (items is List) {
                s['items'] = items.map((e) => e is Map ? _fixItemCover(sourceId, Map<String, dynamic>.from(e)) : e).toList();
              }
            }
          }
          return {'sections': normalized, 'mode': 'local'};
        }
        final msg = raw is Map && raw['error'] != null ? raw['error'].toString() : '板块为空: ${raw.toString().substring(0, raw.toString().length.clamp(0, 200))}';
        LogReporter.instance.report('error', 'explore空[$sourceId]', msg);
        return {'sections': [], 'mode': 'local', 'error': '板块为空，请检查网络或开VPN后重试'};
      } catch (e) {
        LogReporter.instance.report('error', 'explore异常[$sourceId]', e.toString());
        return {'sections': [], 'mode': 'local', 'error': e.toString()};
      }
    }
    return {'sections': [], 'mode': 'none', 'error': '未安装源脚本，请到源市场重新安装'};
  }

  /// 搜索
  Future<Map<String, dynamic>> search(String sourceId, String keyword, int page) async {
    if (await loadLocal(sourceId)) {
      try {
        final raw = await engine.evaluateAwait('globalThis.__search__("$sourceId", "${_jsStr(keyword)}", $page)').timeout(const Duration(seconds: 20), onTimeout: () => throw Exception('搜索超时（源站响应慢或被墙）'));
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
        final raw = await engine.evaluateAwait('globalThis.__categories__("$sourceId")').timeout(const Duration(seconds: 20), onTimeout: () => throw Exception('分类加载超时'));
        if (raw is List && raw.isNotEmpty) {
          return (raw as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
        }
      } catch (_) {}
    }
    return [];
  }

  /// 修复 cover 相对路径（//xx、/xx），baseUrl 从源 JS 的 url 属性缓存
  final Map<String, String> _sourceBaseUrls = {};

  String? baseUrl(String sourceId) => _sourceBaseUrls[sourceId];

  /// 获取图片 Referer（优先用源 baseUrl，兜底用图片域名）
  static String getReferer(String imageUrl, [String? sourceBaseUrl]) {
    if (sourceBaseUrl != null && sourceBaseUrl.startsWith('http')) {
      return sourceBaseUrl.endsWith('/') ? sourceBaseUrl : '$sourceBaseUrl/';
    }
    final host = Uri.tryParse(imageUrl)?.host;
    return host != null ? 'https://$host/' : '';
  }

  String _fixImageUrl(String sourceId, String image) {
    if (image.isEmpty || image.startsWith('data:')) return image;
    if (image.startsWith('//')) return 'https:$image';
    if (image.startsWith('/')) {
      final base = baseUrl(sourceId);
      if (base != null && base.isNotEmpty) {
        return base.endsWith('/') ? base.substring(0, base.length - 1) + image : base + image;
      }
    }
    return image;
  }

  String _fixCover(String sourceId, String cover) {
    if (cover.isEmpty) return cover;
    if (cover.startsWith('//')) return 'https:$cover';
    if (cover.startsWith('/')) {
      final base = baseUrl(sourceId);
      if (base != null && base.isNotEmpty) {
        return base.endsWith('/') ? base.substring(0, base.length - 1) + cover : base + cover;
      }
    }
    return cover;
  }

  Map<String, dynamic> _fixItemCover(String sourceId, Map<String, dynamic> m) {
    final cover = (m['cover'] ?? m['coverUrl'] ?? '').toString();
    if (cover.isNotEmpty) {
      final fixed = _fixCover(sourceId, cover);
      m['cover'] = fixed;
      m['coverUrl'] = fixed;
    }
    return m;
  }

  /// 分类漫画（分页）
  Future<Map<String, dynamic>> categoryComics(String sourceId, String name, int page,
      [String param = '', List<String> options = const [], bool ranking = false]) async {
    if (await loadLocal(sourceId)) {
      try {
        final raw = await engine.evaluateAwait(
            'globalThis.__categoryComics__("$sourceId", "${_jsStr(name)}", "${_jsStr(param)}", ${options.map(_jsStr).toList()}, $page)');
        if (raw is Map && raw['error'] == null) {
          final normalized = _deepNormalize({'items': raw['items'] ?? [], 'hasMore': raw['hasMore'] == true}) as Map;
          final items = (normalized['items'] as List).map((e) => _fixItemCover(sourceId, Map<String, dynamic>.from(e as Map))).toList();
          return {'items': items, 'hasMore': normalized['hasMore'] == true, 'mode': 'local'};
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
        final raw = await engine.evaluateAwait('globalThis.__comic__("$sourceId", "${_jsStr(comicId)}")')
            .timeout(const Duration(seconds: 8), onTimeout: () => throw Exception('连接超时，请检查网络或 VPN 后重试'));
        if (raw is Map) {
          final normalized = _deepNormalize(raw) as Map;
          final err = normalized['error']?.toString();
          if (err != null && err.isNotEmpty) {
            LogReporter.instance.report('error', '详情失败[$sourceId]', '$comicId: $err');
            return {'detail': {}, 'chapters': [], 'mode': 'local', 'error': err};
          }
          // 封面修复 + 章节数日志
          if (normalized['detail'] == null) {
            final cover = (normalized['cover'] ?? '').toString();
            if (cover.isNotEmpty) {
              _fixItemCover(sourceId, Map<String, dynamic>.from(normalized));
            }
            final chs = normalized['chapters'];
            LogReporter.instance.report('info', '详情数据[$sourceId]', '$comicId: chapters=${chs is List ? chs.length : chs?.runtimeType}, keys=${normalized.keys.take(12).join(",")}');
          }
          return {'detail': normalized, 'chapters': normalized['chapters'] ?? [], 'mode': 'local'};
        }
        LogReporter.instance.report('error', '详情空[$sourceId]', '$comicId raw=${raw.toString().substring(0, raw.toString().length.clamp(0, 200))}');
        return {'detail': {}, 'chapters': [], 'mode': 'local', 'error': '详情返回为空'};
      } catch (e) {
        LogReporter.instance.report('error', '详情异常[$sourceId]', '$comicId: ${e.toString()}');
        return {'detail': <String, dynamic>{}, 'chapters': <dynamic>[], 'mode': 'local', 'error': '详情加载失败：${e.toString().replaceAll("Exception: ", "")}'};
      }
    }
    return {'detail': <String, dynamic>{}, 'chapters': <dynamic>[], 'mode': 'none', 'error': '未安装源脚本，请到源市场重新安装'};
  }

  /// 图片页
  Future<Map<String, dynamic>> pages(String sourceId, String comicId, String epId) async {
    if (await loadLocal(sourceId)) {
      try {
        final raw = await engine.evaluateAwait('globalThis.__pages__("$sourceId", "${_jsStr(comicId)}", "${_jsStr(epId)}")')
            .timeout(const Duration(seconds: 20), onTimeout: () => throw Exception('图片加载超时'));
        if (raw is Map && raw['error'] != null) {
          final message = raw['error'].toString();
          LogReporter.instance.report('error', '正文接口失败[$sourceId]', '$comicId/$epId: $message');
          return {'pages': [], 'next': '', 'mode': 'local', 'error': message};
        }
        if (raw is Map && raw['pages'] != null) {
          final normalized = _deepNormalize(raw['pages']);
          final pageList = normalized is List ? normalized : <dynamic>[];
          // 统一提取图片 URL（兼容 String / {url} / {image} / {cover}）
          final images = pageList.map((e) {
            if (e is String) return e;
            if (e is Map) {
              return (e['url'] ?? e['image'] ?? e['cover'] ?? e['imageUrl'] ?? e['src'] ?? '').toString();
            }
            return '';
          }).where((e) => e.isNotEmpty).map((e) => _fixImageUrl(sourceId, e)).toList();
          return {'pages': images, 'next': raw['next'] ?? '', 'mode': 'local'};
        }
      } catch (e) {
        LogReporter.instance.report('error', '正文失败[$sourceId]', '$comicId/$epId: $e');
        return {'pages': [], 'next': '', 'mode': 'local', 'error': '正文加载失败：${e.toString().replaceAll("Exception: ", "")}' };
      }
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
