import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mangaverse/service/api.dart';
import 'package:mangaverse/type/pipe.dart';
import 'package:mangaverse/main.dart';

class ChangeLogPage extends StatefulWidget {
  const ChangeLogPage({super.key});

  @override
  State<ChangeLogPage> createState() => _ChangeLogPageState();
}

class _ChangeLogPageState extends State<ChangeLogPage> {
  List<Map<String, dynamic>> _records = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadChangelog();
  }

  Future<void> _loadChangelog() async {
    try {
      final data = await loadAppChangelog();
      setState(() {
        _records = data;
        _loading = false;
      });
    } catch (e) {
      logger.e('加载更新日志失败', error: e);
      setState(() {
        _error = '加载失败，请稍后重试';
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final textColor = isDark ? Colors.white : Colors.black87;

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: Icon(Icons.arrow_back_ios_new_rounded, color: textColor),
          onPressed: () => context.pop(),
        ),
        title: Text('更新日志', style: TextStyle(color: textColor)),
        backgroundColor: isDark ? Colors.black : Colors.white,
        elevation: 0,
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!, style: TextStyle(color: textColor)))
              : _records.isEmpty
                  ? Center(child: Text('暂无更新记录', style: TextStyle(color: textColor)))
                  : _buildListView(textColor),
    );
  }

  Widget _buildListView(Color textColor) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: ListView.separated(
        padding: const EdgeInsets.only(top: 16, bottom: 16),
        itemCount: _records.length,
        separatorBuilder: (_, __) => const Divider(),
        itemBuilder: (_, index) {
          final record = _records[index];
          final version = record['version'] ?? 'Unknown';
          final date = record['date'] ?? '';
          final changes = record['changes'] as List<dynamic>? ?? [];
          return _ChangeLogCard(
            version: version,
            date: date,
            changes: changes.cast<String>(),
            textColor: textColor,
          );
        },
      ),
    );
  }
}

class _ChangeLogCard extends StatelessWidget {
  final String version;
  final String date;
  final List<String> changes;
  final Color textColor;

  const _ChangeLogCard({
    required this.version,
    required this.date,
    required this.changes,
    required this.textColor,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.red.shade600,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        version,
                        style: const TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.bold,
                          fontSize: 13,
                        ),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        date.isNotEmpty ? date : '未知日期',
                        style: TextStyle(
                          color: textColor.withOpacity(0.6),
                          fontSize: 12,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                ...changes.map((change) => Padding(
                  padding: const EdgeInsets.only(top: 4),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('• ', style: TextStyle(color: textColor)),
                      Expanded(child: Text(change, style: TextStyle(color: textColor, fontSize: 13))),
                    ],
                  ),
                )),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
