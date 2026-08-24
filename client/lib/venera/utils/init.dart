import 'dart:async';

import 'package:flutter/foundation.dart';

/// A mixin class that provides a way to ensure the class is initialized.
abstract mixin class Init {
  bool _isInit = false;
  Future<void>? _initializing;

  final _initCompleter = <Completer<void>>[];

  /// Ensure the class is initialized.
  Future<void> ensureInit() async {
    if (_isInit) {
      return;
    }
    var completer = Completer<void>();
    _initCompleter.add(completer);
    return completer.future;
  }

  Future<void> _markInit() async {
    _isInit = true;
    for (var completer in _initCompleter) {
      completer.complete();
    }
    _initCompleter.clear();
  }

  @protected
  Future<void> doInit();

  /// Initialize the class（防并发：多处同时调用只执行一次 doInit）
  Future<void> init() async {
    if (_isInit) {
      return;
    }
    // 并发保护：第二个调用者等待第一个的初始化完成，而不是重复 doInit
    final inFlight = _initializing;
    if (inFlight != null) {
      return inFlight;
    }
    final f = doInit().then((_) async => await _markInit());
    _initializing = f;
    try {
      await f;
    } finally {
      _initializing = null;
    }
  }
}