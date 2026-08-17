import 'dart:async';
import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:mangaverse/config/router/router.gr.dart' as app_router;
import 'package:mangaverse/service/auth_manager.dart';
import 'package:mangaverse/service/user_api.dart';
import 'package:mangaverse/main.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _phoneCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  String _msgCtrl = "";
  bool _loading = false;
  String? _error;
  bool _isRegister = false;

  void _submit() async {
    final phone = _phoneCtrl.text.trim();
    final password = _passwordCtrl.text.trim();
    if (phone.isEmpty || password.isEmpty) {
      setState(() => _error = '手机号和密码不能为空');
      return;
    }
    if (password.length < 6) {
      setState(() => _error = '密码至少6位');
      return;
    }
    if (!_isRegister) {
      // 登录：先校验手机号格式
      final result = await userApiVerifyPhone(phone);
      if (result == null) {
        setState(() => _error = '校验失败，请检查网络');
        return;
      }
      if (result["valid"] != true) {
        setState(() => _error = result["detail"] ?? '手机号无效');
        return;
      }
    }
    setState(() { _loading = true; _error = null; });
    try {
      final result = _isRegister
          ? await AuthManager.instance.register(phone, password)
          : await AuthManager.instance.login(phone, password);
      if (result['success'] == true) {
        logger.i('登录成功: \$phone');
        if (context.mounted) context.router.replaceAll([const app_router.NavigationBar()]);
      } else {
        setState(() => _error = result['detail'] ?? '操作失败，请重试');
      }
    } catch (e) {
      setState(() => _error = '网络错误: \$e');
    }
    setState(() => _loading = false);
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return Scaffold(
      backgroundColor: isDark ? const Color(0xFF0A0A0A) : const Color(0xFFF5F5F5),
      appBar: AppBar(
        title: Text(_isRegister ? '注册账号' : '登录账号'),
        backgroundColor: Colors.transparent, elevation: 0,
        iconTheme: IconThemeData(color: isDark ? Colors.white : Colors.black87),
        titleTextStyle: TextStyle(color: isDark ? Colors.white : Colors.black87, fontSize: 20, fontWeight: FontWeight.bold),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 32),
          child: Column(children: [
            const SizedBox(height: 60),
            Container(
              width: 80, height: 80,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(20),
                gradient: const LinearGradient(colors: [Color(0xFFFF6B9D), Color(0xFFFF2E63)],
                  begin: Alignment.topLeft, end: Alignment.bottomRight),
              ),
              child: const Center(child: Icon(Icons.menu_book_rounded, size: 44, color: Colors.white)),
            ),
            const SizedBox(height: 24),
            Text(_isRegister ? '创建您的账户' : '欢迎回来',
              style: TextStyle(fontSize: 28, fontWeight: FontWeight.bold, color: isDark ? Colors.white : Colors.black87)),
            const SizedBox(height: 8),
            Text(_isRegister ? '注册后即可同步书架和阅读记录' : '登录以同步书架和阅读记录',
              style: TextStyle(fontSize: 14, color: isDark ? Colors.white54 : Colors.grey[600])),
            const SizedBox(height: 40),
            _buildField('手机号', _phoneCtrl, isPassword: false),
            const SizedBox(height: 16),
            _buildField('密码', _passwordCtrl, isPassword: true),
            if (_error != null) ...[
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                decoration: BoxDecoration(color: Colors.red.withOpacity(0.1), borderRadius: BorderRadius.circular(8)),
                child: Text(_error!, style: const TextStyle(color: Colors.red, fontSize: 13)),
              ),
            ],
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity, height: 52,
              child: ElevatedButton(
                onPressed: _loading ? null : _submit,
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFFF2E63),
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                  elevation: 0,
                ),
                child: _loading
                  ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : Text(_isRegister ? '注册' : '登录', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              ),
            ),
            const SizedBox(height: 16),
            TextButton(
              onPressed: () => setState(() => _isRegister = !_isRegister),
              child: Text(_isRegister ? '已有账号？去登录' : '没有账号？立即注册',
                style: TextStyle(color: isDark ? const Color(0xFFFF6B9D) : const Color(0xFFFF2E63))),
            ),
            const SizedBox(height: 40),
            TextButton(
              onPressed: () => context.router.replaceAll([const app_router.NavigationBar()]),
              child: Text('稍后再说', style: TextStyle(color: isDark ? Colors.white54 : Colors.grey[600])),
            ),
            const SizedBox(height: 20),
          ]),
        ),
      ),
    );
  }

  Widget _buildField(String hint, TextEditingController ctrl, {bool isPassword = false}) {
    return TextField(
      controller: ctrl,
      obscureText: isPassword,
      style: const TextStyle(color: Colors.white, fontSize: 15),
      decoration: InputDecoration(
        hintText: hint,
        hintStyle: const TextStyle(color: Colors.white38),
        filled: true,
        fillColor: const Color(0xFF1A1A1A),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      ),
      keyboardType: isPassword ? TextInputType.visiblePassword : TextInputType.phone,
    );
  }
}
