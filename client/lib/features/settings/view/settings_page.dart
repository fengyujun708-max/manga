import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../app/theme/theme.dart';
import '../../../app/components/manjie_card.dart';
import '../../../app/components/manjie_toast.dart';

class SettingsPage extends StatefulWidget {
  const SettingsPage({super.key});

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  // 阅读设置
  bool _webtoonMode = true;
  bool _volumeButtons = false;
  bool _autoRead = false;
  double _autoReadInterval = 3.0;
  int _preloadCount = 3;
  bool _splitDualPage = false;

  // 下载设置
  int _maxDownloads = 3;
  String _imageQuality = '高清';

  // 显示设置
  bool _showProgress = true;
  bool _showPageNumber = true;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('设置')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // ====== 阅读设置 ======
          _SectionTitle('阅读设置'),
          ManjieCard(
            child: Column(
              children: [
                _SwitchTile(
                  icon: Icons.view_stream,
                  title: '瀑布流模式',
                  subtitle: '纵向连续滚动阅读，到达章节末尾自动加载下一章',
                  value: _webtoonMode,
                  onChanged: (v) => setState(() => _webtoonMode = v),
                ),
                _Divider(),
                _SwitchTile(
                  icon: Icons.volume_up,
                  title: '音量键翻页',
                  subtitle: '使用音量上下键翻页',
                  value: _volumeButtons,
                  onChanged: (v) => setState(() => _volumeButtons = v),
                ),
                _Divider(),
                _SwitchTile(
                  icon: Icons.play_circle,
                  title: '自动阅读',
                  subtitle: '自动翻页阅读',
                  value: _autoRead,
                  onChanged: (v) => setState(() => _autoRead = v),
                ),
                if (_autoRead) ...[
                  _Divider(),
                  _SliderTile(
                    icon: Icons.timer,
                    title: '自动翻页间隔',
                    value: _autoReadInterval,
                    min: 1, max: 10,
                    displayValue: '${_autoReadInterval.toInt()}秒',
                    onChanged: (v) => setState(() => _autoReadInterval = v),
                  ),
                ],
                _Divider(),
                _SelectTile(
                  icon: Icons.download,
                  title: '预加载页数',
                  value: '$_preloadCount 页',
                  onTap: () => _showPicker('预加载页数', ['1页', '2页', '3页', '5页', '10页'], _preloadCount - 1, (i) {
                    setState(() => _preloadCount = [1, 2, 3, 5, 10][i]);
                  }),
                ),
                _Divider(),
                _SwitchTile(
                  icon: Icons.view_column,
                  title: '拆分双页',
                  subtitle: '将横向双页图拆分成上下排列',
                  value: _splitDualPage,
                  onChanged: (v) => setState(() => _splitDualPage = v),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),

          // ====== 下载设置 ======
          _SectionTitle('下载设置'),
          ManjieCard(
            child: Column(
              children: [
                _SelectTile(
                  icon: Icons.speed,
                  title: '同时下载数',
                  value: '$_maxDownloads 个',
                  onTap: () => _showPicker('同时下载数', ['1个', '2个', '3个', '5个'], _maxDownloads - 1, (i) {
                    setState(() => _maxDownloads = [1, 2, 3, 5][i]);
                  }),
                ),
                _Divider(),
                _SelectTile(
                  icon: Icons.image,
                  title: '图片质量',
                  value: _imageQuality,
                  onTap: () => _showPicker('图片质量', ['原图', '高清', '标准', '压缩'], ['原图', '高清', '标准', '压缩'].indexOf(_imageQuality), (i) {
                    setState(() => _imageQuality = ['原图', '高清', '标准', '压缩'][i]);
                  }),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),

          // ====== 显示设置 ======
          _SectionTitle('显示设置'),
          ManjieCard(
            child: Column(
              children: [
                _SwitchTile(
                  icon: Icons.horizontal_rule,
                  title: '阅读进度条',
                  value: _showProgress,
                  onChanged: (v) => setState(() => _showProgress = v),
                ),
                _Divider(),
                _SwitchTile(
                  icon: Icons.numbers,
                  title: '页码显示',
                  value: _showPageNumber,
                  onChanged: (v) => setState(() => _showPageNumber = v),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),

          // ====== 缓存管理 ======
          _SectionTitle('缓存管理'),
          ManjieCard(
            child: Column(
              children: [
                _ActionTile(
                  icon: Icons.storage,
                  title: '缓存大小',
                  value: '256 MB',
                  onTap: () {},
                ),
                _Divider(),
                _ActionTile(
                  icon: Icons.delete_outline,
                  title: '清除缓存',
                  value: '',
                  textColor: Colors.red,
                  onTap: () => _showClearCacheDialog(),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),

          // ====== 关于 ======
          _SectionTitle('关于'),
          ManjieCard(
            child: Column(
              children: [
                _ActionTile(icon: Icons.info_outline, title: '版本', value: '1.0.0 (Build 1)', onTap: () {}),
                _Divider(),
                _ActionTile(icon: Icons.update, title: '检查更新', value: '', onTap: () => ManjieToast.show(context, '已是最新版本')),
                _Divider(),
                _ActionTile(icon: Icons.description, title: '开源许可', value: '', onTap: () => context.push('/licenses')),
                _Divider(),
                _ActionTile(icon: Icons.shield_outlined, title: '隐私政策', value: '', onTap: () {}),
              ],
            ),
          ),
          const SizedBox(height: 32),
        ],
      ),
    );
  }

  void _showPicker(String title, List<String> options, int selected, ValueChanged<int> onSelected) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (_) => Container(
        padding: const EdgeInsets.all(20),
        decoration: const BoxDecoration(
          color: AppTheme.surface,
          borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(width: 40, height: 4,
              decoration: BoxDecoration(color: Color(0xFF312E81), borderRadius: BorderRadius.circular(2)),
            ),
            const SizedBox(height: 16),
            Text(title, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 18, fontWeight: FontWeight.w600)),
            const SizedBox(height: 16),
            ...List.generate(options.length, (i) => ListTile(
              title: Text(options[i], style: TextStyle(color: i == selected ? AppTheme.primary : AppTheme.textPrimary)),
              trailing: i == selected ? const Icon(Icons.check, color: AppTheme.primary) : null,
              onTap: () {
                onSelected(i);
                Navigator.of(context).pop();
              },
            )),
          ],
        ),
      ),
    );
  }

  void _showClearCacheDialog() {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.surface,
        title: const Text('清除缓存'),
        content: const Text('确定要清除所有缓存数据吗？\n包括图片缓存和临时文件。'),
        actions: [
          TextButton(onPressed: () => Navigator.of(ctx).pop(), child: const Text('取消')),
          TextButton(
            onPressed: () {
              Navigator.of(ctx).pop();
              ManjieToast.show(context, '缓存已清除');
            },
            child: const Text('确定', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  final String title;
  const _SectionTitle(this.title);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8, left: 4),
      child: Text(title, style: const TextStyle(color: AppTheme.primary, fontSize: 13, fontWeight: FontWeight.w600)),
    );
  }
}

class _Divider extends StatelessWidget {
  const _Divider();

  @override
  Widget build(BuildContext context) {
    return Divider(color: Color(0xFF312E81).withOpacity(0.3), height: 1, indent: 48);
  }
}

class _SwitchTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String? subtitle;
  final bool value;
  final ValueChanged<bool> onChanged;

  const _SwitchTile({required this.icon, required this.title, this.subtitle, required this.value, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: ListTile(
        leading: Icon(icon, color: AppTheme.primary, size: 22),
        title: Text(title, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 15)),
        subtitle: subtitle != null ? Text(subtitle!, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12)) : null,
        trailing: Switch(value: value, onChanged: onChanged, activeColor: AppTheme.primary),
      ),
    );
  }
}

class _SliderTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final double value;
  final double min;
  final double max;
  final String displayValue;
  final ValueChanged<double> onChanged;

  const _SliderTile({required this.icon, required this.title, required this.value, required this.min, required this.max, required this.displayValue, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: Row(
        children: [
          Icon(icon, color: AppTheme.primary, size: 22),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 15)),
                SliderTheme(
                  data: SliderThemeData(
                    activeTrackColor: AppTheme.primary,
                    inactiveTrackColor: Color(0xFF312E81),
                    thumbColor: AppTheme.primary,
                    overlayColor: AppTheme.primary.withOpacity(0.2),
                  ),
                  child: Slider(value: value, min: min, max: max, onChanged: onChanged),
                ),
              ],
            ),
          ),
          Text(displayValue, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
        ],
      ),
    );
  }
}

class _SelectTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String value;
  final VoidCallback onTap;
  final Color? textColor;

  const _SelectTile({required this.icon, required this.title, required this.value, required this.onTap, this.textColor});

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon, color: AppTheme.primary, size: 22),
      title: Text(title, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 15)),
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(value, style: TextStyle(color: textColor ?? AppTheme.textSecondary, fontSize: 14)),
          const SizedBox(width: 4),
          const Icon(Icons.chevron_right, color: AppTheme.textSecondary, size: 20),
        ],
      ),
      onTap: onTap,
    );
  }
}

class _ActionTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String value;
  final VoidCallback onTap;
  final Color? textColor;

  const _ActionTile({required this.icon, required this.title, required this.value, required this.onTap, this.textColor});

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon, color: AppTheme.primary, size: 22),
      title: Text(title, style: TextStyle(color: textColor ?? AppTheme.textPrimary, fontSize: 15)),
      trailing: value.isNotEmpty
        ? Text(value, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 14))
        : const Icon(Icons.chevron_right, color: AppTheme.textSecondary, size: 20),
      onTap: onTap,
    );
  }
}