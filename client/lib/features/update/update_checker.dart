import 'package:flutter/material.dart';
import 'package:package_info_plus/package_info_plus.dart';
import '../app/components/manjie_button.dart';
import '../app/theme/theme.dart';

/// App 版本信息
class AppVersion {
  final String version;
  final int buildNumber;
  final String platform;

  const AppVersion({
    required this.version,
    required this.buildNumber,
    this.platform = 'android',
  });

  factory AppVersion.fromJson(Map<String, dynamic> json) => AppVersion(
    version: json['version'] as String,
    buildNumber: json['buildNumber'] as int,
    platform: json['platform'] as String? ?? 'android',
  );

  factory AppVersion.current() {
    // 从 PackageInfo 获取
    // 这里用模拟值
    return const AppVersion(version: '1.0.0', buildNumber: 1);
  }

  int get _versionCode {
    final parts = version.split('.').map((p) => int.parse(p)).toList();
    return parts[0] * 10000 + parts[1] * 100 + parts[2];
  }

  bool isOlderThan(AppVersion other) => _versionCode < other._versionCode;
}

/// 更新检查结果
class UpdateCheckResult {
  final bool hasUpdate;
  final bool isForceUpdate;
  final AppVersion? latestVersion;
  final String? updateUrl;
  final String? changelog;
  final String? message;

  const UpdateCheckResult({
    this.hasUpdate = false,
    this.isForceUpdate = false,
    this.latestVersion,
    this.updateUrl,
    this.changelog,
    this.message,
  });

  factory UpdateCheckResult.fromJson(Map<String, dynamic> json) => UpdateCheckResult(
    hasUpdate: json['hasUpdate'] as bool? ?? false,
    isForceUpdate: json['isForceUpdate'] as bool? ?? false,
    latestVersion: json['latestVersion'] != null
      ? AppVersion.fromJson(json['latestVersion'] as Map<String, dynamic>)
      : null,
    updateUrl: json['updateUrl'] as String?,
    changelog: json['changelog'] as String?,
    message: json['message'] as String?,
  );
}

/// 远程配置
class RemoteConfig {
  final Map<String, dynamic> _configs;

  const RemoteConfig(this._configs);

  factory RemoteConfig.fromJson(Map<String, dynamic> json) => RemoteConfig(json);

  bool getBool(String key, {bool defaultValue = false}) =>
    _configs[key] as bool? ?? defaultValue;

  int getInt(String key, {int defaultValue = 0}) =>
    _configs[key] as int? ?? defaultValue;

  double getDouble(String key, {double defaultValue = 0.0}) =>
    (_configs[key] as num?)?.toDouble() ?? defaultValue;

  String getString(String key, {String defaultValue = ''}) =>
    _configs[key] as String? ?? defaultValue;
}

/// 更新检查器
class UpdateChecker {
  /// 检查更新
  static Future<UpdateCheckResult> checkForUpdate() async {
    // TODO: 调用后端 API
    // final response = await apiClient.get('/app/check-update', params: {
    //   'version': currentVersion,
    //   'platform': platform,
    // });
    // return UpdateCheckResult.fromJson(response.data);

    // 模拟：无更新
    return const UpdateCheckResult();
  }

  /// 拉取远程配置
  static Future<RemoteConfig> fetchRemoteConfig() async {
    // TODO: 调用后端 API
    // final response = await apiClient.get('/app/config');
    // return RemoteConfig.fromJson(response.data);

    // 模拟默认配置
    return RemoteConfig({
      'registration_enabled': true,
      'community_enabled': true,
      'source_registry_url': 'https://source.manjie.xxx/registry/index.json',
      'default_reader_mode': 'webtoon',
      'max_downloads': 3,
      'maintenance_mode': false,
      'maintenance_message': '',
    });
  }
}

/// 更新对话框
Future<void> showUpdateDialog(BuildContext context, UpdateCheckResult result) {
  return showDialog(
    context: context,
    barrierDismissible: !result.isForceUpdate,
    builder: (ctx) => WillPopScope(
      onWillPop: () async => !result.isForceUpdate,
      child: AlertDialog(
        backgroundColor: AppTheme.surface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: AppTheme.primary.withOpacity(0.15),
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Icon(Icons.system_update, color: AppTheme.primary, size: 24),
            ),
            const SizedBox(width: 12),
            const Text('发现新版本', style: TextStyle(color: AppTheme.textPrimary, fontSize: 18)),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text('当前版本: ', style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
                Text(AppVersion.current().version, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 13)),
              ],
            ),
            Row(
              children: [
                Text('最新版本: ', style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
                Text(result.latestVersion?.version ?? '', style: const TextStyle(color: AppTheme.accent, fontSize: 13)),
              ],
            ),
            if (result.isForceUpdate) ...[
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: Colors.red.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: const Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.warning, size: 14, color: Colors.red),
                    SizedBox(width: 4),
                    Text('此版本已停止支持，请更新后继续使用', style: TextStyle(color: Colors.red, fontSize: 11)),
                  ],
                ),
              ),
            ],
            if (result.changelog != null) ...[
              const SizedBox(height: 12),
              const Text('更新内容:', style: TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
              const SizedBox(height: 4),
              Text(result.changelog!, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 13, height: 1.5)),
            ],
            if (result.message != null) ...[
              const SizedBox(height: 8),
              Text(result.message!, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
            ],
          ],
        ),
        actions: [
          if (!result.isForceUpdate)
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('稍后再说', style: TextStyle(color: AppTheme.textSecondary)),
            ),
          ManjieButton(
            label: '立即更新',
            width: 140,
            onPressed: () {
              // TODO: 打开下载链接
            },
          ),
        ],
      ),
    ),
  );
}

/// 维护模式页面
class MaintenancePage extends StatelessWidget {
  final String? message;
  const MaintenancePage({super.key, this.message});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: AppTheme.primary.withOpacity(0.1),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.construction, size: 64, color: AppTheme.primary),
              ),
              const SizedBox(height: 24),
              const Text('系统维护中', style: TextStyle(color: AppTheme.textPrimary, fontSize: 24, fontWeight: FontWeight.bold)),
              const SizedBox(height: 12),
              Text(
                message ?? '我们正在进行系统升级，请稍后再来',
                style: const TextStyle(color: AppTheme.textSecondary, fontSize: 16),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 24),
              ManjieButton(
                label: '刷新重试',
                icon: Icons.refresh,
                variant: ManjieButtonVariant.outlined,
                width: 160,
                onPressed: () {},
              ),
            ],
          ),
        ),
      ),
    );
  }
}