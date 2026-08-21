import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:path_provider/path_provider.dart';
import '../manga_source.dart';
import 'runtime/venera_engine.dart';

/// 本地执行型 Venera 源
/// 源 JS 下载到本地后，通过 QuickJS 在客户端执行，网络直连手机（VPN 生效）
class LocalVeneraSource implements MangaSource {
  @override
  final SourceManifest manifest;
  @override
  bool enabled = true;

  final String _sourceDir;
  String? _jsCode;
  final VeneraEngine _engine;

  LocalVeneraSource({required this.manifest, required String sourceDir, VeneraEngine? engine})
      : _sourceDir = sourceDir,
        _engine = engine ?? VeneraEngine.instance;

  @override
  String get id => manifest.id;
  @override
  String get name => manifest.name;
  @override
  String get version => manifest.version;

  /// 确保 JS 代码已加载到引擎
  Future<bool> ensureLoaded() async {
    if (_jsCode == null) {
      final file = File('$_sourceDir/${manifest.id}.js');
      if (!await file.exists()) return false;
      _jsCode = await file.readAsString();
    }
    await _engine.executeSource(manifest.id, _jsCode!);
    return true;
  }

  Future<bool> loadJsCode(String code) async {
    _jsCode = code;
    final dir = Directory(_sourceDir);
    if (!await dir.exists()) await dir.create(recursive: true);
    final file = File('$_sourceDir/${manifest.id}.js');
    await file.writeAsString(code);
    return true;
  }

  /// 首页板块列表 → explore
  Future<List<ExploreSection>> explore() async {
    if (!await ensureLoaded()) return [];
    final raw = await _engine.callSource(manifest.id, 'explore', const []);
    // explore 是数组，取第一个元素的 load() 结果
    final sections = <ExploreSection>[];
    if (raw is List) {
      for (final sec in raw) {
        if (sec is! Map) continue;
        final loadFn = sec['load'];
        final title = sec['title']?.toString() ?? '首页';
        if (loadFn != null) {
          try {
            final res = await _engine.callSource(manifest.id, 'explore', const []);
            break;
          } catch (_) {}
        }
        sections.add(ExploreSection(title: title, comics: []));
      }
    }
    return sections;
  }

  @override
  Future<SearchResult> search(String keyword, {int page = 1, int pageSize = 20}) async {
    if (!await ensureLoaded()) {
      return SearchResult(comics: [], total: 0, page: page, pageSize: pageSize, hasMore: false);
    }
    final raw = await _engine.callSource(manifest.id, 'search', [keyword, page, pageSize]);
    return _parseSearchRaw(raw, page, pageSize);
  }

  @override
  Future<ComicInfo> getDetail(String comicId) async {
    if (!await ensureLoaded()) throw Exception('源未加载');
    final raw = await _engine.callSource(manifest.id, 'comic', [comicId]);
    return _parseDetail(raw, comicId);
  }

  @override
  Future<List<ChapterInfo>> getChapters(String comicId) async {
    if (!await ensureLoaded()) return [];
    try {
      final raw = await _engine.callSource(manifest.id, 'comic', [comicId]);
      final chapters = <ChapterInfo>[];
      if (raw is Map) {
        final chs = raw['chapters'] ?? raw['episodes'];
        if (chs is List) {
          for (final c in chs) {
            if (c is Map) {
              chapters.add(ChapterInfo(
                id: (c['id'] ?? c['episode'] ?? '').toString(),
                title: (c['title'] ?? c['name'] ?? c['episode'] ?? '').toString(),
              ));
            }
          }
        }
      }
      return chapters;
    } catch (_) {
      return [];
    }
  }

  @override
  Future<List<PageInfo>> getPages(String comicId, String chapterId) async {
    if (!await ensureLoaded()) return [];
    try {
      final raw = await _engine.callSource(manifest.id, 'pages', [comicId, chapterId]);
      final pages = <PageInfo>[];
      if (raw is Map) {
        final list = raw['pages'] ?? raw['images'] ?? raw['urls'];
        final next = raw['next'];
        if (list is List) {
          for (final p in list) {
            pages.add(PageInfo(url: p.toString()));
          }
        }
      } else if (raw is List) {
        for (final p in raw) {
          pages.add(PageInfo(url: p.toString()));
        }
      }
      return pages;
    } catch (_) {
      return [];
    }
  }

