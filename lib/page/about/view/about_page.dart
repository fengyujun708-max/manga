import 'dart:async';
import 'dart:io';

import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:mangaverse/config/i18n/localization.dart';
import 'package:mangaverse/config/theme/colors.dart';
import 'package:mangaverse/config/theme/themes.dart';
import 'package:mangaverse/main.dart';
import 'package:mangaverse/config/router/router.gr.dart' as app_router;
import 'package:mangaverse/service/auth_manager.dart';
import 'package:mangaverse/service/log_uploader.dart';
import 'package:mangaverse/service/update/check_update.dart';
import 'package:mangaverse/widgets/mv_card.dart';
import 'package:url_launcher/url_launcher.dart';

@RoutePage()
class AboutPage extends StatefulWidget {
  const AboutPage({super.key});

  @override
  State<AboutPage> createState() => _AboutPageState();
}

class _AboutPageState extends State<AboutPage> {
  String _versionText = "1.0.0";
  bool _checkingUpdate = false;
  bool _sendingLog = false;

  @override
  void initState() {
    super.initState();
    _loadInfo();
  }

  Future<void> _loadInfo() async {
    try {
      final appVersion = await getApplicationVersion();
      if (mounted) {
        setState(() { _versionText = appVersion; });
      }
    } catch (_) {}
  }

  Future<void> _checkForUpdate() async {
    setState(() => _checkingUpdate = true);
    try {
      final res = await checkForUpdate();
      if (!mounted) { setState(() => _checkingUpdate = false); return; }
      if (res == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text("检查更新失败"), duration: Duration(seconds: 2)));
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(res["body"] ?? "已是最新版本"), duration: Duration(seconds: 2)));
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text("检查更新失败: \$e"), duration: Duration(seconds: 2)));
    }
    setState(() => _checkingUpdate = false);
  }

  Future<void> _sendLog() async {
    setState(() => _sendingLog = true);
    try {
      final ok = await LogUploader.instance.upload("info", "用户主动上传日志",
        extra: "device:\${Platform.operatingSystem}");
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ok ? "日志已发送" : "发送失败"), duration: Duration(seconds: 1)));
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text("发送失败: \$e"), duration: Duration(seconds: 1)));
    }
    setState(() => _sendingLog = false);
  }

  Widget _section(String title, Widget child) {
    final theme = Theme.of(context);
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      color: Colors.transparent,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(padding: const EdgeInsets.symmetric(vertical: 4), child: Column(children: [
        Padding(padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: Align(alignment: Alignment.centerLeft,
            child: Text(title, style: TextStyle(color: theme.colorScheme.secondary, fontSize: 13)))),
        Divider(height: 1, color: theme.dividerColor.withOpacity(0.2)),
        child,
      ])),
    );
  }

  Widget _userSection() {
    final session = AuthManager.instance.session;
    final theme = Theme.of(context);
    if (session == null) {
      return _section("账号",
        ListTile(
          leading: Icon(Icons.person_outline, color: theme.colorScheme.secondary),
          title: const Text("未登录"),
          subtitle: const Text("登录后同步书架和阅读记录"),
          onTap: () => context.router.push(const app_router.LoginRoute()),
          trailing: TextButton(onPressed: () => context.router.push(const app_router.LoginRoute()),
            child: const Text("登录 / 注册")),
        ));
    }
    return _section("账号",
      ListTile(
        leading: Icon(Icons.person, color: theme.colorScheme.secondary),
        title: Text(session.username),
        subtitle: Text(session.userId.substring(0, 8)),
        trailing: TextButton(onPressed: () async {
          await AuthManager.instance.logout();
          if (mounted) setState(() {});
        }, child: const Text("退出")),
      ));
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text("关于 MangaVerse"), centerTitle: true),
      body: SingleChildScrollView(
        padding: const EdgeInsets.only(bottom: 16),
        child: Column(children: [
          const SizedBox(height: 20),
          Container(
            width: 80, height: 80,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(20),
              gradient: const LinearGradient(colors: [Color(0xFFFF6B9D), Color(0xFFFF2E63)],
                begin: Alignment.topLeft, end: Alignment.bottomRight),
            ),
            child: const Center(child: Icon(Icons.menu_book_rounded, size: 44, color: Colors.white)),
          ),
          const SizedBox(height: 12),
          Text("MangaVerse", style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: theme.colorScheme.onSurface)),
          Text("版本 \$_versionText", style: TextStyle(fontSize: 13, color: theme.colorScheme.onSurface.withOpacity(0.6))),
          const SizedBox(height: 20),

          _userSection(),

          _section("应用",
            Column(children: [
              ListTile(
                leading: Icon(Icons.info_outline, color: theme.colorScheme.secondary),
                title: const Text("应用简介"),
                subtitle: const Text("专注于漫画阅读的现代化应用"),
              ),
              const Divider(height: 1),
              ListTile(
                leading: Icon(Icons.language, color: theme.colorScheme.secondary),
                title: const Text("官方网站"),
                subtitle: const Text("39.106.192.137"),
                onTap: () async {
                  try {
                    await launchUrl(Uri.parse("http://39.106.192.137"));
                  } catch (_) {}
                },
              ),
            ])),

          _section("功能",
            ListTile(
              leading: Icon(Icons.refresh, color: theme.colorScheme.secondary),
              title: const Text("检查更新"),
              subtitle: const Text("检测新版本"),
              onTap: _checkingUpdate ? null : _checkForUpdate,
              trailing: _checkingUpdate
                ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                : null,
            )),

          _section("日志",
            ListTile(
              leading: Icon(Icons.upload_file, color: theme.colorScheme.secondary),
              title: const Text("发送日志"),
              subtitle: const Text("将诊断日志发送到服务器"),
              onTap: _sendingLog ? null : _sendLog,
              trailing: _sendingLog
                ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                : null,
            )),
        ]),
      ),
    );
  }
}
