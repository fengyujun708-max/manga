import 'package:flutter/foundation.dart';
import 'package:mangaverse/service/crash_reporter.dart';
import 'package:mangaverse/main.dart';

class GlobalErrorHandler {
  static final GlobalErrorHandler instance = GlobalErrorHandler._();
  GlobalErrorHandler._();

  void init() {
    FlutterError.onError = (details) {
      logger.e('FlutterError', error: details.exception, stackTrace: details.stack);
      CrashReporter.instance.reportCrash(details.exceptionToString(), details.stack.toString());
    };
    PlatformDispatcher.instance.onError = (error, stack) {
      logger.e('PlatformError', error: error, stackTrace: stack);
      CrashReporter.instance.reportCrash(error.toString(), stack.toString());
      return true;
    };
  }
}
