import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'dart:ui';
import '../../../app/ds.dart';
import '../bloc/auth_bloc.dart';

/// Apple 风格登录页 — 纯黑沉浸 + 毛玻璃表单 + 一键游客
class LoginPage extends StatefulWidget {
  const LoginPage({super.key});
  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> with SingleTickerProviderStateMixin {
  final _phoneCtrl = TextEditingController(text: 'guest');
  final _pwdCtrl = TextEditingController(text: 'guest');
  final _confirmCtrl = TextEditingController();
  final _nameCtrl = TextEditingController();
  bool _isRegister = false;
  late AnimationController _ctrl;
  late Animation<double> _fade;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(duration: DS.durEmphasis, vsync: this);
    _fade = CurvedAnimation(parent: _ctrl, curve: DS.cEmphasis);
    _ctrl.forward();
  }

  @override
  void dispose() { _phoneCtrl.dispose(); _pwdCtrl.dispose(); _confirmCtrl.dispose(); _nameCtrl.dispose(); _ctrl.dispose(); super.dispose(); }

  void _submit() {
    if (_isRegister) {
      context.read<AuthBloc>().add(AuthRegisterRequested(
        phone: _phoneCtrl.text.trim(), password: _pwdCtrl.text,
        confirmPassword: _confirmCtrl.text,
        nickname: _nameCtrl.text.trim().isEmpty ? '用户${_phoneCtrl.text.trim().substring(0, 4)}' : _nameCtrl.text.trim(),
      ));
    } else {
      context.read<AuthBloc>().add(AuthLoginRequested(_phoneCtrl.text.trim(), _pwdCtrl.text));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: DS.bg,
      body: BlocListener<AuthBloc, AuthState>(
        listenWhen: (p, c) => c is AuthAuthenticated || c is AuthError,
        listener: (ctx, s) {
          if (s is AuthAuthenticated) context.go('/home');
          if (s is AuthError) {
            HapticFeedback.heavyImpact();
            ScaffoldMessenger.of(ctx).showSnackBar(SnackBar(
              backgroundColor: DS.error,
              behavior: SnackBarBehavior.floating,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(DS.rMd)),
              content: Text(s.message, style: const TextStyle(color: Colors.white)),
            ));
          }
        },
        child: BlocBuilder<AuthBloc, AuthState>(
          builder: (ctx, state) {
            final loading = state is AuthLoading;
            return SafeArea(
              child: FadeTransition(
                opacity: _fade,
                child: Center(
                  child: SingleChildScrollView(
                    physics: const BouncingScrollPhysics(),
                    padding: const EdgeInsets.symmetric(horizontal: 28),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        // Logo
                        Container(
                          width: 72, height: 72,
                          decoration: BoxDecoration(
                            color: DS.surface1,
                            borderRadius: BorderRadius.circular(20),
                            border: Border.all(color: DS.glassBorder, width: 0.5),
                          ),
                          child: const Icon(Icons.auto_stories_rounded, size: 32, color: DS.textPrimary),
                        ),
                        const SizedBox(height: 20),
                        Text('漫界', style: DS.display),
                        const SizedBox(height: 6),
                        const Text('海量漫画，一触即达', style: DS.bodySec),
                        const SizedBox(height: 36),

                        // 毛玻璃表单
                        Glass(
                          radius: DS.rLg,
                          padding: const EdgeInsets.all(24),
                          fill: DS.glassFill,
                          child: Column(
                            children: [
                              _field(_phoneCtrl, '手机号 / 账号', Icons.person_outline_rounded, enabled: !loading),
                              const SizedBox(height: 12),
                              _field(_pwdCtrl, '密码', Icons.lock_outline_rounded, obscure: true, enabled: !loading),
                              if (_isRegister) ...[
                                const SizedBox(height: 12),
                                _field(_confirmCtrl, '确认密码', Icons.lock_outline_rounded, obscure: true, enabled: !loading),
                                const SizedBox(height: 12),
                                _field(_nameCtrl, '昵称（可选）', Icons.badge_outlined, enabled: !loading),
                              ],
                              const SizedBox(height: 24),
                              SpringButton(
                                onPressed: loading ? null : _submit,
                                child: loading
                                  ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                                  : Text(_isRegister ? '注册' : '登录'),
                              ),
                              const SizedBox(height: 12),
                              GestureDetector(
                                onTap: loading ? null : () => setState(() => _isRegister = !_isRegister),
                                child: Text(
                                  _isRegister ? '已有账号？去登录' : '没有账号？去注册',
                                  style: const TextStyle(fontSize: 13, color: DS.textTertiary),
                                ),
                              ),
                            ],
                          ),
                        ),

                        const SizedBox(height: 20),

                        // 游客登录
                        GestureDetector(
                          onTap: loading ? null : () => context.read<AuthBloc>().add(AuthGuestLoginRequested()),
                          child: Container(
                            width: double.infinity,
                            padding: const EdgeInsets.symmetric(vertical: 15),
                            decoration: BoxDecoration(
                              color: DS.surface1,
                              borderRadius: BorderRadius.circular(DS.rMd),
                              border: Border.all(color: DS.glassBorder, width: 0.5),
                            ),
                            child: const Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Icon(Icons.person_outline_rounded, size: 18, color: DS.textSecondary),
                                SizedBox(width: 8),
                                Text('游客登录', style: TextStyle(fontSize: 15, color: DS.textSecondary, fontWeight: FontWeight.w500)),
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

  Widget _field(TextEditingController ctrl, String hint, IconData icon,
      {bool obscure = false, bool enabled = true}) {
    return Container(
      decoration: BoxDecoration(
        color: DS.surface2,
        borderRadius: BorderRadius.circular(DS.rMd),
        border: Border.all(color: DS.glassBorder, width: 0.5),
      ),
      child: TextField(
        controller: ctrl, obscureText: obscure, enabled: enabled,
        style: const TextStyle(fontSize: 15, color: DS.textPrimary),
        decoration: InputDecoration(
          hintText: hint, hintStyle: const TextStyle(fontSize: 15, color: DS.textTertiary),
          prefixIcon: Icon(icon, size: 20, color: DS.textTertiary),
          border: InputBorder.none,
          contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 16),
        ),
      ),
    );
  }
}