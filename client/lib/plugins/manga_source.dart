import 'dart:async';
import 'dart:convert';

/// 漫画源清单
class SourceManifest {
  final String id;
  final String name;
  final String version;
  final String author;
  final String description;
  final String icon;
  final String repositoryUrl;
  final String downloadUrl;
  final String minAppVersion;
  final List<String> capabilities;
  final int downloads;
  final double rating;
  final Map<String, dynamic> metadata;

  SourceManifest({
    required this.id,
    required this.name,
    required this.version,
    required this.author,
    required this.description,
    required this.icon,
    required this.repositoryUrl,
    required this.downloadUrl,
    required this.minAppVersion,
    required this.capabilities,
    required this.downloads,
    required this.rating,
    this.metadata = const {},
  });

  factory SourceManifest.fromJson(Map<String, dynamic> json) {
    return SourceManifest(
      id: json['id'] ?? '',
      name: json['name'] ?? '',
      version: json['version'] ?? '1.0.0',
      author: json['author'] ?? '',
      description: json['description'] ?? '',
      icon: json['icon'] ?? '📚',
      repositoryUrl: json['repositoryUrl'] ?? '',
      downloadUrl: json['downloadUrl'] ?? '',
      minAppVersion: json['minAppVersion'] ?? '1.0.0',
      capabilities: (json['capabilities'] as List?)?.map((e) => e.toString()).toList() ??
          ['search', 'detail', 'chapters', 'pages'],
      downloads: json['downloads'] as int? ?? 0,
      rating: (json['rating'] as num?)?.toDouble() ?? 0.0,
      metadata: Map<String, dynamic>.from(json['metadata'] ?? {}),
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'name': name,
    'version': version,
    'author': author,
    'description': description,
    'icon': icon,
    'repositoryUrl': repositoryUrl,
    'downloadUrl': downloadUrl,
    'minAppVersion': minAppVersion,
    'capabilities': capabilities,
    'downloads': downloads,
    'rating': rating,
    'metadata': metadata,
  };
}

/// 漫画基本信息
class ComicInfo {
  final String id;
  final String title;
  final String coverUrl;
  final String author;
  final String description;
  final List<String> tags;
  final String status; // ongoing, completed, hiatus
  final double rating;
  final int views;
  final int chapterCount;
  final String sourceId;

  ComicInfo({
    required this.id,
    required this.title,
    required this.coverUrl,
    required this.author,
    required this.description,
    required this.tags,
    required this.status,
    required this.rating,
    required this.views,
    required this.chapterCount,
    required this.sourceId,
  });

  factory ComicInfo.fromJson(Map<String, dynamic> json) {
    return ComicInfo(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      coverUrl: json['coverUrl']?.toString() ?? '',
      author: json['author']?.toString() ?? '',
      description: json['description']?.toString() ?? '',
      tags: (json['tags'] as List?)?.map((e) => e.toString()).toList() ?? [],
      status: json['status']?.toString() ?? 'unknown',
      rating: (json['rating'] as num?)?.toDouble() ?? 0.0,
      views: json['views'] as int? ?? 0,
      chapterCount: json['chapterCount'] as int? ?? 0,
      sourceId: json['sourceId']?.toString() ?? '',
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'title': title,
    'coverUrl': coverUrl,
    'author': author,
    'description': description,
    'tags': tags,
    'status': status,
    'rating': rating,
    'views': views,
    'chapterCount': chapterCount,
    'sourceId': sourceId,
  };
}

/// 章节信息
class ChapterInfo {
  final String id;
  final String title;
  final int chapterNumber;
  final int pageCount;
  final String sourceId;
  final String comicId;
  final DateTime? updatedAt;

  ChapterInfo({
    required this.id,
    required this.title,
    required this.chapterNumber,
    required this.pageCount,
    required this.sourceId,
    required this.comicId,
    this.updatedAt,
  });

  factory ChapterInfo.fromJson(Map<String, dynamic> json) {
    return ChapterInfo(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      chapterNumber: (json['chapterNumber'] as num?)?.toInt() ?? 0,
      pageCount: json['pageCount'] as int? ?? 0,
      sourceId: json['sourceId']?.toString() ?? '',
      comicId: json['comicId']?.toString() ?? '',
      updatedAt: json['updatedAt'] != null ? DateTime.parse(json['updatedAt'].toString()) : null,
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'title': title,
    'chapterNumber': chapterNumber,
    'pageCount': pageCount,
    'sourceId': sourceId,
    'comicId': comicId,
    'updatedAt': updatedAt?.toIso8601String(),
  };
}

/// 页面信息
class PageInfo {
  final String id;
  final int pageNumber;
  final String imageUrl;
  final String sourceId;
  final String chapterId;

  PageInfo({
    required this.id,
    required this.pageNumber,
    required this.imageUrl,
    required this.sourceId,
    required this.chapterId,
  });

