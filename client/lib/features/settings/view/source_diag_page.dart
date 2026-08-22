import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';
import '../../../plugins/source_data_service.dart';

/// 源引擎分层自诊断：JS 引擎 → 加密桥 → 网络层 → 完整源执行
class SourceDiagPage extends StatefulWidget {
  const SourceDiagPage({super.key});
  @override
  State<SourceDiagPage> createState() => _SourceDiagPageState();
}

class _DiagResult {
  final String name;
  final String detail;
  const _DiagResult(this.name, this.detail);
}

class _SourceDiagPageState extends State<SourceDiagPage> {
  final List<_DiagResult> _results = [];
  bool _running = false;

  Future<void> _run() async {
    setState(() { _running = true; _results.clear(); });
    final svc = SourceDataService.instance;
    final engine = svc.engine;

    void log(String name, Object e) {
      _results.add(_DiagResult(name, e.toString()));
      if (mounted) setState(() {});
    }

    // 1. JS 引擎
    try {
      final r = await engine.evaluate('1+1');
      log('① JS 引擎', r.toString() == '2' ? '✅ 通过 (1+1=2)' : '⚠️ 返回异常: $r');
    } catch (e) {
      log('① JS 引擎', '❌ $e');
    }

    // 2. 加密桥（md5 同步语义）
    try {
      final r = await engine.evaluateAwait("Convert.hexEncode(Convert.md5(Convert.encodeUtf8('abc')))");
      const expect = '900150983cd24fb0d6963f7d28e17f72';
      log('② 加密桥 md5', r.toString() == expect ? '✅ 通过 (md5(abc) 匹配)' : '❌ 得到: $r 期望: $expect');
    } catch (e) {
      log('② 加密桥 md5', '❌ $e');
    }

    // 3. 网络层（XHR/fetch 直连）
    try {
      final r = await engine.evaluateAwait(
        "fetch('https://api.copy2000.online/api/v3/h5/homeIndex', {method:'GET'}).then(r => r.status)");
      log('③ 网络层 fetch', r.toString() == '200' ? '✅ 通过 (HTTP 200)' : '⚠️ 状态码: $r');
    } catch (e) {
      log('③ 网络层 fetch', '❌ $e');
    }

    // 4. 完整链路：copy_manga explore
    try {
      final has = await svc.hasLocalJs('copy_manga');
      if (!has) {
        log('④ copy_manga 全链路', '❌ 本地无 copy_manga.js，请先在市场安装');
      } else {
        final res = await svc.explore('copy_manga');
        if (res['error'] != null) {
          log('④ copy_manga 全链路', '❌ ${res['error']}');
        } else {
          final secs = res['sections'] as List? ?? [];
          log('④ copy_manga 全链路', '✅ ${secs.length} 个板块加载成功');
        }
      }
    } catch (e) {
      log('④ copy_manga 全链路', '❌ $e');
    }

    // 5. 后端连通性
    try {
      final hc = HttpClient();
      final req = await hc.getUrl(Uri.parse('http://39.106.192.137/v1/sources'));
      final resp = await req.close().timeout(const Duration(seconds: 10));
      final body = await resp.transform(utf8.decoder).join();
      hc.close();
      final n = body.contains('"sources"') ? '✅ 可达 (${resp.statusCode})' : '❌ 响应异常';
      log('⑤ 后端 /v1/sources', n);
    } catch (e) {
      log('⑤ 后端 /v1/sources', '❌ $e');
    }

    setState(() => _running = false);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('源引擎诊断')),
      body: Column(children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: FilledButton.icon(
            onPressed: _running ? null : _run,
            icon: _running
                ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                : const Icon(Icons.play_arrow),
            label: Text(_running ? '诊断中…' : '开始诊断'),
          ),
        ),
        Expanded(
          child: ListView.builder(
            itemCount: _results.length,
            itemBuilder: (_, i) => ListTile(
              dense: true,
              title: Text(_results[i].name,
                  style: const TextStyle(fontWeight: FontWeight.bold)),
              subtitle: Text(_results[i].detail),
            ),
          ),
        ),
      ]),
    );
  }
}
