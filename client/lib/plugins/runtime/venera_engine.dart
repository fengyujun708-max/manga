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
      final Map m = args is Map ? args : (() {
        try { return jsonDecode(args.toString()) as Map; } catch (_) { return <String, dynamic>{}; }
      })();
      final op = m['op']?.toString() ?? '';
      final data = m['data']?.toString() ?? '';
      // Venera 桥以字节为界；这里用 latin1(codeUnits) 保真二进制串
      List<int> bin(String s) => List<int>.generate(s.length, (i) => s.codeUnitAt(i) & 0xff);
      String result = '';
      switch (op) {
        // Venera 语义：哈希返回原始二进制(latin1)串，由源自行 hexEncode
        case 'md5': result = String.fromCharCodes(md5.convert(bin(data)).bytes); break;
        case 'sha1': result = String.fromCharCodes(sha1.convert(bin(data)).bytes); break;
        case 'sha256': result = String.fromCharCodes(sha256.convert(bin(data)).bytes); break;
        case 'sha512': result = String.fromCharCodes(sha512.convert(bin(data)).bytes); break;
        case 'hmac':
          final key = m['key']?.toString() ?? '';
          final msg = m['msg']?.toString() ?? '';
          final algo = m['algo']?.toString() ?? 'sha256';
          result = String.fromCharCodes(Hmac(_hashAlgo(algo), bin(key)).convert(bin(msg)).bytes);
          break;
        case 'hex': result = _toHex(data); break;
        case 'b64encode': {
          final bytes = List<int>.generate(data.length, (i) => data.codeUnitAt(i) & 0xff);
          result = base64.encode(bytes);
          break;
        }
        case 'b64decode':
          try { result = String.fromCharCodes(base64.decode(data)); } catch (_) { result = ''; }
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
  Future<bool> executeSource(String sourceId, String jsCode, {Map<String, dynamic>? settings}) async {
    await init();
    // 注入宿主侧设置覆盖（线路选择等）
    if (settings != null && settings.isNotEmpty) {
      final s = jsonEncode(settings).replaceAll("'", "\\'");
      _runtime!.evaluate("globalThis.__settingsOverride__ = JSON.parse('" + s.replaceAll('\n', r'\n') + "');");
    } else {
      _runtime!.evaluate("globalThis.__settingsOverride__ = {};");
    }
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

  String _toHex(String latin1) => latin1.codeUnits.map((c) => c.toRadixString(16).padLeft(2, '0')).join();

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