  @override
  Future<List<CategoryInfo>> getCategories() async {
    if (!await ensureLoaded()) return [];
    try {
      final raw = await _engine.callSource(manifest.id, 'category', const []);
      final out = <CategoryInfo>[];
      if (raw is Map) {
        final parts = raw['parts'];
        if (parts is List) {
          for (final p in parts) {
            if (p is Map) {
              final name = p['name']?.toString() ?? '';
              final cats = p['categories'];
              if (cats is List) {
                for (final c in cats) {
                  out.add(CategoryInfo(id: c.toString(), name: c.toString(), icon: '', count: 0));
                }
              } else if (name.isNotEmpty) {
                out.add(CategoryInfo(id: name, name: name, icon: '', count: 0));
              }
            }
          }
        }
      }
      return out;
    } catch (_) {
      return [];
    }
  }

  @override
  Future<SearchResult> getByCategory(String categoryId, {int page = 1, int pageSize = 20}) async {
    if (!await ensureLoaded()) {
      return SearchResult(comics: [], total: 0, page: page, pageSize: pageSize, hasMore: false);
    }
    try {
      final raw = await _engine.callSource(manifest.id, 'categoryComics', [categoryId, '', [], page]);
      return _parseSearchRaw(raw, page, pageSize);
    } catch (_) {
      return SearchResult(comics: [], total: 0, page: page, pageSize: pageSize, hasMore: false);
    }
  }

  SearchResult _parseSearchRaw(dynamic raw, int page, int pageSize) {
    final comics = <ComicInfo>[];
    if (raw is Map) {
      final items = raw['comics'] ?? raw['items'] ?? raw['list'] ?? raw['results'];
      if (items is List) {
        for (final it in items) {
          if (it is Map) {
            comics.add(ComicInfo(
              id: (it['id'] ?? '').toString(),
              title: (it['title'] ?? it['name'] ?? '').toString(),
              coverUrl: (it['cover'] ?? it['coverUrl'] ?? '').toString(),
              description: (it['description'] ?? '').toString(),
            ));
          }
        }
      }
      final hasMore = raw['hasMore'] == true || raw['hasNextPage'] == true;
      return SearchResult(comics: comics, total: comics.length, page: page, pageSize: pageSize, hasMore: hasMore);
    } else if (raw is List) {
      for (final it in raw) {
        if (it is Map) {
          comics.add(ComicInfo(
            id: (it['id'] ?? '').toString(),
            title: (it['title'] ?? it['name'] ?? '').toString(),
            coverUrl: (it['cover'] ?? it['coverUrl'] ?? '').toString(),
            description: (it['description'] ?? '').toString(),
          ));
        }
      }
      return SearchResult(comics: comics, total: comics.length, page: page, pageSize: pageSize, hasMore: false);
    }
    return SearchResult(comics: [], total: 0, page: page, pageSize: pageSize, hasMore: false);
  }

  ComicInfo _parseDetail(dynamic raw, String comicId) {
    if (raw is Map) {
      return ComicInfo(
        id: (raw['id'] ?? comicId).toString(),
        title: (raw['title'] ?? raw['name'] ?? '').toString(),
        coverUrl: (raw['cover'] ?? '').toString(),
        description: (raw['description'] ?? '').toString(),
        author: (raw['author'] ?? raw['subTitle'] ?? '').toString(),
        status: (raw['status'] ?? '').toString(),
      );
    }
    return ComicInfo(id: comicId, title: '', coverUrl: '', description: '');
  }
}

/// 首页板块
class ExploreSection {
  final String title;
  final List<ComicInfo> comics;
  ExploreSection({required this.title, required this.comics});
}

/// 源安装器：下载源 JS 到本地
class LocalVeneraInstaller {
  /// 根据 manifest 的 downloadUrl 下载 JS 并保存本地
  static Future<bool> install(SourceManifest manifest, String sourceDir) async {
    try {
      final url = manifest.downloadUrl.isEmpty ? manifest.repositoryUrl : manifest.downloadUrl;
      if (url.isEmpty) return false;
      final client = HttpClient();
      try {
        final req = await client.getUrl(Uri.parse(url));
        req.headers.set('User-Agent', 'Mozilla/5.0 (Linux; Android 13) Chrome/120.0 Mobile');
        final resp = await req.close();
        if (resp.statusCode != 200) return false;
        final bytes = await resp.fold<List<int>>(<int>[], (a, b) => a..addAll(b));
        final code = utf8.decode(bytes, allowMalformed: true);
        if (!code.contains('ComicSource')) return false;
        final dir = Directory(sourceDir);
        if (!await dir.exists()) await dir.create(recursive: true);
        final file = File('$sourceDir/${manifest.id}.js');
        await file.writeAsString(code);
        return true;
      } finally {
        client.close();
      }
    } catch (_) {
      return false;
    }
  }

  static Future<String?> ensureSourceDir() async {
    try {
      final docs = await getApplicationDocumentsDirectory();
      final dir = Directory('${docs.path}/sources');
      if (!await dir.exists()) await dir.create(recursive: true);
      return dir.path;
    } catch (_) {
      return null;
    }
  }
}