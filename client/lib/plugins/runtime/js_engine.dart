import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'manga_source.dart';

/// QuickJS 运行时引擎
/// 使用 flutter_quickjs 或 quickjs_dart 运行漫画源 JS 代码
class QuickJSEngine {
  bool _initialized = false;
  final Map<String, dynamic> _globals = {};
  final Map<String, dynamic> _hostFunctions = {};

  /// 初始化 JS 运行时
  Future<void> initialize() async {
    if (_initialized) return;
    
    // 注入宿主函数
    _injectHostFunctions();
    _initialized = true;
  }

  /// 注入宿主环境 API
  void _injectHostFunctions() {
    // 网络请求
    _hostFunctions['fetch'] = _hostFetch;
    // DOM 解析
    _hostFunctions['DOMParser'] = _createDOMParser;
    // Base64
    _hostFunctions['atob'] = (String str) => base64Decode(str);
    _hostFunctions['btoa'] = (String str) => base64Encode(utf8.encode(str));
    // 定时器
    _hostFunctions['setTimeout'] = _hostSetTimeout;
    _hostFunctions['clearTimeout'] = _hostClearTimeout;
    _hostFunctions['setInterval'] = _hostSetInterval;
    _hostFunctions['clearInterval'] = _hostClearInterval;
    // 控制台
    _hostFunctions['console'] = _createConsole();
    // 存储
    _hostFunctions['localStorage'] = _createLocalStorage();
    // JSON
    _hostFunctions['JSON'] = {'parse': jsonDecode, 'stringify': jsonEncode};
    // 编码
    _hostFunctions['encodeURI'] = Uri.encodeComponent;
    _hostFunctions['decodeURI'] = Uri.decodeComponent;
    _hostFunctions['encodeURIComponent'] = Uri.encodeComponent;
    _hostFunctions['decodeURIComponent'] = Uri.decodeComponent;
  }

  /// 沙箱化的 fetch 实现
  Future<Map<String, dynamic>> _hostFetch(String url, [Map<String, dynamic>? options]) async {
    // 安全检查：只允许 HTTP/HTTPS
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      return {'status': 0, 'ok': false, 'error': '不允许的协议: $url'};
    }

    // 这里应该调用宿主的网络层 (Dio/HTTP)
    // 实际实现需要通过 MethodChannel 或 Dart FFI 调用宿主网络
    // 这里提供模拟实现
    try {
      // 实际项目中应该通过 MethodChannel 调用原生网络层
      // 这里返回模拟数据供测试
      return {
        'status': 200,
        'ok': true,
        'headers': {'content-type': 'text/html'},
        'body': '<html><body>模拟响应</body></html>',
        'url': url,
      };
    } catch (e) {
      return {
        'status': 0,
        'ok': false,
        'error': e.toString(),
      };
    }
  }

  /// 创建 DOMParser
  Map<String, Function> _createDOMParser() {
    return {
      'parseFromString': (String html, String mimeType) {
        // 简化的 HTML 解析器
        // 实际应该使用 html 包或原生解析器
        return _SimpleDocument(html);
      },
    };
  }

  /// 沙箱化 setTimeout
  int _hostSetTimeout(Function callback, int ms) {
    // 限制最大延迟
    final clampedMs = ms.clamp(0, 30000);
    final timerId = Timer(Duration(milliseconds: clampedMs), () {
      try {
        callback();
      } catch (e) {
        debugPrint('JS setTimeout error: $e');
      }
    }).hashCode;
    return timerId;
  }

  void _hostClearTimeout(int timerId) {
    // 实际应该维护 Timer 映射
  }

  int _hostSetInterval(Function callback, int ms) {
    final clampedMs = ms.clamp(100, 60000);
    return Timer.periodic(Duration(milliseconds: clampedMs), (_) {
      try {
        callback();
      } catch (e) {
        debugPrint('JS setInterval error: $e');
      }
    }).hashCode;
  }

  void _hostClearInterval(int timerId) {}

  Map<String, Function> _createConsole() {
    return {
      'log': (Object? msg) => debugPrint('[JS] $msg'),
      'warn': (Object? msg) => debugPrint('[JS WARN] $msg'),
      'error': (Object? msg) => debugPrint('[JS ERROR] $msg'),
      'info': (Object? msg) => debugPrint('[JS INFO] $msg'),
    };
  }

  Map<String, dynamic> _createLocalStorage() {
    final storage = <String, String>{};
    return {
      'getItem': (String key) => storage[key],
      'setItem': (String key, String value) => storage[key] = value,
      'removeItem': (String key) => storage.remove(key),
      'clear': () => storage.clear(),
      'length': storage.length,
      'key': (int index) => storage.keys.elementAt(index),
    };
  }

  /// 执行 JS 代码
  Future<T> evaluate<T>(String code) async {
    if (!_initialized) await initialize();
    // 实际应该调用 QuickJS 运行时
    // 这里提供模拟实现
    throw UnimplementedError('需要集成 quickjs_dart 或 flutter_quickjs 包');
  }

  /// 加载并执行源代码
  Future<MangaSource> loadSource(String sourceCode, SourceManifest manifest) async {
    await initialize();
    
    // 创建沙箱上下文
    final context = _SourceContext(manifest);
    
    // 执行源代码
    await evaluate(sourceCode);
    
    // 从全局获取导出的 source 对象
    final source = _globals['source'];
    if (source is MangaSource) {
      return source;
    }
    
    throw StateError('源代码未导出有效的 MangaSource 对象');
  }

  void dispose() {
    _initialized = false;
    _globals.clear();
    _hostFunctions.clear();
  }
}

/// 简化的 Document 对象
class _SimpleDocument {
  final String _html;

  _SimpleDocument(this._html);

  // querySelector
  dynamic querySelector(String selector) {
    // 简化实现
    return _SimpleElement(_extractFirstMatch(_html, selector));
  }

  // querySelectorAll
  List<dynamic> querySelectorAll(String selector) {
    // 简化实现
    return [];
  }

  String _extractFirstMatch(String html, String selector) {
    // 简化的 CSS 选择器匹配
    return '';
  }

  String get textContent => _html.replaceAll(RegExp(r'<[^>]*>'), '').trim();
}

class _SimpleElement {
  final String _html;

  _SimpleElement(this._html);

  String get textContent => _html.replaceAll(RegExp(r'<[^>]*>'), '').trim();
  
  String? getAttribute(String name) {
    final regex = RegExp('$name="([^"]*)"');
    final match = regex.firstMatch(_html);
    return match?.group(1);
  }

  dynamic querySelector(String selector) => null;
  List<dynamic> querySelectorAll(String selector) => [];
}

/// 源上下文
class _SourceContext {
  final SourceManifest manifest;
  final Map<String, dynamic> _exports = {};

  _SourceContext(this.manifest);

  void export(String name, dynamic value) {
    _exports[name] = value;
  }

  dynamic getExport(String name) => _exports[name];
}

/// 全局单例
final quickjsEngine = QuickJSEngine();