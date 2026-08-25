import 'dart:convert';
import 'dart:io';
import 'package:flutter/services.dart' show rootBundle;
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'manga_source.dart';

/// 源安装器：仅负责 APK 内置源提取和本地存储
class SourceInstaller {
  static final List<int> _key = List<int>.from('ManjieSourceKey2026'.codeUnits);

  /// 解密源 JS（XOR + Base64）
  static String _decrypt(String encoded) {
    final encrypted = base64.decode(encoded);
    final decrypted = List<int>.generate(encrypted.length, (i) => encrypted[i] ^ _key[i % _key.length]);
    return utf8.decode(decrypted);
  }

  /// 加载加密的内置源代码
  static Future<String?> loadBundledSource(String id) async {
    try {
      final raw = await rootBundle.loadString('assets/sources/${id.toLowerCase()}.js.enc');
      return _decrypt(raw);
    } catch (_) { return null; }
  }

  /// 内置源清单（id → 文件名）
  static const List<String> vettedSources = [
    'copy_manga','jm','komiic','comick','manga_dex','baozi','ccc','zaimanhua',
    'manhuagui','manhuaren','manwaba','hot_manga','jcomic','goda','mh18','mxs',
    'nhentai','wnacg','lanraragi','hcomic',
    'picacg','comic_walker','shonen_jump_plus','hitomi','ykmh','ikmmh',
    'ehentai','mh1234','kavita','happy','komga','mycomic',
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
        final code = await loadBundledSource(id);
        if (code == null || !code.contains('ComicSource')) continue;
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



  static Future<String?> ensureSourceDir() async {
    try {
      final docs = await getApplicationDocumentsDirectory();
      final dir = Directory('${docs.path}/sources');
      if (!await dir.exists()) await dir.create(recursive: true);
      return dir.path;
    } catch (_) { return null; }
  }

}
