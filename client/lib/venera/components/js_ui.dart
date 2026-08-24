// (c) 2026 漫界 — JsUiApi 简化版（官方 venera ui 桥的占位实现）
// 源中调用 showMessage/showDialog 等 UI 方法时安全空转，不中断执行。
import 'package:flutter/foundation.dart' show protected;
import 'package:flutter/material.dart' show BuildContext;
import 'package:flutter_qjs/flutter_qjs.dart' show JSInvokable;

mixin class JsUiApi {
  dynamic handleUIMessage(Map<String, dynamic> message) {
    // 漫界：UI 消息占位（原版渲染对话框；简化版直接返回）
    return null;
  }

  @protected
  Future<void> something() async {}
}

// 占位类型（官方 components 依赖）
class JSAutoFreeFunction {
  JSAutoFreeFunction(JSInvokable f);
}

// ignore: unused_element
String _unused(BuildContext c) => '';