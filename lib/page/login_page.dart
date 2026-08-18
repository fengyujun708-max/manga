import 'dart:async';
import 'package:auto_route/auto_route.dart';
import 'package:flutter/material.dart';
import 'package:mangaverse/config/router/router.gr.dart' as app_router;
import 'package:mangaverse/service/auth_manager.dart';
import 'package:mangaverse/service/user_api.dart';
import 'package:mangaverse/widgets/toast.dart';
import 'package:mangaverse/main.dart';

@RoutePage()
class LoginPage extends StatefulWidget {
  final String? from;
  final Map<String, dynamic>? loginScheme;
  final Map<String, dynamic>? loginData;

  const LoginPage({
    super.key,
    this.from,
    this.loginScheme,
    this.loginData,
  });

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _phoneCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  String _msgCtrl = "";
  bool _loading = false;
  bool _isRegister = false;

  @override
  void dispose() {
    _phoneCtrl.dispose();
    _passwordCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() => _loading = true);
    try {
      Map<String, dynamic> result;
      if (_isRegister) {
        result = await AuthManager.instance.register(
          _phoneCtrl.text.trim(),
          _passwordCtrl.text,
        );
      } else {
        result = await AuthManager.instance.login(
          _phoneCtrl.text.trim(),
          _passwordCtrl.text,
        );
      }
      if (mounted) {
        if (result["success"] == true) {
          showSuccessToast(_isRegister ? "注册成功" : "登录成功");
          if (widget.from != null) {
            context.router.replacePath(widget.from!);
          } else {
            context.router.replace(const app_router.NavigationBar());
          }
        } else {
          setState(() => _msgCtrl = result["detail"] ?? "失败");
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() => _msgCtrl = "网络错误: $e");
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: Text(_isRegister ? '注册' : '登录')),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            TextField(
              controller: _phoneCtrl,
              keyboardType: TextInputType.phone,
              decoration: const InputDecoration(
                labelText: '手机号',
                prefixIcon: Icon(Icons.phone),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _passwordCtrl,
              obscureText: true,
              decoration: const InputDecoration(
                labelText: '密码',
                prefixIcon: Icon(Icons.lock),
              ),
            ),
            const SizedBox(height: 16),
            if (_msgCtrl.isNotEmpty)
              Text(_msgCtrl, style: TextStyle(color: theme.colorScheme.error)),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              height: 48,
              child: ElevatedButton(
                onPressed: _loading ? null : _submit,
                child: _loading
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : Text(_isRegister ? '注册' : '登录'),
              ),
            ),
            const SizedBox(height: 16),
            TextButton(
              onPressed: () => setState(() => _isRegister = !_isRegister),
              child: Text(_isRegister ? '已有账号？去登录' : '没有账号？去注册'),
            ),
          ],
        ),
      ),
    );
  }
}
