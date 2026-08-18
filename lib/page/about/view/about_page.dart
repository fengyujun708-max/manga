import 'dart:async';
import 'dart:io';

import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:mangaverse/i18n/i18n_helper.dart';
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
  String _version = '加载中...';
  String _buildNumber = '';
  bool _isLoading = true;
  bool _isLoggedIn = false;
  UserSession? _userSession;
  String _updateStatus = '未检查';
  String? _latestVersion;
  String? _updateUrl;

  @override
  void initState() {
    super.initState();
    _loadVersion();
    _checkLoginStatus();
  }

  Future<void> _loadVersion() async {
    try {
      final packageInfo = await PackageInfo.fromPlatform();
      setState(() {
        _version = packageInfo.version;
        _buildNumber = packageInfo.buildNumber;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _checkLoginStatus() async {
    final auth = AuthManager.instance;
    final loggedIn = auth.isLoggedIn;
    if (loggedIn) {
      setState(() {
        _isLoggedIn = true;
        _userSession = auth.session;
      });
    } else {
      setState(() {
        _isLoggedIn = false;
        _userSession = null;
      });
    }
  }

  Future<void> _checkUpdate() async {
    setState(() {
      _updateStatus = '检查中...';
    });
    try {
      final result = await checkUpdate(context);
      setState(() {
        if (result.contains('新版本') || result.contains('可用')) {
          _updateStatus = '发现新版本';
        } else {
          _updateStatus = result;
        }
      });
    } catch (e) {
      setState(() {
        _updateStatus = '检查失败: $e';
      });
    }
  }

  Future<void> _sendLog() async {
    setState(() {
      _updateStatus = '上传日志中...';
    });
    try {
      final userId = AuthManager.instance.userId;
      if (userId.isEmpty) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('请先登录')),
          );
        }
        return;
      }
      final result = await LogUploader.instance.upload(
        'info',
        '用户手动发送诊断日志',
        extra: 'AboutPage manual upload',
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(result ? '日志上传成功' : '日志上传失败')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('日志上传异常: $e')),
        );
      }
    }
    setState(() {
      _updateStatus = '完成';
    });
  }

  Future<void> _logout() async {
    await AuthManager.instance.logout();
    setState(() {
      _isLoggedIn = false;
      _userSession = null;
    });
    if (mounted) {
      context.router.replace(app_router.LoginRoute());
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: const Text('关于'),
        centerTitle: false,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 版本信息卡片
                  _buildSectionCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            const Icon(Icons.menu_book_rounded, size: 44, color: Colors.white),
                            const SizedBox(width: 12),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const Text(
                                  'MangaVerse',
                                  style: TextStyle(
                                    fontSize: 22,
                                    fontWeight: FontWeight.bold,
                                    color: Colors.white,
                                  ),
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  '版本 $_version (build $_buildNumber)',
                                  style: const TextStyle(
                                    fontSize: 14,
                                    color: Colors.white70,
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                        const SizedBox(height: 16),
                        if (_isLoggedIn && _userSession != null) ...[
                          Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: theme.colorScheme.surfaceVariant,
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  '已登录',
                                  style: TextStyle(
                                    fontSize: 14,
                                    fontWeight: FontWeight.w600,
                                    color: theme.colorScheme.primary,
                                  ),
                                ),
                                const SizedBox(height: 8),
                                Text('UID: ${_userSession!.userId}'),
                                Text('用户名: ${_userSession!.username}'),
                                const SizedBox(height: 8),
                                ElevatedButton(
                                  onPressed: _logout,
                                  style: ElevatedButton.styleFrom(
                                    backgroundColor: theme.colorScheme.error,
                                    foregroundColor: theme.colorScheme.onError,
                                    minimumSize: const Size.fromHeight(36),
                                  ),
                                  child: const Text('退出登录'),
                                ),
                              ],
                            ),
                          ),
                        ] else ...[
                          Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: theme.colorScheme.surfaceVariant,
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const Text(
                                  '未登录',
                                  style: TextStyle(
                                    fontSize: 14,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                                const SizedBox(height: 8),
                                ElevatedButton(
                                  onPressed: () => context.router.push(app_router.LoginRoute()),
                                  style: ElevatedButton.styleFrom(
                                    minimumSize: const Size.fromHeight(36),
                                  ),
                                  child: const Text('登录 / 注册'),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  // 检查更新卡片
                  _buildSectionCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          '版本更新',
                          style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                        ),
                        const SizedBox(height: 12),
                        Row(
                          children: [
                            Expanded(
                              child: Text(
                                _updateStatus,
                                style: const TextStyle(fontSize: 14),
                              ),
                            ),
                            TextButton.icon(
                              onPressed: _checkUpdate,
                              icon: const Icon(Icons.refresh, size: 18),
                              label: const Text('检查更新'),
                            ),
                          ],
                        ),
                        if (_latestVersion != null && _updateUrl != null) ...[
                          const SizedBox(height: 8),
                          Text(
                            '最新版本: $_latestVersion',
                            style: const TextStyle(fontSize: 13, color: Colors.amber),
                          ),
                          const SizedBox(height: 8),
                          ElevatedButton.icon(
                            onPressed: () => launchUrl(Uri.parse(_updateUrl!)),
                            icon: const Icon(Icons.download, size: 18),
                            label: const Text('前往下载'),
                            style: ElevatedButton.styleFrom(
                              minimumSize: const Size.fromHeight(36),
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  // 诊断工具卡片
                  _buildSectionCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          '诊断工具',
                          style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                        ),
                        const SizedBox(height: 12),
                        ElevatedButton.icon(
                          onPressed: _sendLog,
                          icon: const Icon(Icons.upload_file, size: 18),
                          label: const Text('发送诊断日志'),
                          style: ElevatedButton.styleFrom(
                            minimumSize: const Size.fromHeight(36),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  // 品牌信息
                  Center(
                    child: Text(
                      'MangaVerse — 纯净阅读体验',
                      style: TextStyle(
                        fontSize: 12,
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ),
                  const SizedBox(height: 24),
                ],
              ),
            ),
    );
  }

  Widget _buildSectionCard({required Widget child}) {
    final theme = Theme.of(context);
    return Container(
      decoration: BoxDecoration(
        color: theme.colorScheme.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: theme.colorScheme.outlineVariant),
      ),
      padding: const EdgeInsets.all(16),
      child: child,
    );
  }
}
