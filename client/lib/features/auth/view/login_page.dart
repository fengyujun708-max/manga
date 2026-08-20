import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'package:dio/dio.dart';
import '../../../app/theme/theme.dart';
import '../../../app/config/app_config.dart';
import '../bloc/auth_bloc.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> with SingleTickerProviderStateMixin {
  final _phoneController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  final _nicknameController = TextEditingController();
  bool _showPassword = false;
  bool _isRegister = false;

  String? _phoneError;
  String? _passwordError;
  String? _confirmError;

  @override
  void dispose() {
    _phoneController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    _nicknameController.dispose();
    super.dispose();
  }

  bool _validatePhone(String phone) {
    if (!RegExp(r'^1\d{10}$').hasMatch(phone)) return false;
    if (!RegExp(r'^1[3-9]\d{9}$').hasMatch(phone)) return false;
    if (RegExp(r'^(\d)\1{10}$').hasMatch(phone)) return false;
    const inc = '01234567890123456789';
    const dec = '98765432109876543210';
    final last6 = phone.substring(5);
    if (inc.contains(last6) || dec.contains(last6)) return false;
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
      _phoneError = '手机号无效';
      valid = false;
    }

    if (_isRegister) {
      if (_passwordController.text.length < 8) {
        _passwordError = '密码至少8位';
        valid = false;
      }
      if (_passwordController.text != _confirmPasswordController.text) {
        _confirmError = '两次密码不一致';
        valid = false;
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
      body: Container(
        decoration: BoxDecoration(
          gradient: AppTheme.backgroundGradient,
        ),
        child: SafeArea(
          child: BlocListener<AuthBloc, AuthState>(
            listenWhen: (prev, curr) => curr is AuthAuthenticated || curr is AuthError,
            listener: (context, state) {
              if (state is AuthAuthenticated) {
                context.go('/home');
              } else if (state is AuthError) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text(state.message), backgroundColor: AppTheme.destructive),
                );
              }
            },
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 28),
              child: ConstrainedBox(
                constraints: BoxConstraints(
                  minHeight: MediaQuery.of(context).size.height - MediaQuery.of(context).padding.top - 56,
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    const SizedBox(height: 60),

                    // Logo — 发光渐变圆
                    Container(
                      width: 88, height: 88,
                      decoration: BoxDecoration(
                        gradient: AppTheme.primaryGradient,
                        borderRadius: BorderRadius.circular(24),
                        boxShadow: AppTheme.glowShadow,
                      ),
                      child: const Icon(Icons.auto_stories, size: 42, color: Colors.white),
                    ),
                    const SizedBox(height: 20),

                    // 标题
                    Text('漫界', style: Theme.of(context).textTheme.headlineLarge?.copyWith(
                      fontSize: 32,
                      letterSpacing: 2,
                    )),
                    const SizedBox(height: 8),
                    Text('发现漫画的无限可能',
                      style: TextStyle(color: AppTheme.textSecondary, fontSize: 14, letterSpacing: 0.5)),

                    const SizedBox(height: 40),

                    // 手机号
                    _buildField(
                      controller: _phoneController,
                      label: '手机号',
                      icon: Icons.phone_android_rounded,
                      error: _phoneError,
                      keyboardType: TextInputType.phone,
                      maxLength: 11,
                    ),
                    const SizedBox(height: 14),

                    // 昵称（注册时）
                    if (_isRegister) ...[
                      _buildField(
                        controller: _nicknameController,
                        label: '昵称',
                        icon: Icons.person_rounded,
                        hint: '给自己取个名字',
                      ),
                      const SizedBox(height: 14),
                    ],

                    // 密码
                    _buildField(
                      controller: _passwordController,
                      label: _isRegister ? '设置密码' : '密码',
                      icon: Icons.lock_outline_rounded,
                      error: _passwordError,
                      hint: _isRegister ? '至少8位' : '输入密码',
                      obscureText: !_showPassword,
                      suffix: IconButton(
                        icon: Icon(_showPassword ? Icons.visibility_off_rounded : Icons.visibility_rounded,
                            color: AppTheme.textSecondary, size: 20),
                        onPressed: () => setState(() => _showPassword = !_showPassword),
                      ),
                    ),
                    const SizedBox(height: 14),

                    // 确认密码（注册时）
                    if (_isRegister) ...[
                      _buildField(
                        controller: _confirmPasswordController,
                        label: '确认密码',
                        icon: Icons.lock_rounded,
                        error: _confirmError,
                        obscureText: !_showPassword,
                      ),
                      const SizedBox(height: 14),
                    ],

                    // 登录/注册按钮
                    BlocBuilder<AuthBloc, AuthState>(
                      builder: (context, state) {
                        final loading = state is AuthLoading;
                        return GlowButton(
                          onPressed: loading ? null : _onSubmit,
                          child: loading
                            ? const SizedBox(width: 22, height: 22,
                                child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                            : Text(_isRegister ? '注册' : '登录',
                                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600, color: Colors.white)),
                        );
                      },
                    ),
                    const SizedBox(height: 16),

                    // 游客登录
                    TextButton(
                      onPressed: () {
                        context.read<AuthBloc>().add(AuthGuestLoginRequested());
                      },
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.explore_rounded, size: 16, color: AppTheme.accent),
                          const SizedBox(width: 6),
                          Text('游客登录', style: TextStyle(color: AppTheme.accent, fontWeight: FontWeight.w500)),
                        ],
                      ),
                    ),
                    const SizedBox(height: 8),

                    // 切换注册/登录
                    TextButton(
                      onPressed: () => setState(() {
                        _isRegister = !_isRegister;
                        _phoneError = null;
                        _passwordError = null;
                        _confirmError = null;
                      }),
                      child: Text(
                        _isRegister ? '已有账号？去登录' : '没有账号？去注册',
                        style: TextStyle(color: AppTheme.textSecondary),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildField({
    required TextEditingController controller,
    required String label,
    required IconData icon,
    String? hint,
    String? error,
    bool obscureText = false,
    TextInputType? keyboardType,
    int? maxLength,
    Widget? suffix,
  }) {
    return TextField(
      controller: controller,
      obscureText: obscureText,
      keyboardType: keyboardType,
      maxLength: maxLength,
      style: const TextStyle(color: AppTheme.textPrimary, fontSize: 16),
      decoration: InputDecoration(
        labelText: label,
        hintText: hint,
        prefixIcon: Icon(icon, color: AppTheme.textSecondary, size: 22),
        suffixIcon: suffix,
        errorText: error,
        counterText: '',
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
        nickname: _nicknameController.text.trim().isNotEmpty
            ? _nicknameController.text.trim() : '用户$phone',
      ));
    } else {
      context.read<AuthBloc>().add(AuthLoginRequested(phone, password));
    }
  }
}
