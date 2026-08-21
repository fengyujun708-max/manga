import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import 'package:dio/dio.dart';
import '../../../core/network/api_client.dart';
import '../../../core/storage/secure_storage.dart';

// Events
abstract class AuthEvent extends Equatable {
  const AuthEvent();
  @override List<Object?> get props => [];
}

class AuthCheckRequested extends AuthEvent {}
class AuthLoginRequested extends AuthEvent {
  final String phone;
  final String password;
  const AuthLoginRequested(this.phone, this.password);
  @override List<Object?> get props => [phone, password];
}

class AuthRegisterRequested extends AuthEvent {
  final String phone;
  final String password;
  final String confirmPassword;
  final String nickname;
  const AuthRegisterRequested({
    required this.phone,
    required this.password,
    required this.confirmPassword,
    required this.nickname,
  });
  @override List<Object?> get props => [phone, password, confirmPassword, nickname];
}

class AuthGuestLoginRequested extends AuthEvent {}

class AuthLogoutRequested extends AuthEvent {}

// States
abstract class AuthState extends Equatable {
  const AuthState();
  @override List<Object?> get props => [];
}

class AuthInitial extends AuthState {}
class AuthLoading extends AuthState {}
class AuthAuthenticated extends AuthState {
  final String userId;
  final String phone;
  final String nickname;
  final String? avatar;
  const AuthAuthenticated({required this.userId, required this.phone, required this.nickname, this.avatar});
  @override List<Object?> get props => [userId, phone, nickname, avatar];
}
class AuthUnauthenticated extends AuthState {}
class AuthError extends AuthState {
  final String message;
  const AuthError(this.message);
  @override List<Object?> get props => [message];
}

// Bloc
class AuthBloc extends Bloc<AuthEvent, AuthState> {
  final ApiClient apiClient;
  final SecureStorage storage;

  AuthBloc({required this.apiClient, required this.storage}) : super(AuthInitial()) {
    on<AuthCheckRequested>(_onCheck);
    on<AuthLoginRequested>(_onLogin);
    on<AuthRegisterRequested>(_onRegister);
    on<AuthGuestLoginRequested>(_onGuestLogin);
    on<AuthLogoutRequested>(_onLogout);
  }

  Future<void> _onCheck(AuthCheckRequested event, Emitter<AuthState> emit) async {
    final token = await storage.read('access_token');
    if (token == null || token.isEmpty) {
      emit(AuthUnauthenticated());
      return;
    }
    // 验证 token 有效性：调 /auth/me
    try {
      await apiClient.get('/auth/me');
      // token 有效，恢复会话
      final userId = await storage.read('user_id') ?? '';
      final phone = await storage.read('user_phone') ?? '';
      final nickname = await storage.read('user_nickname') ?? '';
      final avatar = await storage.read('user_avatar');
      emit(AuthAuthenticated(
        userId: userId,
        phone: phone,
        nickname: nickname,
        avatar: avatar,
      ));
    } on DioException catch (e) {
      if (e.response?.statusCode == 401) {
        // 明确 401 = token 失效 → 清除登录态
        await apiClient.clearTokens();
        await storage.clear();
        emit(AuthUnauthenticated());
      } else {
        // 网络错误/超时 → 乐观恢复（不因网络问题登出用户）
        final userId = await storage.read('user_id') ?? '';
        final phone = await storage.read('user_phone') ?? '';
        final nickname = await storage.read('user_nickname') ?? '';
        final avatar = await storage.read('user_avatar');
        emit(AuthAuthenticated(
          userId: userId,
          phone: phone,
          nickname: nickname,
          avatar: avatar,
        ));
      }
    } catch (e) {
      // 其它错误 → 乐观恢复
      final userId = await storage.read('user_id') ?? '';
      final phone = await storage.read('user_phone') ?? '';
      final nickname = await storage.read('user_nickname') ?? '';
      final avatar = await storage.read('user_avatar');
      emit(AuthAuthenticated(
        userId: userId,
        phone: phone,
        nickname: nickname,
        avatar: avatar,
      ));
    }
  }

  Future<void> _onLogin(AuthLoginRequested event, Emitter<AuthState> emit) async {
    emit(AuthLoading());
    try {
      final res = await apiClient.post('/auth/login', data: {
        'phone': event.phone,
        'password': event.password,
      });
      final data = res.data;
      await _saveUserSession(data);
      emit(AuthAuthenticated(
        userId: data['user']['id'],
        phone: data['user']['phone'],
        nickname: data['user']['nickname'],
        avatar: data['user']['avatar'],
      ));
    } catch (e) {
      final msg = e.toString();
      if (msg.contains('已被禁用')) {
        emit(AuthError('账号已被禁用'));
      } else if (msg.contains('手机号或密码错误')) {
        emit(AuthError('手机号或密码错误'));
      } else {
        emit(AuthError('登录失败，请检查手机号和密码'));
      }
    }
  }

  Future<void> _onRegister(AuthRegisterRequested event, Emitter<AuthState> emit) async {
    emit(AuthLoading());
    try {
      final res = await apiClient.post('/auth/register', data: {
        'phone': event.phone,
        'password': event.password,
        'confirmPassword': event.confirmPassword,
        'nickname': event.nickname,
      });
      final data = res.data;
      await _saveUserSession(data);
      emit(AuthAuthenticated(
        userId: data['user']['id'],
        phone: data['user']['phone'],
        nickname: data['user']['nickname'],
      ));
    } catch (e) {
      final msg = e.toString();
      if (msg.contains('密码不一致')) {
        emit(AuthError('两次密码输入不一致'));
      } else if (msg.contains('手机号已注册')) {
        emit(AuthError('该手机号已注册，请直接登录'));
      } else if (msg.contains('验证码')) {
        emit(AuthError('注册失败，请稍后重试'));
      } else {
        emit(AuthError('注册失败，请检查信息'));
      }
    }
  }

  Future<void> _onGuestLogin(AuthGuestLoginRequested event, Emitter<AuthState> emit) async {
    emit(AuthLoading());
    try {
      final res = await apiClient.post('/auth/login', data: {'phone': 'guest', 'password': 'guest'});
      final data = res.data;
      await _saveUserSession(data, isGuest: true);
      emit(AuthAuthenticated(
        userId: data['user']['id'],
        phone: data['user']['phone'] ?? 'guest',
        nickname: data['user']['nickname'] ?? '游客',
        avatar: data['user']['avatar'],
      ));
    } catch (e) {
      emit(AuthError('游客登录失败，请稍后重试'));
    }
  }

  /// 保存用户登录会话（token + 用户信息）到本地，供下次启动恢复
  Future<void> _saveUserSession(Map<String, dynamic> data, {bool isGuest = false}) async {
    final user = data['user'] as Map<String, dynamic>? ?? {};
    await apiClient.setTokens(data['accessToken'] ?? '', data['refreshToken'] ?? '');
    await storage.write('user_id', user['id']?.toString() ?? '');
    await storage.write('user_phone', user['phone']?.toString() ?? (isGuest ? 'guest' : ''));
    await storage.write('user_nickname', user['nickname']?.toString() ?? (isGuest ? '游客' : ''));
    if (user['avatar'] != null) {
      await storage.write('user_avatar', user['avatar'].toString());
    }
    if (isGuest) {
      await storage.write('is_guest', 'true');
    } else {
      await storage.delete('is_guest');
    }
  }

  Future<void> _onLogout(AuthLogoutRequested event, Emitter<AuthState> emit) async {
    await apiClient.clearTokens();
    await storage.clear();
    emit(AuthUnauthenticated());
  }
}