import 'dart:convert';
import 'dart:io';
import 'package:flutter/foundation.dart' show debugPrint;
import 'package:flutter/services.dart' show rootBundle;
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'manga_source.dart';
import 'source_data_service.dart';

/// 源安装器：从 APK 资产提取全部源 + 注册到本地 + 预热 QuickJS 引擎
class SourceInstaller {
  static final List<int> _key = List<int>.from('ManjieSourceKey2026'.codeUnits);

  /// XOR + Base64 解密
  static String _decrypt(String encoded) {
    final encrypted = base64.decode(encoded);
    final decrypted = List<int>.generate(encrypted.length, (i) => encrypted[i] ^ _key[i % _key.length]);
    return utf8.decode(decrypted);
  }

  /// 从 APK assets 加载加密的源 JS
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

  /// 首次启动：从 APK 资产提取全部源到本地（无网络需求）
  /// 必须在 runApp() 之前完成（同步等待）
  static Future<int> extractBundledSources() async {
    final dir = await ensureSourceDir();
    if (dir == null) return 0;
    var count = 0;
    final prefs = await SharedPreferences.getInstance();
    final list = (jsonDecode(prefs.getString('installed_sources') ?? '[]') as List).toList();

    for (final id in vettedSources) {
      try {
        final code = await loadBundledSource(id);
        if (code == null) {
          debugPrint('[SourceInstaller] $id: 资源文件不存在');
          continue;
        }
        if (!code.contains('ComicSource')) {
          debugPrint('[SourceInstaller] $id: 不是 ComicSource 格式');
          continue;
        }
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
        debugPrint('[SourceInstaller] $id: 已提取 (${code.length} bytes)');
      } catch (e) {
        debugPrint('[SourceInstaller] $id: 提取失败 - $e');
      }
    }
    await prefs.setString('installed_sources', jsonEncode(list));
    debugPrint('[SourceInstaller] 总计提取 $count 个源，目录: $dir');
    return count;
  }

  /// ★ 关键修复：App 启动后预热所有源到 QuickJS 引擎
  /// 解决"jm 等源加载不了" + "首次进入源详情页超时"
  /// 在首页显示后调用即可（不阻塞 UI）
  static Future<int> preloadAllSourcesToEngine() async {
    final dir = await ensureSourceDir();
    if (dir == null) return 0;
    List<FileSystemEntity> files;
    try {
      files = await Directory(dir).list().toList();
    } catch (_) {
      files = [];
    }
    int loaded = 0;
    int failed = 0;
    for (final f in files) {
      if (f is! File || !f.path.endsWith('.js')) continue;
      final sourceId = f.path.split('/').last.replaceAll('.js', '');
      try {
        final code = await f.readAsString();
        final ok = await SourceDataService.instance.preloadSource(sourceId, code);
        if (ok) loaded++; else failed++;
      } catch (e) {
        failed++;
      }
    }
    debugPrint('[SourceInstaller] 引擎预热完成: 成功=$loaded 失败=$failed');
    return loaded;
  }

  static SourceManifest _buildManifest(String id, String code) {
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
