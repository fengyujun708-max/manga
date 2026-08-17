import 'dart:async';

import 'package:mangaverse/main.dart';
import 'package:mangaverse/util/event/event.dart';

class WebViewObserveBus {
  static Stream<WebViewObserveEvent> get stream =>
      eventBus.on<WebViewObserveEvent>();

  static void emit(WebViewObserveEvent event) {
    eventBus.fire(event);
  }
}
