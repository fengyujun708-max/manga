import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:dio/dio.dart';
import 'package:go_router/go_router.dart';
import '../../../app/theme/theme.dart';
import '../../../app/components/manjie_button.dart';
import '../../../app/config/app_config.dart';
import '../bloc/auth_bloc.dart';
import 'dart:convert';
import 'package:flutter/foundation.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _phoneController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  final _nicknameController = TextEditingController();
  bool _showPassword = false;
  bool _isRegister = false;

  // 表单错误
  String? _phoneError;
  String? _passwordError;
  String? _confirmError;

  @override
  void initState() {
    super.initState();
  }

  @override
  void dispose() {
    _phoneController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    _nicknameController.dispose();
    super.dispose();
  }

  bool _validatePhone(String phone) {
    // 1. 基本格式
    if (!RegExp(r'^1\d{10}$').hasMatch(phone)) return false;
    // 2. 运营商号段
    if (!RegExp(r'^1[3-9]\d{9}$').hasMatch(phone)) return false;
    // 3. 不能全是相同数字
    if (RegExp(r'^(\d)\1{10}$').hasMatch(phone)) return false;
    // 4. 不能是连续数字
    const inc = '01234567890123456789';
    const dec = '98765432109876543210';
    final last6 = phone.substring(5);
    if (inc.contains(last6) || dec.contains(last6)) return false;
    // 5. 常见假号黑名单
    const blacklist = ['13800138000', '13900139000', '10000000000', '12345678901', '11111111111', '00000000000', '12312312345'];
    if (blacklist.contains(phone)) return false;
    return true;
  }

  bool _validate() {
    bool valid = true;
    setState(() {
      _phoneError = null;
      _passwordError = null;
      _confirmError = null;
    });

    final phone = _phoneController.text.trim();
    if (!_validatePhone(phone)) {
      _phoneError = '手机号无效（需11位有效手机号，不能是假号/连号/重号）';
      valid = false;
    }

    if (_isRegister) {
      final password = _passwordController.text;
      if (password.length < 8) {
        _passwordError = '密码至少8位';
        valid = false;
      }
      if (_passwordController.text != _confirmPasswordController.text) {
        _confirmError = '两次密码输入不一致';
        valid = false;
      }
      if (_nicknameController.text.trim().isEmpty) {
        // 默认昵称
      }
    } else {
      if (_passwordController.text.isEmpty) {
        _passwordError = '请输入密码';
        valid = false;
      }
    }

    return valid;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: 60),
              // Logo & Title
              Center(
                child: Column(
                  children: [
                    Container(
                      width: 80, height: 80,
                      decoration: BoxDecoration(
                        color: Theme.of(context).colorScheme.primary,
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: const Icon(Icons.auto_stories, size: 40, color: Colors.white),
                    ),
                    const SizedBox(height: 16),
                    Text('漫界', style: Theme.of(context).textTheme.headlineLarge),
                    const SizedBox(height: 8),
                    const Text('发现漫画的无限可能', style: TextStyle(color: AppTheme.textSecondary)),
                  ],
                ),
              ),
              const SizedBox(height: 48),

              // 手机号
              TextField(
                controller: _phoneController,
                keyboardType: TextInputType.phone,
                maxLength: 11,
                decoration: InputDecoration(
                  labelText: '手机号',
                  prefixIcon: const Icon(Icons.phone_android),
                  border: const OutlineInputBorder(),
                  errorText: _phoneError,
                  hintText: '请输入11位手机号',
                ),
              ),
              const SizedBox(height: 16),

              // 昵称（注册时）
              if (_isRegister) ...[
                TextField(
                  controller: _nicknameController,
                  decoration: const InputDecoration(
                    labelText: '昵称',
                    prefixIcon: Icon(Icons.person),
                    border: OutlineInputBorder(),
                    hintText: '给自己取个名字',
                  ),
                ),
                const SizedBox(height: 16),
              ],

              // 密码
              TextField(
                controller: _passwordController,
                obscureText: !_showPassword,
                decoration: InputDecoration(
                  labelText: _isRegister ? '设置密码' : '密码',
                  prefixIcon: const Icon(Icons.lock_outline),
                  border: const OutlineInputBorder(),
                  errorText: _passwordError,
                  hintText: _isRegister ? '至少8位密码' : '输入密码',
                  suffixIcon: IconButton(
                    icon: Icon(_showPassword ? Icons.visibility_off : Icons.visibility),
                    onPressed: () => setState(() => _showPassword = !_showPassword),
                  ),
                ),
              ),
              const SizedBox(height: 16),

              // 确认密码（注册时）
              if (_isRegister) ...[
                TextField(
                  controller: _confirmPasswordController,
                  obscureText: !_showPassword,
                  decoration: InputDecoration(
                    labelText: '确认密码',
                    prefixIcon: const Icon(Icons.lock),
                    border: const OutlineInputBorder(),
                    errorText: _confirmError,
                    hintText: '再次输入密码',
                  ),
                ),
                const SizedBox(height: 16),
              ],

              // 登录按钮
              BlocBuilder<AuthBloc, AuthState>(
                builder: (context, state) {
                  final loading = state is AuthLoading;
                  return SizedBox(
                    width: double.infinity,
                    height: 50,
                    child: ElevatedButton(
                      onPressed: loading ? null : _onSubmit,
                      style: ElevatedButton.styleFrom(
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      ),
                      child: loading
                        ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2))
                        : Text(_isRegister ? '注册' : '登录', style: const TextStyle(fontSize: 16)),
                    ),
                  );
                },
              ),
              const SizedBox(height: 8),
              Center(
                child: TextButton(
                  onPressed: () {
                    context.read<AuthBloc>().add(AuthGuestLoginRequested());
                  },
                  child: const Text('游客登录', style: TextStyle(color: AppTheme.textSecondary)),
                ),
              ),
              const SizedBox(height: 8),

              // 切换注册/登录
              Center(
                child: TextButton(
                  onPressed: () => setState(() {
                    _isRegister = !_isRegister;
                    _phoneError = null;
                    _passwordError = null;
                    _confirmError = null;
                    if (_isRegister) {}
                  }),
                  child: Text(_isRegister ? '已有账号？去登录' : '没有账号？去注册'),
                ),
              ),

              // 全局监听登录状态
              BlocListener<AuthBloc, AuthState>(
                listenWhen: (prev, curr) => curr is AuthAuthenticated || curr is AuthError,
                listener: (context, state) {
                  if (state is AuthAuthenticated) {
                    context.go('/home');
                  } else if (state is AuthError) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text(state.message), backgroundColor: Colors.red[700]),
                    );
                  }
                },
                child: const SizedBox.shrink(),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _onSubmit() {
    if (!_validate()) return;

    final phone = _phoneController.text.trim();
    final password = _passwordController.text.trim();

    if (_isRegister) {
      context.read<AuthBloc>().add(AuthRegisterRequested(
        phone: phone,
        password: password,
        confirmPassword: _confirmPasswordController.text.trim(),
        nickname: _nicknameController.text.trim().isNotEmpty ? _nicknameController.text.trim() : '用户$phone',
      ));
    } else {
      context.read<AuthBloc>().add(AuthLoginRequested(phone, password));
    }
  }
}
