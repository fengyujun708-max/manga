import 'dart:convert';
import 'dart:io';
import 'package:flutter/services.dart' show rootBundle;
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'manga_source.dart';

/// 源安装器：内置源自动提取 + 版本检测 + 服务器更新下载
class SourceInstaller {
  static const String serverBase = 'http://39.106.192.137';

  /// 内置源清单（id → 文件名）
  static const List<String> vettedSources = [
    'copy_manga','jm','komiic','comick','manga_dex','baozi','ccc','zaimanhua',
    'manhuagui','manhuaren','manwaba','hot_manga','jcomic','goda','mh18','mxs',
    'nhentai','wnacg','lanraragi','hcomic',
  ];

  static Map<String, SourceManifest>? _bundledManifests;

  /// 首次启动：从 APK 资产提取全部源到本地（无网络需求）
  static Future<int> extractBundledSources() async {
    final dir = await ensureSourceDir();
    if (dir == null) return 0;
    var count = 0;
    final prefs = await SharedPreferences.getInstance();
    final list = (jsonDecode(prefs.getString('installed_sources') ?? '[]') as List).toList();

    for (final id in vettedSources) {
      try {
        final code = await rootBundle.loadString('assets/sources/$id.js');
        if (!code.contains('ComicSource')) continue;
        final file = File('$dir/$id.js');
        await file.writeAsString(code);
        // 注册 manifest
        if (!list.any((e) {
          try { return (SourceManifest.fromJson(e as Map<String, dynamic>).id.toLowerCase() == id); } catch (_) { return false; }
        })) {
          final m = _buildManifest(id, code);
          list.add(m.toJson());
        }
        count++;
      } catch (_) {}
    }
    await prefs.setString('installed_sources', jsonEncode(list));
    return count;
  }

  static SourceManifest _buildManifest(String id, String code) {
    // 从源 JS 中提取 name 和 version
    final nameMatch = RegExp(r'name\s*=\s*"([^"]+)"').firstMatch(code);
    final verMatch = RegExp(r'version\s*=\s*"([^"]+)"').firstMatch(code);
    return SourceManifest(
      id: id,
      name: nameMatch?.group(1) ?? id,
      version: verMatch?.group(1) ?? '1.0.0',
      author: '', description: '', icon: '',
      repositoryUrl: '', downloadUrl: '', minAppVersion: '',
      capabilities: const [], downloads: 0, rating: 0,
      networkType: '',
    );
  }

  /// 检查服务器上是否有更新版本 → 返回需要更新的源列表
  static Future<List<SourceUpdate>> checkUpdates() async {
    final updates = <SourceUpdate>[];
    try {
      final client = HttpClient()..connectionTimeout = const Duration(seconds: 8);
      final req = await client.getUrl(Uri.parse('$serverBase/v1/sources'));
      final resp = await req.close().timeout(const Duration(seconds: 10));
      if (resp.statusCode != 200) { client.close(); return updates; }
      final body = await resp.transform(utf8.decoder).join();
      client.close();
      final data = jsonDecode(body);
      final sources = data is Map ? (data['sources'] as List? ?? []) : [];
      final prefs = await SharedPreferences.getInstance();
      final localList = jsonDecode(prefs.getString('installed_sources') ?? '[]') as List;
      final localVersions = <String, String>{};
      for (final e in localList) {
        try {
          final m = SourceManifest.fromJson(e as Map<String, dynamic>);
          localVersions[m.id.toLowerCase()] = m.version ?? '';
        } catch (_) {}
      }
      for (final s in sources) {
        if (s is! Map) continue;
        final sid = (s['id'] ?? '').toString().toLowerCase();
        final sver = (s['version'] ?? '').toString();
        final local = localVersions[sid];
        if (local == null || local.isEmpty) continue;
        if (_compareVersions(sver, local) > 0) {
          updates.add(SourceUpdate(id: sid, name: (s['name'] ?? sid).toString(), currentVersion: local, newVersion: sver));
        }
      }
    } catch (_) {}
    return updates;
  }

