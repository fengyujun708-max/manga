import 'dart:io';
import 'dart:convert';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter/foundation.dart';
import 'package:mangaverse/network/http/wind_http.dart';
import 'package:mangaverse/service/user_api.dart';
import 'package:mangaverse/service/auth_manager.dart';
import 'package:mangaverse/main.dart';

class CrashReporter {
  static final CrashReporter instance = CrashReporter._();
  CrashReporter._();

  String get _appVersion => "1.0.0";

  Map<String, String> _deviceInfo = {};
  bool _deviceInfoLoaded = false;

  Future<Map<String, String>> getDeviceInfo() async {
    if (_deviceInfoLoaded) return _deviceInfo;
    try {
      final info = DeviceInfoPlugin();
      if (Platform.isAndroid) {
        final d = await info.androidInfo;
        _deviceInfo = {
          "model": d.model,
          "manufacturer": d.manufacturer,
          "os": "Android ${d.version.release}",
          "sdk": d.version.sdkInt.toString(),
          "brand": d.brand,
          "chipset": d.hardware,
        };
      } else if (Platform.isIOS) {
        final d = await info.iosInfo;
        _deviceInfo = {
          "model": d.model,
          "os": "iOS ${d.systemVersion}",
          "name": d.name,
        };
      }
    } catch (e) {
      _deviceInfo = {"error": e.toString()};
    }
    _deviceInfoLoaded = true;
    return _deviceInfo;
  }

  Future<String> _getClientIp() async {
    try {
      final res = await fetch(
        "http://39.106.192.137/api/account/ip",
        timeout: const Duration(seconds: 5),
      );
      if (res.ok) {
        final data = jsonDecode(res.text);
        if (data is Map && data["ip"] != null) return data["ip"] as String;
      }
    } catch (_) {}
    return "unknown";
  }

  Future<Map<String, String>> buildCrashReport(String errorMessage, String stackTrace) async {
    final device = await getDeviceInfo();
    final ip = await _getClientIp();
    final session = AuthManager.instance.session;
    return {
      "user_id": session?.userId ?? "anonymous",
      "username": session?.username ?? "",
      "device_info": jsonEncode(device),
      "ip_address": ip,
      "account_info": session != null ? jsonEncode({"userId": session.userId, "username": session.username}) : "",
      "error_message": errorMessage,
      "stack_trace": stackTrace,
      "app_version": _appVersion,
      "os_version": device["os"] ?? "unknown",
    };
  }

  Future<bool> reportCrash(String errorMessage, String stackTrace) async {
    try {
      final report = await buildCrashReport(errorMessage, stackTrace);
      return await userApiReportCrash(report);
    } catch (e) {
      logger.e("crash report upload failed", error: e);
      return false;
    }
  }

  Future<Map<String, dynamic>> registerCrashHandler() async {
    final device = await getDeviceInfo();
    return {
      "device": jsonEncode(device),
      "appVersion": _appVersion,
    };
  }
}
