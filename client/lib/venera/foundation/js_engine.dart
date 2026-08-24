import 'dart:convert';
import 'dart:io';
import 'dart:math' as math;
import 'package:crypto/crypto.dart';
import 'package:dio/io.dart';
import 'package:enough_convert/enough_convert.dart';
import 'package:flutter/foundation.dart' show protected;
import 'package:flutter/services.dart';
import 'package:html/parser.dart' as html;
import 'package:html/dom.dart' as dom;
import 'package:flutter_qjs/flutter_qjs.dart';
import 'package:pointycastle/api.dart';
import 'package:pointycastle/asn1/asn1_parser.dart';
import 'package:pointycastle/asn1/primitives/asn1_integer.dart';
import 'package:pointycastle/asn1/primitives/asn1_sequence.dart';
import 'package:pointycastle/asymmetric/api.dart';
import 'package:pointycastle/asymmetric/pkcs1.dart';
import 'package:pointycastle/asymmetric/rsa.dart';
import 'package:pointycastle/block/aes.dart';
import 'package:pointycastle/block/modes/cbc.dart';
import 'package:pointycastle/block/modes/cfb.dart';
import 'package:pointycastle/block/modes/ecb.dart';
import 'package:pointycastle/block/modes/ofb.dart';
import 'package:uuid/uuid.dart';
import 'package:manjie/venera/components/js_ui.dart';
import 'package:manjie/venera/foundation/app.dart';
import 'package:manjie/venera/foundation/js_pool.dart';
import 'package:manjie/core/network/log_reporter.dart';
import 'package:manjie/venera/network/app_dio.dart';
import 'package:manjie/venera/network/cookie_jar.dart';
import 'package:manjie/venera/network/proxy.dart';
import 'package:manjie/venera/utils/init.dart';

import 'comic_source/comic_source.dart';
import 'consts.dart';
import 'log.dart';

class JavaScriptRuntimeException implements Exception {
  final String message;

  JavaScriptRuntimeException(this.message);

  @override
  String toString() {
    return "JSException: $message";
  }
}

class JsEngine with _JSEngineApi, JsUiApi, Init {
  factory JsEngine() => _cache ?? (_cache = JsEngine._create());

  static JsEngine? _cache;

  JsEngine._create();

  FlutterQjs? _engine;

  bool _closed = true;

  Dio? _dio;

  final Set<String> _loadedSources = {};

  // 执行锁：evaluateAwait 共享全局变量，必须串行（并发会互相覆盖数据）
  bool _busy = false;
  final List<Completer<void>> _waitQueue = [];

  Future<T> _serialized<T>(Future<T> Function() fn) async {
    while (_busy) {
      final c = Completer<void>();
      _waitQueue.add(c);
      await c.future;
    }
    _busy = true;
    try {
      return await fn();
    } finally {
      _busy = false;
      for (final w in _waitQueue) {
        w.complete();
      }
      _waitQueue.clear();
    }
  }

  static void reset() {
    _cache = null;
    _cache?.dispose();
    JsEngine().init();
  }

  void resetDio() {
    _dio = AppDio(BaseOptions(
        responseType: ResponseType.plain, validateStatus: (status) => true));
  }

  static Uint8List? _jsInitCache;

  static void cacheJsInit(Uint8List jsInit) {
    _jsInitCache = jsInit;
  }

