import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'package:get_it/get_it.dart';
import 'package:dio/dio.dart';
import '../../../app/theme/theme.dart';
import '../../../app/config/app_config.dart';
import '../bloc/auth_bloc.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});
  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> with TickerProviderStateMixin {
  final _phoneController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  final _nicknameController = TextEditingController();
  bool _showPassword = false;
  bool _isRegister = false;
  String? _phoneError, _passwordError, _confirmError;

  late AnimationController _fadeController;
  late Animation<double> _fade;
  late AnimationController _logoController;
  late Animation<double> _logoScale;
  late Animation<double> _logoGlow;

  @override
  void initState() {
    super.initState();
    _fadeController = AnimationController(duration: const Duration(milliseconds: 600), vsync: this);
    _fade = CurvedAnimation(parent: _fadeController, curve: Curves.easeOut);
    _logoController = AnimationController(duration: const Duration(milliseconds: 1200), vsync: this);
    _logoScale = Tween<double>(begin: 0.6, end: 1.0).animate(CurvedAnimation(parent: _logoController, curve: Curves.elasticOut));
    _logoGlow = Tween<double>(begin: 0, end: 1).animate(CurvedAnimation(parent: _logoController, curve: Curves.easeIn));
    _fadeController.forward();
    _logoController.forward();
  }

  @override
  void dispose() {
    _phoneController.dispose(); _passwordController.dispose();
    _confirmPasswordController.dispose(); _nicknameController.dispose();
    _fadeController.dispose(); _logoController.dispose();
    super.dispose();
  }

  bool _validatePhone(String p) {
    if (!RegExp(r'^1\d{10}$').hasMatch(p)) return false;
    if (!RegExp(r'^1[3-9]\d{9}$').hasMatch(p)) return false;
    if (RegExp(r'^(\d)\1{10}$').hasMatch(p)) return false;
    const inc = '01234567890123456789'; const dec = '98765432109876543210';
    final l6 = p.substring(5);
    if (inc.contains(l6) || dec.contains(l6)) return false;
    const bl = ['13800138000','13900139000','10000000000','12345678901','11111111111','00000000000','12312312345'];
    if (bl.contains(p)) return false;
    return true;
  }

  bool _validate() {
    bool ok = true;
    setState(() { _phoneError = _passwordError = _confirmError = null; });
    final p = _phoneController.text.trim();
    if (!_validatePhone(p)) { _phoneError = '手机号无效'; ok = false; }
    if (_isRegister) {
      if (_passwordController.text.length < 8) { _passwordError = '密码至少8位'; ok = false; }
      if (_passwordController.text != _confirmPasswordController.text) { _confirmError = '两次不一致'; ok = false; }
    } else {
      if (_passwordController.text.isEmpty) { _passwordError = '请输入密码'; ok = false; }
    }
    return ok;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(gradient: AppTheme.heroGradient),
        child: SafeArea(
          child: BlocListener<AuthBloc, AuthState>(
            listenWhen: (p, c) => c is AuthAuthenticated || c is AuthError,
            listener: (ctx, s) {
              if (s is AuthAuthenticated) { context.go('/home'); }
              else if (s is AuthError) {
                ScaffoldMessenger.of(ctx).showSnackBar(
                  SnackBar(content: Text(s.message), backgroundColor: AppTheme.destructive),
                );
              }
            },
            child: FadeTransition(
              opacity: _fade,
              child: SingleChildScrollView(
                padding: const EdgeInsets.symmetric(horizontal: 28),
                child: ConstrainedBox(
                  constraints: BoxConstraints(minHeight: MediaQuery.of(context).size.height - MediaQuery.of(context).padding.top - 56),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      const SizedBox(height: 50),
                      // 发光 Logo — 弹性入场
                      ScaleTransition(
                        scale: _logoScale,
                        child: Container(
                          width: 88, height: 88,
                          decoration: BoxDecoration(
                            gradient: AppTheme.primaryGradient,
                            borderRadius: BorderRadius.circular(24),
                            boxShadow: [
                              BoxShadow(
                                color: AppTheme.primary.withValues(alpha: _logoGlow.value * 0.5),
                                blurRadius: 30, spreadRadius: -4,
                              ),
                            ],
                          ),
                          child: const Icon(Icons.auto_stories_rounded, size: 42, color: Colors.white),
                        ),
                      ),
                      const SizedBox(height: 20),
                      Text('漫界', style: Theme.of(context).textTheme.headlineLarge?.copyWith(fontSize: 34, letterSpacing: 3)),
                      const SizedBox(height: 6),
                      Text('发现漫画的无限可能', style: TextStyle(color: AppTheme.textSecondary, fontSize: 13, letterSpacing: 0.8)),
                      const SizedBox(height: 36),

                      _field(_phoneController, '手机号', Icons.phone_android_rounded, error: _phoneError, kb: TextInputType.phone, maxLen: 11),
                      const SizedBox(height: 12),
                      if (_isRegister) ...[
                        _field(_nicknameController, '昵称', Icons.person_rounded, hint: '给自己取个名字'),
                        const SizedBox(height: 12),
                      ],
                      _field(_passwordController, _isRegister ? '设置密码' : '密码', Icons.lock_outline_rounded,
                        error: _passwordError, hint: _isRegister ? '至少8位' : '输入密码',
                        obscure: !_showPassword,
                        suffix: IconButton(
                          icon: Icon(_showPassword ? Icons.visibility_off_rounded : Icons.visibility_rounded, color: AppTheme.textSecondary, size: 20),
                          onPressed: () => setState(() => _showPassword = !_showPassword),
                        ),
                      ),
                      const SizedBox(height: 12),
                      if (_isRegister) ...[
                        _field(_confirmPasswordController, '确认密码', Icons.lock_rounded, error: _confirmError, obscure: !_showPassword),
                        const SizedBox(height: 12),
                      ],

                      // 登录按钮
                      BlocBuilder<AuthBloc, AuthState>(
                        builder: (ctx, s) {
                          final loading = s is AuthLoading;
                          return SpringButton(
                            onPressed: loading ? null : _onSubmit,
                            child: loading
                              ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                              : Text(_isRegister ? '注册' : '登录', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600, color: Colors.white)),
                          );
                        },
                      ),
                      const SizedBox(height: 14),

                      // 游客登录 — 胶囊按钮
                      GestureDetector(
                        onTap: () => context.read<AuthBloc>().add(AuthGuestLoginRequested()),
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 10),
                          decoration: BoxDecoration(
                            color: AppTheme.accent.withValues(alpha: 0.1),
                            borderRadius: BorderRadius.circular(20),
                            border: Border.all(color: AppTheme.accent.withValues(alpha: 0.3), width: 0.5),
                          ),
                          child: Row(mainAxisSize: MainAxisSize.min, children: [
                            Icon(Icons.explore_rounded, size: 16, color: AppTheme.accent),
                            const SizedBox(width: 6),
                            Text('游客登录', style: TextStyle(color: AppTheme.accent, fontWeight: FontWeight.w600, fontSize: 13)),
                          ]),
                        ),
                      ),
                      const SizedBox(height: 8),

                      TextButton(
                        onPressed: () => setState(() {
                          _isRegister = !_isRegister;
                          _phoneError = _passwordError = _confirmError = null;
                        }),
                        child: Text(_isRegister ? '已有账号？去登录' : '没有账号？去注册', style: TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _field(TextEditingController c, String label, IconData icon, {String? hint, String? error, bool obscure = false, TextInputType? kb, int? maxLen, Widget? suffix}) {
    return TextField(
      controller: c, obscureText: obscure, keyboardType: kb, maxLength: maxLen,
      style: const TextStyle(color: AppTheme.textPrimary, fontSize: 16),
      decoration: InputDecoration(labelText: label, hintText: hint,
        prefixIcon: Icon(icon, color: AppTheme.textSecondary, size: 22), suffixIcon: suffix, errorText: error, counterText: ''),
    );
  }

  void _onSubmit() {
    if (!_validate()) return;
    final phone = _phoneController.text.trim();
    final pw = _passwordController.text.trim();
    if (_isRegister) {
      context.read<AuthBloc>().add(AuthRegisterRequested(
        phone: phone, password: pw, confirmPassword: _confirmPasswordController.text.trim(),
        nickname: _nicknameController.text.trim().isNotEmpty ? _nicknameController.text.trim() : '用户$phone',
      ));
    } else {
      context.read<AuthBloc>().add(AuthLoginRequested(phone, pw));
    }
  }
}
