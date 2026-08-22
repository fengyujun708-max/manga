import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'package:get_it/get_it.dart';
import 'package:dio/dio.dart';
import '../../../app/theme/theme.dart';
import '../../../app/widgets/comic_widgets.dart';
import '../bloc/auth_bloc.dart';

/// Apple 液态玻璃登录页 — 极简深沉 + 毛玻璃输入 + 一键游客登录
class LoginPage extends StatefulWidget {
  const LoginPage({super.key});
  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> with SingleTickerProviderStateMixin {
  final _phoneCtrl = TextEditingController(text: 'guest');
  final _passwordCtrl = TextEditingController(text: 'guest');
  final _confirmCtrl = TextEditingController();
  final _nameCtrl = TextEditingController();
  bool _isRegister = false;
  late AnimationController _entranceCtrl;
  late Animation<double> _entrance;

  @override
  void initState() {
    super.initState();
    _entranceCtrl = AnimationController(duration: const Duration(milliseconds: 800), vsync: this);
    _entrance = CurvedAnimation(parent: _entranceCtrl, curve: Curves.easeOutCubic);
    _entranceCtrl.forward();
  }

  @override
  void dispose() {
    _phoneCtrl.dispose(); _passwordCtrl.dispose();
    _confirmCtrl.dispose(); _nameCtrl.dispose();
    _entranceCtrl.dispose();
    super.dispose();
  }

  void _guestLogin() {
    context.read<AuthBloc>().add(AuthGuestLoginRequested());
  }

  void _submit() {
    if (_isRegister) {
      context.read<AuthBloc>().add(AuthRegisterRequested(
        phone: _phoneCtrl.text.trim(),
        password: _passwordCtrl.text,
        confirmPassword: _confirmCtrl.text,
        nickname: _nameCtrl.text.trim().isEmpty ? '用户${_phoneCtrl.text.trim().substring(0, 4)}' : _nameCtrl.text.trim(),
      ));
    } else {
      context.read<AuthBloc>().add(AuthLoginRequested(
        _phoneCtrl.text.trim(),
        _passwordCtrl.text,
      ));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      body: BlocListener<AuthBloc, AuthState>(
        listenWhen: (p, c) => c is AuthAuthenticated || c is AuthError,
        listener: (ctx, s) {
          if (s is AuthAuthenticated) context.go('/home');
          if (s is AuthError) {
            ScaffoldMessenger.of(ctx).showSnackBar(SnackBar(
              backgroundColor: AppTheme.destructive,
              content: Row(children: [
                const Icon(Icons.error_outline_rounded, color: Colors.white, size: 18),
                const SizedBox(width: 8),
                Expanded(child: Text(s.message, style: const TextStyle(color: Colors.white, fontSize: 13))),
              ]),
              behavior: SnackBarBehavior.floating,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            ));
          }
        },
        child: BlocBuilder<AuthBloc, AuthState>(
          builder: (ctx, state) {
            final loading = state is AuthLoading;
            return SafeArea(
              child: FadeTransition(
                opacity: _entrance,
                child: Center(
                  child: SingleChildScrollView(
                    physics: const BouncingScrollPhysics(),
                    padding: const EdgeInsets.symmetric(horizontal: 28),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        // Logo
                        Container(
                          width: 80, height: 80,
                          decoration: BoxDecoration(
                            gradient: AppTheme.primaryGradient,
                            borderRadius: BorderRadius.circular(22),
                            boxShadow: [BoxShadow(color: AppTheme.primary.withValues(alpha: 0.3), blurRadius: 24, offset: const Offset(0, 8))],
                          ),
                          child: const Icon(Icons.auto_stories_rounded, size: 38, color: Colors.white),
                        ),
                        const SizedBox(height: 16),
                        const Text('漫界', style: TextStyle(fontSize: 28, fontWeight: FontWeight.w800, color: AppTheme.textPrimary, letterSpacing: -1)),
                        const SizedBox(height: 6),
                        const Text('海量漫画，一触即达', style: TextStyle(fontSize: 14, color: AppTheme.textTertiary)),
                        const SizedBox(height: 40),

                        // 液态玻璃表单
                        LiquidGlass(
                          radius: BorderRadius.circular(AppTheme.radiusLg),
                          padding: const EdgeInsets.all(24),
                          fillColor: AppTheme.glassFillRegular,
                          child: Column(children: [
                            // 手机号
                            _GlassField(
                              controller: _phoneCtrl,
                              hint: '手机号 / 账号',
                              icon: Icons.phone_iphone_rounded,
                              enabled: !loading,
                            ),
                            const SizedBox(height: 14),
                            // 密码
                            _GlassField(
                              controller: _passwordCtrl,
                              hint: '密码',
                              icon: Icons.lock_outline_rounded,
                              obscure: true,
                              enabled: !loading,
                            ),
                            if (_isRegister) ...[
                              const SizedBox(height: 14),
                              _GlassField(
                                controller: _confirmCtrl,
                                hint: '确认密码',
                                icon: Icons.lock_outline_rounded,
                                obscure: true,
                                enabled: !loading,
                              ),
                              const SizedBox(height: 14),
                              _GlassField(
                                controller: _nameCtrl,
                                hint: '昵称（可选）',
                                icon: Icons.person_outline_rounded,
                                enabled: !loading,
                              ),
                            ],
                            const SizedBox(height: 24),
                            // 登录按钮
                            SpringButton(
                              onPressed: loading ? null : _submit,
                              child: loading
                                ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2.5, color: Colors.white))
                                : Text(_isRegister ? '注册' : '登录', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: Colors.white)),
                            ),
                            const SizedBox(height: 14),
                            // 切换登录/注册
                            GestureDetector(
                              onTap: loading ? null : () => setState(() => _isRegister = !_isRegister),
                              child: Text(
                                _isRegister ? '已有账号？去登录' : '没有账号？去注册',
                                style: const TextStyle(fontSize: 13, color: AppTheme.textTertiary),
                              ),
                            ),
                          ]),
                        ),

                        const SizedBox(height: 24),

                        // 游客登录
                        GestureDetector(
                          onTap: loading ? null : _guestLogin,
                          child: Container(
                            width: double.infinity,
                            padding: const EdgeInsets.symmetric(vertical: 16),
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(AppTheme.radiusMd),
                              border: Border.all(color: AppTheme.glassBorder, width: 0.5),
                            ),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Icon(Icons.person_outline_rounded, size: 18, color: AppTheme.textTertiary),
                                const SizedBox(width: 8),
                                const Text('游客登录', style: TextStyle(fontSize: 15, color: AppTheme.textSecondary, fontWeight: FontWeight.w500)),
                              ],
                            ),
                          ),
                        ),
                        const SizedBox(height: 40),
                      ],
                    ),
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}

class _GlassField extends StatelessWidget {
  final TextEditingController controller;
  final String hint;
  final IconData icon;
  final bool obscure;
  final bool enabled;
  const _GlassField({required this.controller, required this.hint, required this.icon, this.obscure = false, this.enabled = true});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppTheme.surfaceLight.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppTheme.glassBorder, width: 0.5),
      ),
      child: TextField(
        controller: controller,
        obscureText: obscure,
        enabled: enabled,
        style: const TextStyle(fontSize: 15, color: AppTheme.textPrimary),
        decoration: InputDecoration(
          hintText: hint,
          hintStyle: const TextStyle(fontSize: 15, color: AppTheme.textTertiary),
          prefixIcon: Icon(icon, size: 20, color: AppTheme.textTertiary),
          border: InputBorder.none,
          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
        ),
      ),
    );
  }
}