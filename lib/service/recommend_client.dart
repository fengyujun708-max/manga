import 'dart:convert';
import 'package:mangaverse/network/http/wind_http.dart';
import 'package:mangaverse/main.dart';

/// 推荐服务 - 从后端API获取推荐数据
class RecommendService {
  static const String _baseUrl = 'https://api.mangaverse.app/api/recommend';

  /// 获取个性化推荐流
  static Future<List<RecommendItem>> getHomeRecommendations({
    String uid = '',
    int limit = 20,
  }) async {
    try {
      final uri = Uri.parse('$_baseUrl/home').replace(queryParameters: {
        'uid': uid,
        'limit': limit.toString(),
      });
      final response = await windHttp.get(uri.toString());
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        final list = data['recommendations'] as List? ?? [];
        return list.map((e) => RecommendItem.fromJson(e)).toList();
      }
    } catch (e, st) {
      logger.w('获取推荐失败', error: e, stackTrace: st);
    }
    return [];
  }

  /// 获取热门漫画
  static Future<List<RecommendItem>> getHotComics({
    int limit = 20,
    int page = 1,
  }) async {
    try {
      final uri = Uri.parse('$_baseUrl/hot').replace(queryParameters: {
        'limit': limit.toString(),
        'page': page.toString(),
      });
      final response = await windHttp.get(uri.toString());
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        final list = data['hot'] as List? ?? [];
        return list.map((e) => RecommendItem.fromJson(e)).toList();
      }
    } catch (e, st) {
      logger.w('获取热门失败', error: e, stackTrace: st);
    }
    return [];
  }

  /// 获取相似漫画
  static Future<List<RecommendItem>> getRelatedComics(String comicId, {int limit = 10}) async {
    try {
      final uri = Uri.parse('$_baseUrl/related').replace(queryParameters: {
        'comic_id': comicId,
        'limit': limit.toString(),
      });
      final response = await windHttp.get(uri.toString());
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        final list = data['related'] as List? ?? [];
        return list.map((e) => RecommendItem.fromJson(e)).toList();
      }
    } catch (e, st) {
      logger.w('获取相似漫画失败', error: e, stackTrace: st);
    }
    return [];
  }

  /// 记录阅读反馈
  static Future<void> recordFeedback(String uid, String comicId, {String action = 'view'}) async {
    try {
      final uri = Uri.parse('$_baseUrl/feedback').replace(queryParameters: {
        'uid': uid,
        'comic_id': comicId,
        'action': action,
      });
      await windHttp.post(uri.toString());
    } catch (e) {
      // 静默失败
    }
  }
}

class RecommendItem {
  final String mangaId;
  final String title;
  final String author;
  final String coverUrl;
  final String source;
  final double score;
  final String reason;

  const RecommendItem({
    required this.mangaId,
    this.title = '',
    this.author = '',
    this.coverUrl = '',
    this.source = '',
    this.score = 0.0,
    this.reason = '',
  });

  factory RecommendItem.fromJson(Map<String, dynamic> json) {
    return RecommendItem(
      mangaId: json['manga_id']?.toString() ?? json['mangaId']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      author: json['author']?.toString() ?? '',
      coverUrl: json['cover_url']?.toString() ?? json['coverUrl']?.toString() ?? '',
      source: json['source']?.toString() ?? '',
      score: (json['score'] ?? 0).toDouble(),
      reason: json['reason']?.toString() ?? '',
    );
  }
}