  @override
  @protected
  Future<void> doInit() async {
    if (!_closed) {
      return;
    }
    try {
      if (App.isInitialized) {
        _cookieJar ??= await SingleInstanceCookieJar.createInstance();
      }
      _dio ??= AppDio(BaseOptions(
          responseType: ResponseType.plain, validateStatus: (status) => true));
      _closed = false;
      _engine = FlutterQjs();
      _engine!.dispatch();
      var setGlobalFunc =
          _engine!.evaluate("(key, value) => { this[key] = value; }");
      (setGlobalFunc as JSInvokable)(["sendMessage", _messageReceiver]);
      setGlobalFunc(["appVersion", App.version]);
      setGlobalFunc.free();
      Uint8List jsInit;
      var buffer = await rootBundle.load("assets/venera_client_runtime.js");
      jsInit = buffer.buffer.asUint8List();
      _engine!
          .evaluate(utf8.decode(jsInit), name: "<init>");
      // 验证 runtime 加载成功（Convert 是对象，检查非 undefined 即可）
      final check = _engine!.evaluate('typeof globalThis.Convert');
      if (check.toString() == 'undefined') {
        Log.error('JS Engine', 'Runtime 加载异常: Convert=${check}');
        try {
          LogReporter.instance.report('error', 'JsEngine Runtime 加载失败', 'Convert=${check}');
        } catch (_) {}
      }
    } catch (e, s) {
      Log.error('JS Engine', 'JS Engine Init Error:\n$e\n$s');
      try {
        LogReporter.instance.report('error', 'JsEngine Init Error', '$e\n$s');
      } catch (_) {}
    }
  }

  Object? _messageReceiver(dynamic message) {
    try {
      if (message is Map<dynamic, dynamic>) {
        if (message["method"] == null) return null;
        String method = message["method"] as String;
        switch (method) {
          case "log":
            String level = message["level"];
            Log.addLog(
                switch (level) {
                  "error" => LogLevel.error,
                  "warning" => LogLevel.warning,
                  "info" => LogLevel.info,
                  _ => LogLevel.warning
                },
                message["title"],
                message["content"].toString());
          case 'load_data':
            String key = message["key"];
            String dataKey = message["data_key"];
            return ComicSource.find(key)?.data[dataKey];
          case 'save_data':
            String key = message["key"];
            String dataKey = message["data_key"];
            if (dataKey == 'setting') {
              throw "setting is not allowed to be saved";
            }
            var data = message["data"];
            var source = ComicSource.find(key)!;
            source.data[dataKey] = data;
            source.saveData();
          case 'delete_data':
            String key = message["key"];
            String dataKey = message["data_key"];
            var source = ComicSource.find(key);
            source?.data.remove(dataKey);
            source?.saveData();
          case 'http':
            return _http(Map.from(message));
          case 'crypto':
            return _crypto(Map.from(message));
          case 'html':
            return handleHtmlCallback(Map.from(message));
          case 'convert':
            return _convert(Map.from(message));
          case "random":
            return _random(
              message["min"] ?? 0,
              message["max"] ?? 1,
              message["type"],
            );
          case "cookie":
            return handleCookieCallback(Map.from(message));
          case "uuid":
            return const Uuid().v1();
          case "load_setting":
            String key = message["key"];
            String settingKey = message["setting_key"];
            var source = ComicSource.find(key)!;
            return source.data["settings"]?[settingKey] ??
                source.settings?[settingKey]!['default'] ??
                (throw "Setting not found: $settingKey");
          case "isLogged":
            return ComicSource.find(message["key"])!.isLogged;
          // temporary solution for [setTimeout] function
          // TODO: implement [setTimeout] in quickjs project
          case "delay":
            return Future.delayed(Duration(milliseconds: message["time"]));
          case "UI":
            return handleUIMessage(Map.from(message));
          case "getLocale":
            return "${App.locale.languageCode}_${App.locale.countryCode}";
          case "getPlatform":
            return Platform.operatingSystem;
          case "setClipboard":
            return Clipboard.setData(ClipboardData(text: message["text"]));
          case "getClipboard":
            return Future.sync(() async {
              var res = await Clipboard.getData(Clipboard.kTextPlain);
              return res?.text;
            });
          case "compute":
            final func = message["function"];
            final args = message["args"];
            if (func is JSInvokable) {
              func.free();
              throw "Function must be a string";
            }
            if (func is! String) {
              throw "Function must be a string";
            }
            if (args != null && args is! List) {
              throw "Args must be a list";
            }
            return JSPool().execute(func, args ?? []);
          case "image":
            return _image(Map.from(message));
        }
      }
      return null;
    } catch (e, s) {
      Log.error("Failed to handle message: $message\n$e\n$s", "JsEngine");
      rethrow;
    }
  }

