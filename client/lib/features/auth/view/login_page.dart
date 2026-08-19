import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:dio/dio.dart';
import '../../../app/theme/theme.dart';
import '../../../app/components/manjie_button.dart';
import '../../../app/config/app_config.dart';
import '../bloc/auth_bloc.dart';

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
  final _captchaController = TextEditingController();
  bool _showPassword = false;
  bool _isRegister = false;

  // 图片验证码
  String _captchaId = '';
  String _captchaSvg = '';

  // 表单错误
  String? _phoneError;
  String? _passwordError;
  String? _confirmError;
  String? _captchaError;

  @override
  void initState() {
    super.initState();
    _loadCaptcha();
  }

  @override
  void dispose() {
    _phoneController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    _nicknameController.dispose();
    _captchaController.dispose();
    super.dispose();
  }

  Future<void> _loadCaptcha() async {
    try {
      final dio = Dio(BaseOptions(baseUrl: AppConfig.apiBaseUrl));
      final res = await dio.get('/auth/captcha');
      setState(() {
        _captchaId = res.data['id'];
        _captchaSvg = res.data['svg'];
      });
    } catch (e) {
      // 使用默认验证码
      _captchaId = 'default';
    }
  }

  bool _validatePhone(String phone) {
    return RegExp(r'^1[3-9]\d{9}$').hasMatch(phone);
  }

  bool _validate() {
    bool valid = true;
    setState(() {
      _phoneError = null;
      _passwordError = null;
      _confirmError = null;
      _captchaError = null;
    });

    final phone = _phoneController.text.trim();
    if (!_validatePhone(phone)) {
      _phoneError = '手机号格式不正确（需要11位手机号）';
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
      if (_captchaController.text.trim().length != 4) {
        _captchaError = '请输入4位验证码';
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

                // 图片验证码
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _captchaController,
                        maxLength: 4,
                        keyboardType: TextInputType.number,
                        decoration: InputDecoration(
                          labelText: '验证码',
                          prefixIcon: const Icon(Icons.security),
                          border: const OutlineInputBorder(),
                          errorText: _captchaError,
                          hintText: '输入图片中的数字',
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    GestureDetector(
                      onTap: _loadCaptcha,
                      child: Container(
                        width: 120,
                        height: 56,
                        margin: const EdgeInsets.only(top: 8),
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(color: AppTheme.divider),
                          color: Colors.white,
                        ),
                        child: _captchaSvg.isNotEmpty
                          ? ClipRRect(
                              borderRadius: BorderRadius.circular(8),
                              child: SizedBox(
                                width: 120, height: 56,
                                child: Image.network(
                                  'data:image/svg+xml;base64,${base64Encode(utf8.encode(_captchaSvg))}',
                                  fit: BoxFit.contain,
                                  errorBuilder: (_, __, ___) => const Center(
                                    child: Text('点击刷新', style: TextStyle(color: Colors.grey, fontSize: 11)),
                                  ),
                                ),
                              ),
                            )
                          : const Center(
                              child: Text('点击加载', style: TextStyle(color: Colors.grey, fontSize: 11)),
                            ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
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
              const SizedBox(height: 16),

              // 切换注册/登录
              Center(
                child: TextButton(
                  onPressed: () => setState(() {
                    _isRegister = !_isRegister;
                    _phoneError = null;
                    _passwordError = null;
                    _confirmError = null;
                    _captchaError = null;
                    if (_isRegister) _loadCaptcha();
                  }),
                  child: Text(_isRegister ? '已有账号？去登录' : '没有账号？去注册'),
                ),
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
        captchaId: _captchaId,
        captchaAnswer: _captchaController.text.trim(),
      ));
    } else {
      context.read<AuthBloc>().add(AuthLoginRequested(phone, password));
    }
  }
}

import 'dart:convert';
import 'package:flutter/foundation.dart';