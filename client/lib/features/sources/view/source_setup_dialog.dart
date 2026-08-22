import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../../app/ds.dart';
import '../../../plugins/source_installer.dart';
import '../../../plugins/manga_source.dart';

/// 首次启动源配置检测 — 强制弹窗，一键安装 20 个可用源
class SourceSetupDialog {
  static const _flag = 'source_setup_done';

  /// 根 Navigator（由 app.dart 注入），确保弹窗一定能弹出
  static GlobalKey<NavigatorState>? navigatorKey;

  static const Map<String, String> _names = {
    'copy_manga': '拷贝漫画', 'jm': '禁漫天堂', 'komiic': 'Komiic',
    'comick': 'Comick', 'manga_dex': 'MangaDex', 'baozi': '包子漫画',
    'ccc': 'CCC追漫台', 'zaimanhua': '再漫画', 'manhuagui': '漫画柜',
    'manhuaren': '漫画人', 'manwaba': '漫画吧', 'hot_manga': '热血漫画',
    'jcomic': 'JComic', 'goda': 'GODA', 'mh18': '漫画18', 'mxs': '漫小肆',
    'nhentai': 'nHentai', 'wnacg': '绅士漫画', 'lanraragi': 'LANraragi',
    'hcomic': 'HComic',
  };

  /// 检查是否需要弹出（首次进入且未完成过）
  static Future<void> maybeShow(BuildContext context) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final done = prefs.getBool(_flag) ?? false;
      debugPrint('[SourceSetup] flag=$done');
      if (done) return;
      BuildContext ctx = (navigatorKey?.currentState != null)
          ? navigatorKey!.currentContext!
          : context;
      if (!ctx.mounted) return;
      await showDialog(
        context: ctx,
        barrierDismissible: false,
        barrierColor: Colors.black87,
        builder: (_) => const _SetupSheet(),
      );
    } catch (e) {
      debugPrint('[SourceSetup] show failed: $e');
    }
  }

  static SourceManifest _manifest(String id) => SourceManifest(
        id: id, name: _names[id] ?? id, version: '1.0.0', author: '',
        description: '', icon: '', repositoryUrl: '', downloadUrl: '',
        minAppVersion: '', capabilities: const [], downloads: 0, rating: 0,
        networkType: '',
      );

  /// 一键安装全部白名单源
  static Future<int> installAll(void Function(int done, int total, String name) onProgress) async {
    final dir = await SourceInstaller.ensureSourceDir();
    if (dir == null) return 0;
    final prefs = await SharedPreferences.getInstance();
    final list = (jsonDecode(prefs.getString('installed_sources') ?? '[]') as List).toList();
    var ok = 0;
    final total = SourceInstaller.vettedSources.length;
    for (var i = 0; i < total; i++) {
      final id = SourceInstaller.vettedSources[i];
      onProgress(i, total, _names[id] ?? id);
      final m = _manifest(id);
      if (await SourceInstaller.install(m, dir)) {
        if (!list.any((e) { try { return SourceManifest.fromJson(e as Map<String, dynamic>).id == id; } catch (_) { return false; } })) {
          list.add(m.toJson());
        }
        ok++;
      }
    }
    await prefs.setString('installed_sources', jsonEncode(list));
    await prefs.setBool(_flag, true);
    return ok;
  }
}

class _SetupSheet extends StatefulWidget {
  const _SetupSheet();
  @override
  State<_SetupSheet> createState() => _SetupSheetState();
}

class _SetupSheetState extends State<_SetupSheet> {
  bool _installing = false;
  int _done = 0, _total = SourceInstaller.vettedSources.length;
  String _current = '';
  int _okCount = 0;

  Future<void> _start() async {
    setState(() => _installing = true);
    final ok = await SourceSetupDialog.installAll((d, t, n) {
      if (mounted) setState(() { _done = d; _total = t; _current = n; });
    });
    if (!mounted) return;
    setState(() { _installing = false; _okCount = ok; });
    Navigator.of(context).pop(ok);
  }

  @override
  Widget build(BuildContext context) {
    return Dialog(
      backgroundColor: DS.surface2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(DS.rLg)),
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(color: DS.accentDim, borderRadius: BorderRadius.circular(DS.rMd)),
              child: const Icon(Icons.auto_stories_rounded, color: DS.accent, size: 24),
            ),
            const SizedBox(width: 12),
            const Expanded(child: Text('检测到尚未安装漫画源', style: TextStyle(color: DS.textPrimary, fontSize: 17, fontWeight: FontWeight.w700))),
          ]),
          const SizedBox(height: 14),
          Text(
            _installing ? '正在安装：$_current ($_done/$_total)' : '一键安装 ${SourceInstaller.vettedSources.length} 个精选漫画源，完成后即可在「发现」页浏览。海外源需自行开启 VPN 直连。',
            style: const TextStyle(color: DS.textSecondary, fontSize: 13.5, height: 1.5),
          ),
          if (_installing) ...[
            const SizedBox(height: 16),
            ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value: _total == 0 ? null : _done / _total,
                backgroundColor: DS.surface3,
                color: DS.accent,
                minHeight: 4,
              ),
            ),
          ],
          if (_okCount > 0) ...[
            const SizedBox(height: 14),
            Row(children: [
              const Icon(Icons.check_circle_rounded, color: Color(0xFF34D399), size: 18),
              const SizedBox(width: 6),
              Text('成功安装 $_okCount 个源', style: const TextStyle(color: Color(0xFF34D399), fontSize: 13, fontWeight: FontWeight.w600)),
            ]),
          ],
          const SizedBox(height: 22),
          Row(children: [
            Expanded(
              child: TextButton(
                onPressed: _installing ? null : () => Navigator.of(context).pop(0),
                style: TextButton.styleFrom(foregroundColor: DS.textTertiary),
                child: const Text('暂不'),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              flex: 2,
              child: FilledButton(
                onPressed: _installing ? null : _start,
                style: FilledButton.styleFrom(
                  backgroundColor: DS.accent,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 13),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(DS.rMd)),
                ),
                child: _installing
                    ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                    : const Text('一键安装', style: TextStyle(fontWeight: FontWeight.w700)),
              ),
            ),
          ]),
        ]),
      ),
    );
  }
}