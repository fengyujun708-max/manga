import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mangaverse/type/pipe.dart';
import 'package:mangaverse/main.dart';

@RoutePage()
class ChangeLogPage extends StatefulWidget {
  const ChangeLogPage({super.key});

  @override
  State<ChangeLogPage> createState() => _ChangeLogPageState();
}

class _ChangeLogPageState extends State<ChangeLogPage> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('更新日志')),
      body: const Center(child: Text('暂无更新日志')),
    );
  }
}
