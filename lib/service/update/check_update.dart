import 'dart:convert';
import 'dart:io';

import 'package:auto_route/auto_route.dart';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter/material.dart';
import 'package:markdown_widget/widget/markdown_block.dart';
import 'package:open_file/open_file.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:path/path.dart' as p;
import 'package:permission_guard/permission_guard.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:mangaverse/type/pipe.dart';
import 'package:mangaverse/util/get_path.dart';
import 'package:mangaverse/network/utils/github_proxy.dart';
import 'package:mangaverse/i18n/strings.g.dart';
import 'package:mangaverse/widgets/toast.dart';
import 'package:mangaverse/main.dart';

/// 应用更新服务器 API
const updateServerBaseUrl = 'http://39.106.192.137';
const updateApiUrl = '$updateServerBaseUrl/api/app/update';
const announcementApiUrl = '$updateServerBaseUrl/api/app/announcement';
const changelogApiUrl = '$updateServerBaseUrl/api/app/changelog';

/// 新版本信息（从服务器返回）
class UpdateInfo {
  final String version;
  final String tag;
  final String body;
  final String apkUrl;
  final String downloadUrl;
  final String releaseDate;

  UpdateInfo({
    required this.version,
    required this.tag,
    required this.body,
    required this.apkUrl,
    required this.downloadUrl,
    required this.releaseDate,
  });

  factory UpdateInfo.fromJson(Map<String, dynamic> json) {
    return UpdateInfo(
      version: json['version'] ?? '0.0.0',
      tag: json['tag'] ?? json['version'] ?? 'v0.0.0',
      body: json['body'] ?? json['releaseNotes'] ?? '',
      apkUrl: json['apkUrl'] ?? '',
      downloadUrl: json['downloadUrl'] ?? '',
      releaseDate: json['releaseDate'] ?? json['date'] ?? '',
    );
  }
}

Future<String> getAppVersion() async {
  String version = 'Unknown';
  try {
    final PackageInfo packageInfo = await PackageInfo.fromPlatform();
    version = packageInfo.version;
  } catch (e, stackTrace) {
    logger.e(e, stackTrace: stackTrace);
  }
  return version;
}

/// 从服务器获取云端版本信息
Future<UpdateInfo> getCloudVersion() async {
  try {
    final response = await fetch(updateApiUrl);
    final body = response.text.trim();
    if (response.ok && body.isNotEmpty) {
      final json = jsonDecode(body) as Map<String, dynamic>;
      return UpdateInfo.fromJson(json);
    }
  } catch (e, stackTrace) {
    logger.e('获取更新信息失败: $updateApiUrl', error: e, stackTrace: stackTrace);
  }

  // 备用: 直接从 index.json 读取版本
  try {
    final response = await fetch('$updateServerBaseUrl/api/plugins/index.json');
    final body = response.text.trim();
    if (response.ok && body.isNotEmpty) {
      final json = jsonDecode(body) as Map<String, dynamic>;
      final version = json['version'] ?? '1.0.0';
      return UpdateInfo(
        version: version.split('T').first,
        tag: 'v$version',
        body: '',
        apkUrl: '',
        downloadUrl: '',
        releaseDate: version,
      );
    }
  } catch (e) {
    logger.e(e);
  }

  throw Exception('无法获取云端版本');
}

bool isUpdateAvailable(String cloudVersion, String localVersion) {
  logger.d('App version: $localVersion\nCloud version: $cloudVersion');

  cloudVersion = cloudVersion.replaceFirst('v', '').split('+').first;
  localVersion = localVersion.split('+').first;

  final cloudParts = cloudVersion.split('.');
  final localParts = localVersion.split('.');

  for (int i = 0; i < 3; i++) {
    final cloudPart = int.tryParse(cloudParts[i]) ?? 0;
    final localPart = int.tryParse(localParts[i]) ?? 0;
    if (cloudPart > localPart) return true;
    if (cloudPart < localPart) return false;
  }
  return false;
}

/// 从服务器下载 APK 并安装
Future<void> installApk(String apkUrl) async {
  if (await _requestInstallPackagesPermission()) {
    try {
      final tempDir = await getCachePath();
      final apkFilePath = p.join(tempDir, 'app.apk');

      final response = await fetch(apkUrl);
      await File(apkFilePath).writeAsBytes(response.body);

      OpenFile.open(apkFilePath);
    } catch (e) {
      showErrorToast(t.update.apkDownloadFailed);
    }
  } else {
    showErrorToast(t.update.installPermissionRequired);
  }
}

Future<bool> _requestInstallPackagesPermission() async {
  if (Platform.isAndroid) {
    var status = await Permission.requestInstallPackages.status;
    if (status.isGranted) return true;
    if (status.isDenied) {
      return (await Permission.requestInstallPackages.request()).isGranted;
    }
  }
  return false;
}

/// 检查更新并弹窗
Future<String> checkUpdate(BuildContext context) async {
  if (!context.mounted) return '';
  try {
    final updateInfo = await getCloudVersion();
    final String localVersion = await getAppVersion();

    if (!isUpdateAvailable(updateInfo.version, localVersion)) {
      return '当前已是最新版本';
    }

    if (!context.mounted) return '';

    showDialog(
      context: context,
      builder: (context) {
        return _UpdateDialog(updateInfo: updateInfo);
      },
    );

    return '有新版本可用';
  } catch (e) {
    logger.e('检查更新失败', error: e);
    return '检测失败，请稍后重试';
  }
}

/// 更新弹窗
class _UpdateDialog extends StatelessWidget {
  final UpdateInfo updateInfo;

  const _UpdateDialog({required this.updateInfo});

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(t.update.newVersion),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(updateInfo.tag, style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 8),
            if (updateInfo.releaseDate.isNotEmpty)
              Text(updateInfo.releaseDate, style: Theme.of(context).textTheme.bodySmall),
            const SizedBox(height: 16),
            MarkdownBlock(data: '# ${updateInfo.tag}\n${updateInfo.body}'),
          ],
        ),
      ),
      actions: [
        TextButton(
          child: Text(t.common.cancel),
          onPressed: () => context.pop(),
        ),
        if (updateInfo.downloadUrl.isNotEmpty)
          TextButton(
            child: Text(t.update.goToGitHub),
            onPressed: () {
              launchUrl(Uri.parse(updateInfo.downloadUrl));
              context.pop();
            },
          ),
        if (Platform.isAndroid && updateInfo.apkUrl.isNotEmpty)
          TextButton(
            child: Text(t.update.downloadInstall),
            onPressed: () async {
              context.pop();
              await installApk(updateInfo.apkUrl);
            },
          ),
      ],
    );
  }
}
