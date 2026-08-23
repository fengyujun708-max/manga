import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';

/// 书架服务 — 收藏 / 阅读历史 / 进度记忆
class LibraryService {
  static LibraryService? _instance;
  static LibraryService get instance => _instance ??= LibraryService._();
  LibraryService._();

  static const _favKey = 'library_favorites';
  static const _historyKey = 'library_history';
  static const _progressKey = 'library_progress';

  Future<SharedPreferences> get _prefs => SharedPreferences.getInstance();

  // ===== 收藏 =====
  Future<List<Map<String, dynamic>>> getFavorites() async {
    final p = await _prefs;
    try { return List<Map<String, dynamic>>.from(jsonDecode(p.getString(_favKey) ?? '[]')); }
    catch (_) { return []; }
  }

  Future<bool> isFavorited(String sourceId, String comicId) async {
    final list = await getFavorites();
    return list.any((e) => e['sourceId'] == sourceId && e['comicId'] == comicId);
  }

  Future<void> toggleFavorite(Map<String, dynamic> comic) async {
    final p = await _prefs;
    var list = await getFavorites();
    final exists = list.any((e) => e['sourceId'] == comic['sourceId'] && e['comicId'] == comic['comicId']);
    if (exists) {
      list.removeWhere((e) => e['sourceId'] == comic['sourceId'] && e['comicId'] == comic['comicId']);
    } else {
      comic['addedAt'] = DateTime.now().toIso8601String();
      list.insert(0, comic);
    }
    await p.setString(_favKey, jsonEncode(list));
  }

  // ===== 阅读历史 =====
  Future<List<Map<String, dynamic>>> getHistory() async {
    final p = await _prefs;
    try { return List<Map<String, dynamic>>.from(jsonDecode(p.getString(_historyKey) ?? '[]')); }
    catch (_) { return []; }
  }

  Future<void> recordRead({required String sourceId, required String comicId, required String title, required String cover, required int chapterIndex, required String chapterTitle}) async {
    final p = await _prefs;
    var list = await getHistory();
    list.removeWhere((e) => e['sourceId'] == sourceId && e['comicId'] == comicId);
    list.insert(0, {
      'sourceId': sourceId, 'comicId': comicId, 'title': title,
      'cover': cover, 'chapterIndex': chapterIndex, 'chapterTitle': chapterTitle,
      'readAt': DateTime.now().toIso8601String(),
    });
    if (list.length > 100) list = list.sublist(0, 100);
    await p.setString(_historyKey, jsonEncode(list));
  }

  /// 获取上次阅读进度（用于继续阅读）
  Future<Map<String, dynamic>?> getProgress(String sourceId, String comicId) async {
    final history = await getHistory();
    try {
      return history.firstWhere((e) => e['sourceId'] == sourceId && e['comicId'] == comicId);
    } catch (_) { return null; }
  }
}