  Future<Map<String, dynamic>> _http(Map<String, dynamic> req) async {
    Response? response;
    String? error;

    try {
      var headers = Map<String, dynamic>.from(req["headers"] ?? {});
      var extra = Map<String, dynamic>.from(req["extra"] ?? {});
      if (headers["user-agent"] == null && headers["User-Agent"] == null) {
        headers["User-Agent"] = webUA;
      }
      var dio = _dio;
      if (headers['http_client'] == "dart:io") {
        dio = Dio(BaseOptions(
          responseType: ResponseType.plain,
          validateStatus: (status) => true,
        ));
        var proxy = await getProxy();
        dio.httpClientAdapter = IOHttpClientAdapter(
          createHttpClient: () {
            return HttpClient()
              ..findProxy = (uri) => proxy == null ? "DIRECT" : "PROXY $proxy";
          },
        );
        dio.interceptors
            .add(CookieManagerSql(SingleInstanceCookieJar.instance!));
        dio.interceptors.add(LogInterceptor());
      }
      response = await dio!.request(req["url"],
          data: req["data"],
          options: Options(
              method: req['http_method'],
              responseType: req["bytes"] == true
                  ? ResponseType.bytes
                  : ResponseType.plain,
              headers: headers,
              extra: extra,
          )
      );
    } catch (e) {
      error = e.toString();
    }

    Map<String, String> headers = {};

    response?.headers
        .forEach((name, values) => headers[name] = values.join(','));

    dynamic body = response?.data;
    if (body is! Uint8List && body is List<int>) {
      body = Uint8List.fromList(body);
    }

    return {
      "status": response?.statusCode,
      "headers": headers,
      "body": body,
      "error": error,
    };
  }

  dynamic runCode(String js, [String? name]) {
    return _engine!.evaluate(js, name: name);
  }

  /// 同步求值（别名，兼容旧代码）
  dynamic evaluate(String js) {
    return _engine!.evaluate(js);
  }