  /// 从服务器下载并覆盖指定源的 JS
  static Future<bool> updateSource(String sourceId) async {
    try {
      final url = '$serverBase/sources/${sourceId.toLowerCase()}.js';
      final code = await _httpGet(url);
      if (code == null || !code.contains('ComicSource')) return false;
      final dir = await ensureSourceDir();
      if (dir == null) return false;
      final file = File('$dir/${sourceId.toLowerCase()}.js');
      await file.writeAsString(code);
      // 更新本地版本号
      final prefs = await SharedPreferences.getInstance();
      final list = (jsonDecode(prefs.getString('installed_sources') ?? '[]') as List).toList();
      for (var i = 0; i < list.length; i++) {
        try {
          final m = SourceManifest.fromJson(list[i] as Map<String, dynamic>);
          if (m.id.toLowerCase() == sourceId.toLowerCase()) {
            final verMatch = RegExp(r'version\s*=\s*"([^"]+)"').firstMatch(code);
            final updated = SourceManifest(
              id: m.id, name: m.name, version: verMatch?.group(1) ?? m.version,
              author: m.author, description: m.description, icon: m.icon,
              repositoryUrl: m.repositoryUrl, downloadUrl: m.downloadUrl,
              minAppVersion: m.minAppVersion, capabilities: m.capabilities,
              downloads: m.downloads, rating: m.rating, networkType: m.networkType,
            );
            list[i] = updated.toJson();
            break;
          }
        } catch (_) {}
      }
      await prefs.setString('installed_sources', jsonEncode(list));
      return true;
    } catch (_) { return false; }
  }

  /// 批量更新所有有新版本的源
  static Future<int> updateAll(List<SourceUpdate> updates) async {
    var count = 0;
    for (final u in updates) {
      if (await updateSource(u.id)) count++;
    }
    return count;
  }

  static int _compareVersions(String a, String b) {
    final pa = a.split('.').map((e) => int.tryParse(e) ?? 0).toList();
    final pb = b.split('.').map((e) => int.tryParse(e) ?? 0).toList();
    final len = pa.length > pb.length ? pa.length : pb.length;
    for (var i = 0; i < len; i++) {
      final va = i < pa.length ? pa[i] : 0;
      final vb = i < pb.length ? pb[i] : 0;
      if (va > vb) return 1;
      if (va < vb) return -1;
    }
    return 0;
  }

  static Future<String?> ensureSourceDir() async {
    try {
      final docs = await getApplicationDocumentsDirectory();
      final dir = Directory('${docs.path}/sources');
      if (!await dir.exists()) await dir.create(recursive: true);
      return dir.path;
    } catch (_) { return null; }
  }

  static Future<String?> _httpGet(String url) async {
    try {
      final client = HttpClient()..connectionTimeout = const Duration(seconds: 10);
      final req = await client.getUrl(Uri.parse(url));
      final resp = await req.close().timeout(const Duration(seconds: 15));
      if (resp.statusCode != 200) { client.close(); return null; }
      final body = await resp.transform(utf8.decoder).join();
      client.close();
      return body;
    } catch (_) { return null; }
  }
  /// 安装单个源（下载 JS 并保存到本地）
  static Future<bool> install(SourceManifest manifest, String sourceDir) async {
    try {
      final id = manifest.id.toLowerCase();
      final code = await _httpGet('$serverBase/sources/$id.js');
      if (code == null || !code.contains('ComicSource')) return false;
      final dir = Directory(sourceDir);
      if (!await dir.exists()) await dir.create(recursive: true);
      await File('$sourceDir/$id.js').writeAsString(code);
      return true;
    } catch (_) { return false; }
  }
}

class SourceUpdate {
  final String id;
  final String name;
  final String currentVersion;
  final String newVersion;
  SourceUpdate({required this.id, required this.name, required this.currentVersion, required this.newVersion});
}