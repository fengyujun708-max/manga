import 'dart:convert';
import 'package:mangaverse/service/user_api.dart';
import 'package:mangaverse/main.dart';
import 'package:shared_preferences/shared_preferences.dart';

class UserSession {
  final String userId;
  final String username;
  final String token;

  const UserSession({
    required this.userId,
    required this.username,
    required this.token,
  });

  Map<String, dynamic> toJson() => {"userId": userId, "username": username, "token": token};
  static UserSession? fromJson(Map<String, dynamic>? json) {
    if (json == null) return null;
    return UserSession(
      userId: json["userId"] ?? "",
      username: json["username"] ?? "",
      token: json["token"] ?? "",
    );
  }
}

class AuthManager {
  static final AuthManager instance = AuthManager._();
  AuthManager._();

  UserSession? _session;
  bool get isLoggedIn => _session != null;
  UserSession? get session => _session;
  String get userId => _session?.userId ?? "";
  String get username => _session?.username ?? "";

  Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString("user_session");
    if (raw != null) {
      try {
        _session = UserSession.fromJson(jsonDecode(raw));
      } catch (_) { _session = null; }
    }
  }

  Future<Map<String, dynamic>> login(String phone, String password) async {
    final result = await userApiLogin(phone, password);
    if (result?.containsKey("success") == true && result?["success"] == true) {
      _session = UserSession(
        userId: result!["userId"] as String,
        username: result["username"] as String,
        token: result["token"] as String,
      );
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString("user_session", jsonEncode(_session!.toJson()));
    }
    return result ?? {"success": false, "detail": "网络错误"};
  }

  Future<Map<String, dynamic>> register(String phone, String password) async {
    final result = await userApiRegister(phone, password);
    if (result?.containsKey("success") == true && result?["success"] == true) {
      _session = UserSession(
        userId: result!["userId"] as String,
        username: result["username"] as String,
        token: result["token"] as String,
      );
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString("user_session", jsonEncode(_session!.toJson()));
    }
    return result ?? {"success": false, "detail": "网络错误"};
  }

  Future<void> logout() async {
    _session = null;
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove("user_session");
  }
}