  factory PageInfo.fromJson(Map<String, dynamic> json) {
    return PageInfo(
      id: json['id']?.toString() ?? '',
      pageNumber: (json['pageNumber'] as num?)?.toInt() ?? 0,
      imageUrl: json['imageUrl']?.toString() ?? '',
      sourceId: json['sourceId']?.toString() ?? '',
      chapterId: json['chapterId']?.toString() ?? '',
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'pageNumber': pageNumber,
    'imageUrl': imageUrl,
    'sourceId': sourceId,
    'chapterId': chapterId,
  };
}

/// 搜索结果
class SearchResult {
  final List<ComicInfo> comics;
  final int total;
  final int page;
  final int pageSize;
  final bool hasMore;

  SearchResult({
    required this.comics,
    required this.total,
    required this.page,
    required this.pageSize,
    required this.hasMore,
  });

  factory SearchResult.fromJson(Map<String, dynamic> json) {
    return SearchResult(
      comics: (json['comics'] as List?)?.map((e) => ComicInfo.fromJson(e)).toList() ?? [],
      total: json['total'] as int? ?? 0,
      page: json['page'] as int? ?? 1,
      pageSize: json['pageSize'] as int? ?? 20,
      hasMore: json['hasMore'] as bool? ?? false,
    );
  }
}

/// 统一漫画源接口
abstract class MangaSource {
  final SourceManifest manifest;
  bool enabled = true;

  MangaSource(this.manifest);

  String get id => manifest.id;
  String get name => manifest.name;
  String get version => manifest.version;

  // 核心方法
  Future<SearchResult> search(String keyword, {int page = 1, int pageSize = 20});
  Future<ComicInfo> getDetail(String comicId);
  Future<List<ChapterInfo>> getChapters(String comicId);
  Future<List<PageInfo>> getPages(String comicId, String chapterId);

  // 可选方法
  Future<SearchResult> getPopular({int page = 1, int pageSize = 20}) async =>
      SearchResult(comics: [], total: 0, page: 1, pageSize: 20, hasMore: false);
  Future<SearchResult> getLatest({int page = 1, int pageSize = 20}) async =>
      SearchResult(comics: [], total: 0, page: 1, pageSize: 20, hasMore: false);
  Future<List<CategoryInfo>> getCategories() async => [];
  Future<SearchResult> getByCategory(String categoryId, {int page = 1, int pageSize = 20}) async =>
      SearchResult(comics: [], total: 0, page: 1, pageSize: 20, hasMore: false);
  Future<bool> login(Map<String, String> credentials) async => false;
  Future<bool> checkLogin() async => false;
  Future<List<ComicInfo>> getFavorites({int page = 1}) async => [];
  Future<bool> addFavorite(String comicId) async => false;
  Future<bool> removeFavorite(String comicId) async => false;
}

class CategoryInfo {
  final String id;
  final String name;
  final String icon;
  final int count;

  CategoryInfo({required this.id, required this.name, required this.icon, required this.count});

  factory CategoryInfo.fromJson(Map<String, dynamic> json) {
    return CategoryInfo(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      icon: json['icon']?.toString() ?? '',
      count: (json['count'] as num?)?.toInt() ?? 0,
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'name': name,
    'icon': icon,
    'count': count,
  };
}

/// 源管理器
class SourceManager {
  final Map<String, MangaSource> _sources = {};
  final Map<String, SourceManifest> _manifests = {};
  final Map<String, bool> _enabled = {};

  Future<void> loadManifests(List<SourceManifest> manifests) async {
    for (final manifest in manifests) {
      _manifests[manifest.id] = manifest;
      _enabled[manifest.id] = true;
    }
  }

  Future<void> registerSource(MangaSource source) async {
    _sources[source.manifest.id] = source;
    _manifests[source.manifest.id] = source.manifest;
    _enabled[source.manifest.id] = true;
  }

  void enableSource(String id) => _enabled[id] = true;
  void disableSource(String id) => _enabled[id] = false;
  bool isEnabled(String id) => _enabled[id] ?? false;

  MangaSource? getSource(String id) => _sources[id];
  SourceManifest? getManifest(String id) => _manifests[id];
  List<MangaSource> getEnabledSources() =>
      _sources.values.where((s) => _enabled[s.manifest.id] ?? false).toList();
  List<SourceManifest> getAllManifests() => _manifests.values.toList();
}

/// 源测试结果
class SourceTestResult {
  final bool passed;
  final List<TestCaseResult> results;
  final Duration duration;
  final String? error;

  SourceTestResult({
    required this.passed,
    required this.results,
    required this.duration,
    this.error,
  });

  factory SourceTestResult.failed(String error, Duration duration) {
    return SourceTestResult(
      passed: false,
      results: [TestCaseResult(name: '连接测试', passed: false, error: error)],
      duration: duration,
      error: error,
    );
  }
}

class TestCaseResult {
  final String name;
  final bool passed;
  final String? error;
  final Duration? duration;

  TestCaseResult({
    required this.name,
    required this.passed,
    this.error,
    this.duration,
  });
}