  /// 执行源 JS，注册到 __sources__
  Future<String?> executeSource(String sourceId, String jsCode, {Map<String, dynamic>? settings}) {
    return _serialized(() async {
      await init();
      // 已加载且无设置变化 → 直接复用（修复重复 eval 和首次点击不加载）
      if (_loadedSources.contains(sourceId) && (settings == null || settings.isEmpty)) {
        return null;
      }
      if (settings != null && settings.isNotEmpty) {
      final s = jsonEncode(settings);
      _engine!.evaluate("globalThis.__settingsOverride__ = JSON.parse('${s.replaceAll("'", "\\'").replaceAll('\n', r'\n')}');");
    } else {
      _engine!.evaluate("globalThis.__settingsOverride__ = {};");
    }
    _engine!.evaluate('try { delete globalThis.__sourceClass; } catch(_) {} try { delete globalThis.__sourceLoadError__; } catch(_) {}');
    final b64 = base64.encode(utf8.encode(jsCode));
    _engine!.evaluate("globalThis.__executeSourceB64__('${b64}', '$sourceId');");
    // flutter_qjs 的 dispatch() 自动处理 Promise，等待源注册
    for (var i = 0; i < 500; i++) {
      final r = _engine!.evaluate('JSON.stringify(Object.keys(globalThis.__sources__ || {}))');
      if (r.toString().contains('"$sourceId"')) {
        _loadedSources.add(sourceId);
        return null;
      }
      await Future.delayed(const Duration(milliseconds: 20));
    }
    final err = _engine!.evaluate('globalThis.__sourceLoadError__');
    final msg = err.toString();
    return (msg == 'null' || msg.isEmpty) ? '源加载超时' : msg;
  }

  /// 求值 JS 并等待 Promise 完成（flutter_qjs dispatch 自动处理事件循环）
  Future<dynamic> evaluateAwait(String js, {int timeoutMs = 20000}) {
    return _serialized(() async {
      _engine!.evaluate('''
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
      final maxIter = timeoutMs ~/ 10;
      for (var i = 0; i < maxIter; i++) {
        final done = _engine!.evaluate('JSON.stringify(globalThis.__evalDone__)').toString();
        if (done == 'true') break;
        await Future.delayed(const Duration(milliseconds: 10));
      }
      final r = _engine!.evaluate('globalThis.__evalResult__');
      if (r is Map && r['__error'] != null) {
        throw Exception(r['__error']);
      }
      return r;
    });
  }

  void dispose() {
    _cache = null;
    _closed = true;
    _loadedSources.clear();
    _engine?.close();
    _engine?.port.close();
  }
}

mixin class _JSEngineApi {
  CookieJarSql? _cookieJar;

  final _documents = <int, DocumentWrapper>{};

  Object? handleHtmlCallback(Map<String, dynamic> data) {
    switch (data["function"]) {
      case "parse":
        if (_documents.length > 8) {
          var shouldDelete = _documents.keys.first;
          Log.warning(
            "JS Engine",
            "Too many documents, deleting the oldest: $shouldDelete\n"
                "Current documents: ${_documents.keys}",
          );
          _documents.remove(shouldDelete);
        }
        _documents[data["key"]] = DocumentWrapper.parse(data["data"]);
        return null;
      case "querySelector":
        var key = data["key"];
        return _documents[key]!.querySelector(data["query"]);
      case "querySelectorAll":
        var key = data["key"];
        return _documents[key]!.querySelectorAll(data["query"]);
      case "getText":
        return _documents[data["doc"]]!.elementGetText(data["key"]);
      case "getAttributes":
        var res = _documents[data["doc"]]!.elementGetAttributes(data["key"]);
        return res;
      case "dom_querySelector":
        var doc = _documents[data["doc"]]!;
        return doc.elementQuerySelector(data["key"], data["query"]);
      case "dom_querySelectorAll":
        var doc = _documents[data["doc"]]!;
        return doc.elementQuerySelectorAll(data["key"], data["query"]);
      case "getChildren":
        var doc = _documents[data["doc"]]!;
        return doc.elementGetChildren(data["key"]);
      case "getNodes":
        var doc = _documents[data["doc"]]!;
        return doc.elementGetNodes(data["key"]);
      case "getInnerHTML":
        var doc = _documents[data["doc"]]!;
        return doc.elementGetInnerHTML(data["key"]);
      case "getParent":
        var doc = _documents[data["doc"]]!;
        return doc.elementGetParent(data["key"]);
      case "node_text":
        return _documents[data["doc"]]!.nodeGetText(data["key"]);
      case "node_type":
        return _documents[data["doc"]]!.nodeType(data["key"]);
      case "node_to_element":
        return _documents[data["doc"]]!.nodeToElement(data["key"]);
      case "dispose":
        var docKey = data["key"];
        _documents.remove(docKey);
        return null;
      case "getClassNames":
        return _documents[data["doc"]]!.getClassNames(data["key"]);
      case "getId":
        return _documents[data["doc"]]!.getId(data["key"]);
      case "getLocalName":
        return _documents[data["doc"]]!.getLocalName(data["key"]);
      case "getElementById":
        return _documents[data["key"]]!.getElementById(data["id"]);
      case "getPreviousSibling":
        return _documents[data["doc"]]!.getPreviousSibling(data["key"]);
      case "getNextSibling":
        return _documents[data["doc"]]!.getNextSibling(data["key"]);
    }
    return null;
  }

  dynamic handleCookieCallback(Map<String, dynamic> data) {
    switch (data["function"]) {
      case "set":
        _cookieJar!.saveFromResponse(
            Uri.parse(data["url"]),
            (data["cookies"] as List).map((e) {
              var c = Cookie(e["name"], e["value"]);
              if (e['domain'] != null) {
                c.domain = e['domain'];
              }
              return c;
            }).toList());
        return null;
      case "get":
        var cookies = _cookieJar!.loadForRequest(Uri.parse(data["url"]));
        return cookies
            .map((e) => {
                  "name": e.name,
                  "value": e.value,
                  "domain": e.domain,
                  "path": e.path,
                  "expires": e.expires,
                  "max-age": e.maxAge,
                  "secure": e.secure,
                  "httpOnly": e.httpOnly,
                  "session": e.expires == null,
                })
            .toList();
      case "delete":
        clearCookies([data["url"]]);
        return null;
    }
  }

  void clearCookies(List<String> domains) async {
    for (var domain in domains) {
      var uri = Uri.tryParse(domain);
      if (uri == null) continue;
      _cookieJar!.deleteUri(uri);
    }
  }

  /// crypto 桥：md5/sha/hmac/base64（Venera 语义：哈希返回 latin1 二进制串）
  dynamic _crypto(Map<String, dynamic> data) {
    final op = data['op']?.toString() ?? '';
    List<int> bin(String s) => List<int>.generate(s.length, (i) => s.codeUnitAt(i) & 0xff);
    String result = '';
    final dataStr = data['data']?.toString() ?? '';
    switch (op) {
      case 'md5': result = String.fromCharCodes(md5.convert(bin(dataStr)).bytes); break;
      case 'sha1': result = String.fromCharCodes(sha1.convert(bin(dataStr)).bytes); break;
      case 'sha256': result = String.fromCharCodes(sha256.convert(bin(dataStr)).bytes); break;
      case 'sha512': result = String.fromCharCodes(sha512.convert(bin(dataStr)).bytes); break;
      case 'hmac':
        final key = data['key']?.toString() ?? '';
        final msg = data['msg']?.toString() ?? '';
        final algo = data['algo']?.toString() ?? 'sha256';
        result = String.fromCharCodes(Hmac(switch (algo) {
          'md5' => md5, 'sha1' => sha1, 'sha256' => sha256, 'sha512' => sha512,
          _ => sha256,
        }, bin(key)).convert(bin(msg)).bytes);
        break;
      case 'b64encode': {
        final bytes = bin(dataStr);
        result = base64Encode(bytes);
        break;
      }
      case 'b64decode':
        try { result = String.fromCharCodes(base64Decode(dataStr)); } catch (_) { result = ''; }
        break;
      default: result = '';
    }
    return result;
  }

  Object? _convert(Map<String, dynamic> data) {
    String type = data["type"];
    var value = data["value"];
    bool isEncode = data["isEncode"];
    try {
      switch (type) {
        case "utf8":
          return isEncode ? utf8.encode(value) : utf8.decode(value);
        case "gbk":
          final codec = const GbkCodec();
          return isEncode
              ? Uint8List.fromList(codec.encode(value))
              : codec.decode(value);
        case "base64":
          return isEncode ? base64Encode(value) : base64Decode(value);
        case "md5":
          return Uint8List.fromList(md5.convert(value).bytes);
        case "sha1":
          return Uint8List.fromList(sha1.convert(value).bytes);
        case "sha256":
          return Uint8List.fromList(sha256.convert(value).bytes);
        case "sha512":
          return Uint8List.fromList(sha512.convert(value).bytes);
        case "hmac":
          var key = data["key"];
          var hash = data["hash"];
          var hmac = Hmac(
              switch (hash) {
                "md5" => md5,
                "sha1" => sha1,
                "sha256" => sha256,
                "sha512" => sha512,
                _ => throw "Unsupported hash: $hash"
              },
              key);
          if (data['isString'] == true) {
            return hmac.convert(value).toString();
          } else {
            return Uint8List.fromList(hmac.convert(value).bytes);
          }
        case "aes-ecb":
          var key = data["key"];
          var cipher = ECBBlockCipher(AESEngine());
          cipher.init(
            isEncode,
            KeyParameter(key),
          );
          var offset = 0;
          var result = Uint8List(value.length);
          while (offset < value.length) {
            offset += cipher.processBlock(
              value,
              offset,
              result,
              offset,
            );
          }
          return result;
        case "aes-cbc":
          var key = data["key"];
          var iv = data["iv"];
          var cipher = CBCBlockCipher(AESEngine());
          cipher.init(isEncode, ParametersWithIV(KeyParameter(key), iv));
          var offset = 0;
          var result = Uint8List(value.length);
          while (offset < value.length) {
            offset += cipher.processBlock(
              value,
              offset,
              result,
              offset,
            );
          }
          return result;
        case "aes-cfb":
          var key = data["key"];
          var iv = data["iv"];
          var blockSize = data["blockSize"];
          var cipher = CFBBlockCipher(AESEngine(), blockSize);
          cipher.init(isEncode, ParametersWithIV(KeyParameter(key), iv));
          var offset = 0;
          var result = Uint8List(value.length);
          while (offset < value.length) {
            offset += cipher.processBlock(
              value,
              offset,
              result,
              offset,
            );
          }
          return result;
        case "aes-ofb":
          var key = data["key"];
          var blockSize = data["blockSize"];
          var cipher = OFBBlockCipher(AESEngine(), blockSize);
          cipher.init(isEncode, KeyParameter(key));
          var offset = 0;
          var result = Uint8List(value.length);
          while (offset < value.length) {
            offset += cipher.processBlock(
              value,
              offset,
              result,
              offset,
            );
          }
          return result;
        case "rsa":
          if (!isEncode) {
            var key = data["key"];
            final cipher = PKCS1Encoding(RSAEngine());
            cipher.init(false,
                PrivateKeyParameter<RSAPrivateKey>(_parsePrivateKey(key)));
            return _processInBlocks(cipher, value);
          }
          return null;
        default:
          return value;
      }
    } catch (e, s) {
      Log.error("JS Engine", "Failed to convert $type: $e", s);
      return null;
    }
  }

  RSAPrivateKey _parsePrivateKey(String privateKeyString) {
    List<int> privateKeyDER = base64Decode(privateKeyString);
    var asn1Parser = ASN1Parser(privateKeyDER as Uint8List);
    final topLevelSeq = asn1Parser.nextObject() as ASN1Sequence;
    final privateKey = topLevelSeq.elements![2];

    asn1Parser = ASN1Parser(privateKey.valueBytes!);
    final pkSeq = asn1Parser.nextObject() as ASN1Sequence;

    final modulus = pkSeq.elements![1] as ASN1Integer;
    final privateExponent = pkSeq.elements![3] as ASN1Integer;
    final p = pkSeq.elements![4] as ASN1Integer;
    final q = pkSeq.elements![5] as ASN1Integer;

    return RSAPrivateKey(
        modulus.integer!, privateExponent.integer!, p.integer!, q.integer!);
  }

  Uint8List _processInBlocks(AsymmetricBlockCipher engine, Uint8List input) {
    final numBlocks = input.length ~/ engine.inputBlockSize +
        ((input.length % engine.inputBlockSize != 0) ? 1 : 0);

    final output = Uint8List(numBlocks * engine.outputBlockSize);

    var inputOffset = 0;
    var outputOffset = 0;
    while (inputOffset < input.length) {
      final chunkSize = (inputOffset + engine.inputBlockSize <= input.length)
          ? engine.inputBlockSize
          : input.length - inputOffset;

      outputOffset += engine.processBlock(
          input, inputOffset, chunkSize, output, outputOffset);

      inputOffset += chunkSize;
    }

    return (output.length == outputOffset)
        ? output
        : output.sublist(0, outputOffset);
  }

  num _random(num min, num max, String type) {
    if (type == "double") {
      return min + (max - min) * math.Random().nextDouble();
    }
    return (min + (max - min) * math.Random().nextDouble()).toInt();
  }

  // 官方引擎 Image 桥：RGBA 像素缓冲池（copyRange/rotate90/fillImageAt 等）
  final Map<int, Uint8List> _imageBuffers = {};
  final Map<int, (int, int)> _imageSizes = {};
  int _imageKeyCounter = 0;

  Object? _image(Map<String, dynamic> data) {
    final f = data["function"] as String?;
    switch (f) {
      case "emptyImage":
        final w = (data["width"] as num).toInt();
        final h = (data["height"] as num).toInt();
        final key = ++_imageKeyCounter;
        _imageBuffers[key] = Uint8List(w * h * 4); // 透明黑
        _imageSizes[key] = (w, h);
        return key;
      case "getWidth":
        return _imageSizes[data["key"]]?.$1;
      case "getHeight":
        return _imageSizes[data["key"]]?.$2;
      case "copyRange":
        final key = data["key"];
        final src = _imageBuffers[key];
        final size = _imageSizes[key];
        if (src == null || size == null) return null;
        final (sw, sh) = size;
        final x = (data["x"] as num).toInt();
        final y = (data["y"] as num).toInt();
        final w = (data["width"] as num).toInt();
        final h = (data["height"] as num).toInt();
        final dst = Uint8List(w * h * 4);
        for (var row = 0; row < h; row++) {
          final sr = y + row;
          if (sr < 0 || sr >= sh) continue;
          for (var col = 0; col < w; col++) {
            final sc = x + col;
            if (sc < 0 || sc >= sw) continue;
            final si = (sr * sw + sc) * 4;
            final di = (row * w + col) * 4;
            dst[di] = src[si];
            dst[di + 1] = src[si + 1];
            dst[di + 2] = src[si + 2];
            dst[di + 3] = src[si + 3];
          }
        }
        final nk = ++_imageKeyCounter;
        _imageBuffers[nk] = dst;
        _imageSizes[nk] = (w, h);
        return nk;
      case "copyAndRotate90":
        final key = data["key"];
        final src = _imageBuffers[key];
        final size = _imageSizes[key];
        if (src == null || size == null) return null;
        final (w, h) = size;
        final nw = h, nh = w;
        final dst = Uint8List(nw * nh * 4);
        for (var y = 0; y < h; y++) {
          for (var x = 0; x < w; x++) {
            final si = (y * w + x) * 4;
            final dx = h - 1 - y;
            final dy = x;
            final di = (dy * nw + dx) * 4;
            dst[di] = src[si];
            dst[di + 1] = src[si + 1];
            dst[di + 2] = src[si + 2];
            dst[di + 3] = src[si + 3];
          }
        }
        final nk = ++_imageKeyCounter;
        _imageBuffers[nk] = dst;
        _imageSizes[nk] = (nw, nh);
        return nk;
      case "fillImageAt":
      case "fillImageRangeAt":
        final key = data["key"];
        final dst = _imageBuffers[key];
        final dsize = _imageSizes[key];
        if (dst == null || dsize == null) return null;
        final (dw, dh) = dsize;
        final imgKey = data["image"];
        final src = _imageBuffers[imgKey];
        final ssize = _imageSizes[imgKey];
        if (src == null || ssize == null) return null;
        final (sw, sh) = ssize;
        final dx = (data["x"] as num).toInt();
        final dy = (data["y"] as num).toInt();
        final sx = f == "fillImageRangeAt"
            ? (data["srcX"] as num).toInt()
            : 0;
        final sy = f == "fillImageRangeAt"
            ? (data["srcY"] as num).toInt()
            : 0;
        final fw = f == "fillImageRangeAt"
            ? (data["width"] as num).toInt()
            : sw;
        final fh = f == "fillImageRangeAt"
            ? (data["height"] as num).toInt()
            : sh;
        for (var row = 0; row < fh; row++) {
          final dr = dy + row, sr = sy + row;
          if (dr < 0 || dr >= dh || sr < 0 || sr >= sh) continue;
          for (var col = 0; col < fw; col++) {
            final dc = dx + col, sc = sx + col;
            if (dc < 0 || dc >= dw || sc < 0 || sc >= sw) continue;
            final si = (sr * sw + sc) * 4;
            final di = (dr * dw + dc) * 4;
            dst[di] = src[si];
            dst[di + 1] = src[si + 1];
            dst[di + 2] = src[si + 2];
            dst[di + 3] = src[si + 3];
          }
        }
        return null;
    }
    return null;
  }
}

class DocumentWrapper {
  final dom.Document doc;

  DocumentWrapper.parse(String doc) : doc = html.parse(doc);

  var elements = <dom.Element>[];

  var nodes = <dom.Node>[];

  int? querySelector(String query) {
    var element = doc.querySelector(query);
    if (element == null) return null;
    elements.add(element);
    return elements.length - 1;
  }

  List<int> querySelectorAll(String query) {
    var res = doc.querySelectorAll(query);
    var keys = <int>[];
    for (var element in res) {
      elements.add(element);
      keys.add(elements.length - 1);
    }
    return keys;
  }

  String? elementGetText(int key) {
    return elements[key].text;
  }

  Map<String, String> elementGetAttributes(int key) {
    return elements[key].attributes.map(
          (key, value) => MapEntry(
            key.toString(),
            value,
          ),
        );
  }

  String? elementGetInnerHTML(int key) {
    return elements[key].innerHtml;
  }

  int? elementGetParent(int key) {
    var res = elements[key].parent;
    if (res == null) return null;
    elements.add(res);
    return elements.length - 1;
  }

  int? elementQuerySelector(int key, String query) {
    var res = elements[key].querySelector(query);
    if (res == null) return null;
    elements.add(res);
    return elements.length - 1;
  }

  List<int> elementQuerySelectorAll(int key, String query) {
    var res = elements[key].querySelectorAll(query);
    var keys = <int>[];
    for (var element in res) {
      elements.add(element);
      keys.add(elements.length - 1);
    }
    return keys;
  }

  List<int> elementGetChildren(int key) {
    var res = elements[key].children;
    var keys = <int>[];
    for (var element in res) {
      elements.add(element);
      keys.add(elements.length - 1);
    }
    return keys;
  }

  List<int> elementGetNodes(int key) {
    var res = elements[key].nodes;
    var keys = <int>[];
    for (var node in res) {
      nodes.add(node);
      keys.add(nodes.length - 1);
    }
    return keys;
  }

  String? nodeGetText(int key) {
    return nodes[key].text;
  }

  String nodeType(int key) {
    return switch (nodes[key].nodeType) {
      dom.Node.ELEMENT_NODE => "element",
      dom.Node.TEXT_NODE => "text",
      dom.Node.COMMENT_NODE => "comment",
      dom.Node.DOCUMENT_NODE => "document",
      _ => "unknown"
    };
  }

  int? nodeToElement(int key) {
    if (nodes[key] is dom.Element) {
      elements.add(nodes[key] as dom.Element);
      return elements.length - 1;
    }
    return null;
  }

  List<String> getClassNames(int key) {
    return (elements[key]).classes.toList();
  }

  String? getId(int key) {
    return (elements[key]).id;
  }

  String? getLocalName(int key) {
    return (elements[key]).localName;
  }

  int? getElementById(String id) {
    var element = doc.getElementById(id);
    if (element == null) return null;
    elements.add(element);
    return elements.length - 1;
  }

  int? getPreviousSibling(int key) {
    var res = elements[key].previousElementSibling;
    if (res == null) return null;
    elements.add(res);
    return elements.length - 1;
  }

  int? getNextSibling(int key) {
    var res = elements[key].nextElementSibling;
    if (res == null) return null;
    elements.add(res);
    return elements.length - 1;
  }
}

class JSAutoFreeFunction {
  final JSInvokable func;

  /// Automatically free the function when it's not used anymore
  JSAutoFreeFunction(this.func) {
    func.dup();
    finalizer.attach(this, func);
  }

  dynamic call(List<dynamic> args) {
    return func(args);
  }

  static final finalizer = Finalizer<JSInvokable>((func) {
    func.destroy();
  });
}
