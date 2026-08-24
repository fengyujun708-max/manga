import 'dart:async';
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
  final Set<String> _loadedSources = {};
  bool _executing = false;
  final List<Completer<void>> _waitQueue = [];

  Future<void> init() async {
    if (_prepared) return;
    _runtime = getJavascriptRuntime(xhr: true, extraArgs: const {'stackSize': 1024 * 1024 * 2});

    // 加密桥接：flutter_js 宿主会把回调返回值同步传回 JS；
    // 签名是 void Function，用 dynamic 中转变量绕过静态类型
    dynamic Function(dynamic) cryptoBridge = (dynamic args) {
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
      // 结果同时走两条通道：返回值（新桥）+ 全局变量（旧桥兜底）
      _runtime!.evaluate('globalThis.__cryptoResult = ${jsonEncode(result)};');
      return result;
    };
    _runtime!.setupBridge('crypto', cryptoBridge);

    // 加载运行时基座 JS
    final runtimeJs = await rootBundle.loadString('assets/venera_client_runtime.js');
    _runtime!.evaluate(runtimeJs);
    _prepared = true;
  }

  /// 执行源 JS（每次重置全局，保证隔离）
  /// 返回 null=成功；否则返回错误信息
  Future<String?> executeSource(String sourceId, String jsCode, {Map<String, dynamic>? settings}) async {
    await init();
    // 已加载且无设置变化 → 直接复用，避免重复 eval（修复"点几次才加载"）
    if (_loadedSources.contains(sourceId) && (settings == null || settings.isEmpty)) {
      return null;
    }
    // 锁：防止并发 loadLocal 互相干扰（修复首次点击不加载）
    while (_executing) {
      final c = Completer<void>();
      _waitQueue.add(c);
      await c.future;
    }
    _executing = true;
    try {
      // 注入宿主侧设置覆盖（线路选择等）
      if (settings != null && settings.isNotEmpty) {
        final s = jsonEncode(settings).replaceAll("'", "\\'");
        _runtime!.evaluate("globalThis.__settingsOverride__ = JSON.parse('" + s.replaceAll('\n', r'\n') + "');");
      } else {
        _runtime!.evaluate("globalThis.__settingsOverride__ = {};");
      }
      _runtime!.evaluate('''
      try { delete globalThis.__sourceClass; } catch(_) {}
      try { delete globalThis.__sourceLoadError__; } catch(_) {}
    ''');
      // base64 注入，避免字符串转义损坏源代码
      final b64 = base64.encode(utf8.encode(jsCode));
      _runtime!.evaluate("globalThis.__executeSourceB64__('$b64', '$sourceId');");
      // 泵送 Promise 直到源注册
      for (var i = 0; i < 300; i++) {
        _runtime!.executePendingJob();
        final r = _runtime!.evaluate('JSON.stringify(Object.keys(globalThis.__sources__ || {}))');
        if (r.rawResult.toString().contains('"$sourceId"')) {
          _loadedSources.add(sourceId);
          return null;
        }
        await Future.delayed(const Duration(milliseconds: 30));
      }
      // 超时未注册
      final err = _runtime!.evaluate('globalThis.__sourceLoadError__');
      final msg = err.rawResult.toString();
      return (msg == 'null' || msg.isEmpty) ? '源加载超时' : msg;
    } finally {
      _executing = false;
      for (final w in _waitQueue) { w.complete(); }
      _waitQueue.clear();
    }
  }

  /// 求值 JS 并等待 Promise 完成
  Future<dynamic> evaluateAwait(String js, {int timeoutMs = 15000}) async {
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
    // 快速轮询，用 JSON.stringify 避免类型比较问题
    final maxIter = (timeoutMs ~/ 10);
    for (var i = 0; i < maxIter; i++) {
      _runtime!.executePendingJob();
      final done = _runtime!.evaluate('JSON.stringify(globalThis.__evalDone__)').rawResult.toString();
      if (done == 'true') break;
      await Future.delayed(const Duration(milliseconds: 10));
    }
    final done = _runtime!.evaluate('JSON.stringify(globalThis.__evalDone__)').rawResult.toString();
    if (done != 'true') {
      _runtime!.evaluate('globalThis.__evalDone__ = true; globalThis.__evalResult__ = { __error: "请求超时（源站无响应或被墙）" };');
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
    _loadedSources.clear();
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