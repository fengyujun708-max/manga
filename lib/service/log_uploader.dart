import 'dart:convert';
import 'package:mangaverse/service/user_api.dart';
import 'package:mangaverse/service/auth_manager.dart';
import 'package:mangaverse/main.dart';

class LogUploader {
  static final LogUploader instance = LogUploader._();
  LogUploader._();

  Future<bool> upload(String level, String message, {String extra = ""}) async {
    final userId = AuthManager.instance.userId;
    if (userId.isEmpty) return false;
    final log = {
      "user_id": userId,
      "log_type": "app_log",
      "level": level,
      "message": message,
      "extra": extra,
    };
    return await userApiUploadLog(log);
  }
}
