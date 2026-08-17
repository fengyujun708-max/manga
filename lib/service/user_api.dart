import 'dart:convert';

import 'package:mangaverse/network/http/wind_http.dart';
import 'package:mangaverse/main.dart';

const _baseUrl = "http://39.106.192.137";

Future<Map<String, dynamic>?> userApiVerifyPhone(String phone) async {
  try {
    final res = await fetch("$_baseUrl/api/account/phone/verify",
      method: "POST",
      body: jsonEncode({"phone": phone}),
      headers: {"Content-Type": "application/json"},
      timeout: const Duration(seconds: 10),
    );
    if (res.ok) return jsonDecode(res.text) as Map<String, dynamic>?;
  } catch (e) {
    logger.e("verify phone failed", error: e);
  }
  return null;
}

Future<Map<String, dynamic>?> userApiRegister(String phone, String password) async {
  try {
    final res = await fetch("$_baseUrl/api/account/register",
      method: "POST",
      body: jsonEncode({"phone": phone, "password": password}),
      headers: {"Content-Type": "application/json"},
      timeout: const Duration(seconds: 15),
    );
    if (res.ok) return jsonDecode(res.text) as Map<String, dynamic>?;
  } catch (e) {
    logger.e("register failed", error: e);
  }
  return null;
}

Future<Map<String, dynamic>?> userApiLogin(String phone, String password) async {
  try {
    final res = await fetch("$_baseUrl/api/account/login",
      method: "POST",
      body: jsonEncode({"login": phone, "password": password}),
      headers: {"Content-Type": "application/json"},
      timeout: const Duration(seconds: 15),
    );
    if (res.ok) return jsonDecode(res.text) as Map<String, dynamic>?;
  } catch (e) {
    logger.e("login failed", error: e);
  }
  return null;
}

Future<List<Map<String, dynamic>>?> userApiGetBookshelf(String userId) async {
  try {
    final res = await fetch("$_baseUrl/api/account/bookshelf/$userId",
      timeout: const Duration(seconds: 15));
    if (res.ok) {
      final data = jsonDecode(res.text);
      if (data is List) return data.cast<Map<String, dynamic>>();
    }
  } catch (e) { logger.e("get bookshelf failed", error: e); }
  return null;
}

Future<bool> userApiSyncBookshelf(String userId, List<Map<String, dynamic>> items) async {
  try {
    final res = await fetch("$_baseUrl/api/account/bookshelf/sync",
      method: "POST",
      body: jsonEncode({"items": items}),
      headers: {"Content-Type": "application/json", "X-User-Id": userId},
      timeout: const Duration(seconds: 15));
    return res.ok;
  } catch (e) { logger.e("sync bookshelf failed", error: e); return false; }
}

Future<bool> userApiReportCrash(Map<String, dynamic> report) async {
  try {
    final res = await fetch("$_baseUrl/api/account/crash/report",
      method: "POST",
      body: jsonEncode(report),
      headers: {"Content-Type": "application/json"},
      timeout: const Duration(seconds: 15));
    return res.ok;
  } catch (e) { logger.e("crash report failed", error: e); return false; }
}

Future<bool> userApiUploadLog(Map<String, dynamic> log) async {
  try {
    final res = await fetch("$_baseUrl/api/account/log/upload",
      method: "POST",
      body: jsonEncode(log),
      headers: {"Content-Type": "application/json"},
      timeout: const Duration(seconds: 15));
    return res.ok;
  } catch (e) { logger.e("log upload failed", error: e); return false; }
}
