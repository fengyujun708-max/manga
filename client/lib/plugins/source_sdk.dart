import 'dart:convert';
import 'source_data_service.dart';

/// ManjieSourceSDK v2 — Venera 完整 API Surface 的 Dart 封装
class SourceSDK {
  static SourceDataService get _svc => SourceDataService.instance;

  /// 探测源能力
  static Future<List<String>> features(String sourceId) async {
    if (!await _svc.loadLocal(sourceId)) return [];
    try {
      final r = await _svc.engine.evaluateAwait('__features__("$sourceId")');
      if (r is Map && r['features'] is List) return (r['features'] as List).cast<String>();
    } catch (_) {}
    return [];
  }

  // ===== Account =====
  static Future<Map<String, dynamic>> login(String sourceId, String account, String password) =>
      _accountAction(sourceId, 'login', {'account': account, 'password': password});

  static Future<Map<String, dynamic>> logout(String sourceId) =>
      _accountAction(sourceId, 'logout', {});

  static Future<String?> registerUrl(String sourceId) async {
    final r = await _accountAction(sourceId, 'registerWebsite', {});
    return r['url'] as String?;
  }

  static Future<bool> hasAccount(String sourceId) async {
    return (await features(sourceId)).contains('account');
  }

  static Future<Map<String, dynamic>> _accountAction(String sourceId, String action, Map<String, dynamic> params) async {
    if (!await _svc.loadLocal(sourceId)) return {'error': 'not loaded'};
    try {
      final p = jsonEncode(params).replaceAll("'", "\\'");
      final r = await _svc.engine.evaluateAwait("__account__('$sourceId', '$action', JSON.parse('$p'))");
      if (r is Map) return Map<String, dynamic>.from(r);
    } catch (_) {}
    return {'error': 'failed'};
  }

  // ===== Favorites =====
  static Future<List<dynamic>> favoriteFolders(String sourceId) async {
    if (!await _svc.loadLocal(sourceId)) return [];
    try {
      final r = await _svc.engine.evaluateAwait('__favorites__("$sourceId", "folders", {})');
      if (r is Map) return (r['folders'] as List?) ?? [];
    } catch (_) {}
    return [];
  }

  static Future<Map<String, dynamic>> favoriteList(String sourceId, {int page = 1, String folder = ''}) async {
    if (!await _svc.loadLocal(sourceId)) return {'items': []};
    try {
      final folderJson = jsonEncode(folder).replaceAll("'", "\\'");
      final r = await _svc.engine.evaluateAwait('__favorites__("$sourceId", "list", {"page": $page, "folder": $folderJson})');
      if (r is Map) return Map<String, dynamic>.from(r);
    } catch (_) {}
    return {'items': []};
  }

  // ===== Comments =====
  static Future<List<dynamic>> comments(String sourceId, String comicId, {String epId = '', int page = 1}) async {
    if (!await _svc.loadLocal(sourceId)) return [];
    try {
      final cid = comicId.replaceAll("'", "\\'");
      final eid = epId.replaceAll("'", "\\'");
      final r = await _svc.engine.evaluateAwait('__comments__("$sourceId", "$cid", "$eid", $page)');
      if (r is Map) return (r['comments'] as List?) ?? [];
    } catch (_) {}
    return [];
  }

  // ===== Settings =====
  static Future<List<Map<String, dynamic>>> settings(String sourceId) async {
    if (!await _svc.loadLocal(sourceId)) return [];
    try {
      final r = await _svc.engine.evaluateAwait('__settings__("$sourceId")');
      if (r is Map && r['settings'] is List) {
        return (r['settings'] as List).map((s) => Map<String, dynamic>.from(s as Map)).toList();
      }
    } catch (_) {}
    return [];
  }

  // ===== onImageLoad — 获取图片请求头 =====
  static Future<Map<String, String>> imageHeaders(String sourceId, String url, {String comicId = '', String epId = ''}) async {
    try {
      if (!await _svc.hasLocalJs(sourceId)) return {};
      final u = url.replaceAll("'", "\\'");
      final cid = comicId.replaceAll("'", "\\'");
      final eid = epId.replaceAll("'", "\\'");
      final r = await _svc.engine.evaluate('__onImageLoad__("$sourceId", "$u", "$cid", "$eid")');
      if (r is Map && r['headers'] is Map) {
        return (r['headers'] as Map).map((k, v) => MapEntry(k.toString(), v.toString()));
      }
    } catch (_) {}
    return {};
  }

  // ===== Download =====
  static Future<List<String>> downloadImages(String sourceId, String comicId, String epId) async {
    if (!await _svc.loadLocal(sourceId)) return [];
    try {
      final cid = comicId.replaceAll("'", "\\'");
      final eid = epId.replaceAll("'", "\\'");
      final r = await _svc.engine.evaluateAwait('__downloadInfo__("$sourceId", "$cid", "$eid")');
      if (r is Map && r['images'] is List) return (r['images'] as List).cast<String>();
    } catch (_) {}
    return [];
  }
}