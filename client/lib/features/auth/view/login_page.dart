import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../bloc/auth_bloc.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _phoneController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _showPassword = false;
  bool _isRegister = false;

  @override
  void dispose() {
    _phoneController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: BlocListener<AuthBloc, AuthState>(
        listener: (context, state) {
          if (state is AuthError) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text(state.message), backgroundColor: Colors.red),
            );
          }
        },
        child: SafeArea(
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
                        width: 80,
                        height: 80,
                        decoration: BoxDecoration(
                          color: Theme.of(context).colorScheme.primary,
                          borderRadius: BorderRadius.circular(20),
                        ),
                        child: const Icon(Icons.auto_stories, size: 40, color: Colors.white),
                      ),
                      const SizedBox(height: 16),
                      Text('漫界', style: Theme.of(context).textTheme.headlineLarge),
                      const SizedBox(height: 8),
                      Text('发现漫画的无限可能', style: Theme.of(context).textTheme.bodyMedium),
                    ],
                  ),
                ),
                const SizedBox(height: 48),
                // Phone input
                TextField(
                  controller: _phoneController,
                  keyboardType: TextInputType.phone,
                  decoration: const InputDecoration(
                    labelText: '手机号',
                    prefixIcon: Icon(Icons.phone_android),
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 16),
                // Password input
                TextField(
                  controller: _passwordController,
                  obscureText: !_showPassword,
                  decoration: InputDecoration(
                    labelText: _isRegister ? '设置密码' : '密码',
                    prefixIcon: const Icon(Icons.lock_outline),
                    border: const OutlineInputBorder(),
                    suffixIcon: IconButton(
                      icon: Icon(_showPassword ? Icons.visibility_off : Icons.visibility),
                      onPressed: () => setState(() => _showPassword = !_showPassword),
                    ),
                  ),
                ),
                if (!_isRegister) ...[
                  const SizedBox(height: 8),
                  Align(
                    alignment: Alignment.centerRight,
                    child: TextButton(
                      onPressed: () {},
                      child: const Text('忘记密码？'),
                    ),
                  ),
                ],
                const SizedBox(height: 24),
                // Login button
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
                // Toggle register/login
                Center(
                  child: TextButton(
                    onPressed: () => setState(() => _isRegister = !_isRegister),
                    child: Text(_isRegister ? '已有账号？登录' : '没有账号？注册'),
                  ),
                ),
                const SizedBox(height: 8),
                // SMS login
                Center(
                  child: TextButton.icon(
                    onPressed: () => _showSmsLogin(context),
                    icon: const Icon(Icons.message_outlined),
                    label: const Text('验证码登录'),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  void _onSubmit() {
    final phone = _phoneController.text.trim();
    final password = _passwordController.text.trim();
    if (phone.isEmpty || password.length < 8) return;

    if (_isRegister) {
      // 先发验证码，跳转注册页面
      context.read<AuthBloc>().add(AuthSendCodeRequested(phone));
      _showRegisterDialog(context, phone, password);
    } else {
      context.read<AuthBloc>().add(AuthLoginRequested(phone, password));
    }
  }

  void _showSmsLogin(BuildContext context) {
    showModalBottomSheet(
      context: context,
      builder: (_) => _SmsLoginSheet(),
    );
  }

  void _showRegisterDialog(BuildContext context, String phone, String password) {
    // 简化：实际应跳转完整注册页
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('输入验证码'),
        content: TextField(
          decoration: const InputDecoration(labelText: '验证码', hintText: '请输入6位验证码'),
          keyboardType: TextInputType.number,
          maxLength: 6,
          onSubmitted: (code) {
            Navigator.of(ctx).pop();
            context.read<AuthBloc>().add(AuthRegisterRequested(phone, code, password, '用户$phone'));
          },
        ),
      ),
    );
  }
}

class _SmsLoginSheet extends StatefulWidget {
  @override
  State<_SmsLoginSheet> createState() => _SmsLoginSheetState();
}

class _SmsLoginSheetState extends State<_SmsLoginSheet> {
  final _phoneController = TextEditingController();
  final _codeController = TextEditingController();
  int _countdown = 0;

  @override
  void dispose() {
    _phoneController.dispose();
    _codeController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        left: 24, right: 24, top: 24,
        bottom: MediaQuery.of(context).viewInsets.bottom + 24,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('验证码登录', style: Theme.of(context).textTheme.headlineMedium),
          const SizedBox(height: 24),
          TextField(
            controller: _phoneController,
            keyboardType: TextInputType.phone,
            decoration: const InputDecoration(labelText: '手机号', border: OutlineInputBorder()),
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _codeController,
                  keyboardType: TextInputType.number,
                  maxLength: 6,
                  decoration: const InputDecoration(labelText: '验证码', border: OutlineInputBorder()),
                ),
              ),
              const SizedBox(width: 12),
              TextButton(
                onPressed: _countdown > 0 ? null : () {
                  context.read<AuthBloc>().add(AuthSendCodeRequested(_phoneController.text.trim()));
                  setState(() => _countdown = 60);
                  Future.delayed(const Duration(seconds: 1), () {});
                },
                child: Text(_countdown > 0 ? '${_countdown}s' : '获取验证码'),
              ),
            ],
          ),
          const SizedBox(height: 24),
          SizedBox(
            width: double.infinity,
            height: 50,
            child: ElevatedButton(
              onPressed: () {
                context.read<AuthBloc>().add(AuthSmsLoginRequested(
                  _phoneController.text.trim(),
                  _codeController.text.trim(),
                ));
                Navigator.of(context).pop();
              },
              style: ElevatedButton.styleFrom(shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12))),
              child: const Text('登录', style: TextStyle(fontSize: 16)),
            ),
          ),
        ],
      ),
    );
  }
}