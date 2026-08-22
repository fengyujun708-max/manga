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

  /// 解析源 JS 真实下载地址：downloadUrl → jsdelivr 直猜 → 官方 index.json 匹配
  static Future<String> resolveJsUrl(SourceManifest m) async {
    final direct = m.downloadUrl.isNotEmpty ? m.downloadUrl : m.repositoryUrl;
    if (direct.startsWith('http')) return direct;
    // 1) 按约定文件名直猜（venera-configs 的 fileName 基本都是 key 小写）
    final guess = 'https://cdn.jsdelivr.net/gh/venera-app/venera-configs@main/${m.id.toLowerCase()}.js';
    try {
      final r = await _httpGet(guess);
      if (r != null) return guess;
    } catch (_) {}
    // 2) 拉 index.json 按 key/name 匹配（跳过多账号重复版本）
    try {
      final idx = await _httpGet('https://cdn.jsdelivr.net/gh/venera-app/venera-configs@main/index.json');
      if (idx != null) {
        final list = (jsonDecode(idx) as List).cast<Map<String, dynamic>>();
        final wanted = m.id.toLowerCase();
        Map<String, dynamic>? best;
        for (final e in list) {
          final key = (e['key'] ?? '').toString().toLowerCase();
          final name = (e['name'] ?? '').toString();
          final fname = (e['fileName'] ?? '').toString();
          if ((key == wanted || fname.toLowerCase() == '$wanted.js') && best == null) {
            best = e;
          }
        }
        if (best == null) {
          for (final e in list) {
            if ((e['key'] ?? '').toString().toLowerCase() == wanted &&
                !(e['name'] ?? '').toString().contains('多账号')) {
              best = e; break;
            }
          }
        }
        final f = (best?['fileName'] ?? '').toString();
        if (f.isNotEmpty) {
          final u = 'https://cdn.jsdelivr.net/gh/venera-app/venera-configs@main/$f';
          final r = await _httpGet(u);
          if (r != null) return u;
        }
      }
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