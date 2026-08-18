import 'dart:convert';
import 'package:mangaverse/service/user_api.dart';
import 'package:mangaverse/service/auth_manager.dart';
import 'package:mangaverse/main.dart';

class BookshelfSync {
  static final BookshelfSync instance = BookshelfSync._();
  BookshelfSync._();

  Future<bool> syncItem(Map<String, dynamic> item) async {
    final userId = AuthManager.instance.userId;
    if (userId.isEmpty) return false;
    final items = [item];
    final ok = await userApiSyncBookshelf(userId, items);
    if (ok) logger.d('书架同步成功: ${item["comicId"]?.toString() ?? "?"}');
    return ok;
  }

  Future<bool> syncItems(List<Map<String, dynamic>> items) async {
    if (items.isEmpty) return false;
    final userId = AuthManager.instance.userId;
    if (userId.isEmpty) return false;
    return await userApiSyncBookshelf(userId, items);
  }

  Future<List<Map<String, dynamic>>?> loadRemote() async {
    final userId = AuthManager.instance.userId;
    if (userId.isEmpty) return null;
    return await userApiGetBookshelf(userId);
  }
}
