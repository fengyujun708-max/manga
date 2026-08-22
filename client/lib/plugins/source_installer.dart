import 'dart:io';
import 'dart:convert';
import 'package:path_provider/path_provider.dart';
import 'manga_source.dart';

/// 源 JS 安装器：下载源 JS 到本地目录
class SourceInstaller {
  /// 确保源存储目录存在
  static Future<String?> ensureSourceDir() async {
    try {
      final docs = await getApplicationDocumentsDirectory();
      final dir = Directory('${docs.path}/sources');
      if (!await dir.exists()) await dir.create(recursive: true);
      return dir.path;
    } catch (_) {
      return null;
    }
  }

  /// 服务器源列表（20 个可用源）
  static const String _serverBase = 'http://39.106.192.137';
  static const List<String> vettedSources = [
    'copy_manga','jm','komiic','comick','manga_dex','baozi','ccc','zaimanhua',
    'manhuagui','manhuaren','manwaba','hot_manga','jcomic','goda','mh18','mxs',
    'nhentai','wnacg','lanraragi','hcomic',
  ];

  /// 优先从自建服务器下载源 JS（国内可达），兜底 jsdelivr
  static Future<String> resolveJsUrl(SourceManifest m) async {
    final id = m.id.toLowerCase();
    // 0) 自建服务器（20 个白名单源）
    final serverUrl = '$_serverBase/sources/$id.js';
    try {
      final r = await _httpGet(serverUrl);
      if (r != null && r.contains('ComicSource')) return serverUrl;
    } catch (_) {}
    // 1) downloadUrl/repositoryUrl 直链
    final direct = m.downloadUrl.isNotEmpty ? m.downloadUrl : m.repositoryUrl;
    if (direct.startsWith('http')) {
      try {
        final r = await _httpGet(direct);
        if (r != null && r.contains('ComicSource')) return direct;
      } catch (_) {}
    }
    // 2) jsdelivr 直猜
    final guess = 'https://cdn.jsdelivr.net/gh/venera-app/venera-configs@main/$id.js';
    try {
      final r = await _httpGet(guess);
      if (r != null && r.contains('ComicSource')) return guess;
    } catch (_) {}
    return '';
  }

  static Future<String?> _httpGet(String url) async {
    try {
      final client = HttpClient()..connectionTimeout = const Duration(seconds: 12);
      try {
        final req = await client.getUrl(Uri.parse(url));
        req.headers.set('User-Agent', 'Mozilla/5.0 (Linux; Android 13) Chrome/120.0 Mobile');
        final resp = await req.close();
        if (resp.statusCode != 200) return null;
        return await resp.transform(utf8.decoder).join();
      } finally {
        client.close();
      }
    } catch (_) {
      return null;
    }
  }

  /// 下载源 JS 并保存本地
  static Future<bool> install(SourceManifest manifest, String sourceDir) async {
    try {
      final url = await resolveJsUrl(manifest);
      if (url.isEmpty) return false;
      final code = await _httpGet(url);
      if (code == null || !code.contains('ComicSource')) return false;
      final dir = Directory(sourceDir);
      if (!await dir.exists()) await dir.create(recursive: true);
      final file = File('$sourceDir/${manifest.id}.js');
      await file.writeAsString(code);
      return true;
    } catch (_) {
      return false;
    }
  }

  /// 检查本地是否有该源的 JS
  static Future<bool> hasJs(String sourceId) async {
    final dir = await ensureSourceDir();
    if (dir == null) return false;
    return File('$dir/$sourceId.js').exists();
  }

  /// 读取本地 JS 代码
  static Future<String?> readJs(String sourceId) async {
    final dir = await ensureSourceDir();
    if (dir == null) return null;
    final file = File('$dir/$sourceId.js');
    if (!await file.exists()) return null;
    return file.readAsString();
  }
}