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

  /// 下载源 JS 并保存本地
  static Future<bool> install(SourceManifest manifest, String sourceDir) async {
    try {
      final url = manifest.downloadUrl.isEmpty
          ? manifest.repositoryUrl
          : manifest.downloadUrl;
      if (url.isEmpty) return false;
      final client = HttpClient();
      try {
        final req = await client.getUrl(Uri.parse(url));
        req.headers.set('User-Agent',
            'Mozilla/5.0 (Linux; Android 13) Chrome/120.0 Mobile');
        final resp = await req.close();
        if (resp.statusCode != 200) return false;
        final bytes =
            await resp.fold<List<int>>(<int>[], (a, b) => a..addAll(b));
        final code = utf8.decode(bytes, allowMalformed: true);
        if (!code.contains('ComicSource')) return false;
        final dir = Directory(sourceDir);
        if (!await dir.exists()) await dir.create(recursive: true);
        final file = File('$sourceDir/${manifest.id}.js');
        await file.writeAsString(code);
        return true;
      } finally {
        client.close();
      }
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