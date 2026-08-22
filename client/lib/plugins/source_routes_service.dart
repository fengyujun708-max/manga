import 'dart:convert';
import 'dart:io';
import 'package:shared_preferences/shared_preferences.dart';
import 'source_installer.dart';

/// 源线路探测与切换（Venera 式网络检测）
/// - 从源 JS 中提取候选 API 域名
/// - 并发测速（HEAD, 4s 超时）
/// - 选中线路持久化到 SharedPreferences，执行源时注入 loadSetting
class RouteProbe {
  final String host;
  final int? latencyMs; // null = 不可达
  RouteProbe(this.host, this.latencyMs);
  bool get ok => latencyMs != null;
}

class SourceRoutesService {
  SourceRoutesService._();
  static final instance = SourceRoutesService._();

  static const _prefPrefix = 'src_route_';

  /// 从源 JS 提取候选域名
  Future<List<String>> extractHosts(String sourceId) async {
    final file = await _jsFile(sourceId);
    if (file == null || !await file.exists()) return [];
    final code = await file.readAsString();
    final hosts = <String>{};
    final re = RegExp(r'https?://([a-zA-Z0-9.-]+(?::\d+)?)');
    for (final m in re.allMatches(code)) {
      final h = m.group(1)!;
      if (h.contains('jsdelivr') || h.contains('githubusercontent') || h.contains('w3.org')) continue;
      hosts.add(h);
    }
    return hosts.toList();
  }

  Future<File?> _jsFile(String sourceId) async {
    try {
      final dir = await SourceInstaller.ensureSourceDir();
      if (dir == null) return null;
      final f = File('$dir/$sourceId.js');
      return await f.exists() ? f : null;
    } catch (_) {
      return null;
    }
  }

  /// 并发测速所有候选域名
  Future<List<RouteProbe>> probe(List<String> hosts) async {
    final results = await Future.wait(hosts.map(_probeOne));
    results.sort((a, b) {
      if (a.ok && b.ok) return a.latencyMs!.compareTo(b.latencyMs!);
      if (a.ok) return -1;
      if (b.ok) return 1;
      return 0;
    });
    return results;
  }

  Future<RouteProbe> _probeOne(String host) async {
    final sw = Stopwatch()..start();
    try {
      final client = HttpClient()..connectionTimeout = const Duration(seconds: 4);
      client.badCertificateCallback = (_, __, ___) => true;
      try {
        final req = await client.headUrl(Uri.parse('https://$host')).timeout(const Duration(seconds: 5));
        await req.close().timeout(const Duration(seconds: 5));
        client.close();
        return RouteProbe(host, sw.elapsedMilliseconds);
      } on HttpException {
        // HEAD 可能不被支持，降级 GET /
        final req = await client.getUrl(Uri.parse('https://$host')).timeout(const Duration(seconds: 4));
        await req.close().timeout(const Duration(seconds: 4));
        client.close();
        return RouteProbe(host, sw.elapsedMilliseconds);
      }
    } catch (_) {
      return RouteProbe(host, null);
    }
  }

  /// 保存选中线路（覆盖注入 domains 字段；值为主机名）
  Future<void> selectRoute(String sourceId, String settingKey, String value) async {
    final sp = await SharedPreferences.getInstance();
    final cur = getOverrides(sourceId);
    cur[settingKey] = value;
    await sp.setString('$_prefPrefix$sourceId', jsonEncode(cur));
  }

  Future<void> setOverride(String sourceId, String settingKey, dynamic value) => selectRoute(sourceId, settingKey, value);

  /// 自动选择延迟最低的可用线路
  Future<void> autoSelect(String sourceId) async {
    final hosts = await extractHosts(sourceId);
    if (hosts.isEmpty) return;
    final probes = await probe(hosts);
    final ok = probes.where((p) => p.ok).toList()
      ..sort((a, b) => a.latencyMs!.compareTo(b.latencyMs!));
    if (ok.isNotEmpty) await selectRoute(sourceId, 'domains', ok.first.host);
  }

  Map<String, dynamic> getOverrides(String sourceId) {
    // 同步读取不可行（SharedPreferences 异步），缓存于内存
    return _cache[sourceId] ?? {};
  }

  final Map<String, Map<String, dynamic>> _cache = {};

  Future<void> warm() async {
    final sp = await SharedPreferences.getInstance();
    for (final k in sp.getKeys()) {
      if (k.startsWith(_prefPrefix)) {
        try {
          _cache[k.substring(_prefPrefix.length)] = jsonDecode(sp.getString(k)!) as Map<String, dynamic>;
        } catch (_) {}
      }
    }
  }
}
