import 'dart:convert';
import 'package:crypto/crypto.dart';
import 'package:flutter/services.dart' show rootBundle;
import 'package:flutter_js/flutter_js.dart';

/// 客户端 Venera 源执行引擎
/// flutter_js(QuickJS) 本地执行 Venera 源 JS
/// 网络：flutter_js 内置 fetch(XHR→Dart http)，直连手机（用户开 VPN 可访问海外源）
/// 加密：通过 sendMessage 桥接 Dart crypto（用全局变量传递结果）
class VeneraEngine {
  static VeneraEngine? _instance;
  static VeneraEngine get instance => _instance ??= VeneraEngine();

  JavascriptRuntime? _runtime;
  bool _prepared = false;

  Future<void> init() async {
    if (_prepared) return;
    _runtime = getJavascriptRuntime(xhr: true, extraArgs: const {'stackSize': 1024 * 1024 * 2});

    // 加密桥接：sendMessage 是 void，用全局变量 __cryptoResult 传递结果
    _runtime!.setupBridge('crypto', (dynamic args) {
      final m = args as Map;
      final op = m['op']?.toString() ?? '';
      final data = m['data']?.toString() ?? '';
      String result = '';
      switch (op) {
        case 'md5': result = md5.convert(utf8.encode(data)).toString(); break;
        case 'sha1': result = sha1.convert(utf8.encode(data)).toString(); break;
        case 'sha256': result = sha256.convert(utf8.encode(data)).toString(); break;
        case 'sha512': result = sha512.convert(utf8.encode(data)).toString(); break;
        case 'hmac':
          final key = m['key']?.toString() ?? '';
          final msg = m['msg']?.toString() ?? '';
          final algo = m['algo']?.toString() ?? 'sha256';
          result = Hmac(_hashAlgo(algo), utf8.encode(key)).convert(utf8.encode(msg)).toString();
          break;
        case 'b64encode': result = base64.encode(utf8.encode(data)); break;
        case 'b64decode':
          try { result = utf8.decode(base64.decode(data), allowMalformed: true); } catch (_) { result = ''; }
          break;
        default: result = '';
      }
      // 把结果存到 JS 全局变量（同步可读）
      _runtime!.evaluate('globalThis.__cryptoResult = ${jsonEncode(result)};');
    });

    // 加载运行时基座 JS
    final runtimeJs = await rootBundle.loadString('assets/venera_client_runtime.js');
    _runtime!.evaluate(runtimeJs);
    _prepared = true;
  }

  /// 执行源 JS（每次重置全局，保证隔离）
  Future<bool> executeSource(String sourceId, String jsCode) async {
    await init();
    _runtime!.evaluate('''
      try { delete globalThis.__sourceClass; } catch(_) {}
      try { delete globalThis.__sources__; } catch(_) {}
    ''');
    // 通过 __executeSource__ 工厂执行（含实例化 + init）
    final esc = jsCode.replaceAll('\\', '\\\\').replaceAll("'", "\\'").replaceAll('\n', '\\n');
    _runtime!.evaluate("globalThis.__executeSource__('" + esc + "', '" + sourceId + "');");
    // 泵送 Promise 直到源注册
    for (var i = 0; i < 300; i++) {
      _runtime!.executePendingJob();
      final r = _runtime!.evaluate('globalThis.__sources__');
      final m = r.rawResult;
      if (m is Map && m[sourceId] != null) return true;
      await Future.delayed(const Duration(milliseconds: 30));
    }
    return false;
  }

  /// 求值 JS 并等待 Promise 完成
  Future<dynamic> evaluateAwait(String js) async {
    _runtime!.evaluate('''
globalThis.__evalResult__ = null;
globalThis.__evalDone__ = false;
(async () => {
  try {
    const r = await $js;
    globalThis.__evalResult__ = r;
  } catch(e) {
    globalThis.__evalResult__ = { __error: String(e && e.message ? e.message : e) };
  }
  globalThis.__evalDone__ = true;
})();
''');
    for (var i = 0; i < 600; i++) {
      _runtime!.executePendingJob();
      final done = _runtime!.evaluate('globalThis.__evalDone__').rawResult;
      if (done == true) break;
      await Future.delayed(const Duration(milliseconds: 30));
    }
    final r = _runtime!.evaluate('globalThis.__evalResult__');
    final val = r.rawResult;
    if (val is Map && val['__error'] != null) {
      throw Exception(val['__error']);
    }
    return val;
  }

  /// 同步求值
  dynamic evaluate(String js) {
    return _runtime!.evaluate(js).rawResult;
  }

  void dispose() {
    _runtime?.dispose();
    _runtime = null;
    _prepared = false;
  }

  Hash _hashAlgo(String algo) {
    switch (algo.toLowerCase()) {
      case 'md5': return md5;
      case 'sha1': return sha1;
      case 'sha256': return sha256;
      case 'sha512': return sha512;
      default: return sha256;
    }
  }
}