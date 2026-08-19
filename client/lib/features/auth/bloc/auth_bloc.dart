import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import '../../core/network/api_client.dart';
import '../../core/storage/secure_storage.dart';

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
  final String code;
  final String password;
  final String nickname;
  const AuthRegisterRequested(this.phone, this.code, this.password, this.nickname);
  @override List<Object?> get props => [phone, code, password, nickname];
}

class AuthSmsLoginRequested extends AuthEvent {
  final String phone;
  final String code;
  const AuthSmsLoginRequested(this.phone, this.code);
  @override List<Object?> get props => [phone, code];
}

class AuthSendCodeRequested extends AuthEvent {
  final String phone;
  const AuthSendCodeRequested(this.phone);
  @override List<Object?> get props => [phone];
}

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
    on<AuthSmsLoginRequested>(_onSmsLogin);
    on<AuthSendCodeRequested>(_onSendCode);
    on<AuthLogoutRequested>(_onLogout);
  }

  Future<void> _onCheck(AuthCheckRequested event, Emitter<AuthState> emit) async {
    final token = await storage.read('access_token');
    if (token != null) {
      // TODO: validate token with server
      emit(AuthUnauthenticated());
    } else {
      emit(AuthUnauthenticated());
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
      await apiClient.setTokens(data['accessToken'], data['refreshToken']);
      await storage.write('user_id', data['user']['id']);
      emit(AuthAuthenticated(
        userId: data['user']['id'],
        phone: data['user']['phone'],
        nickname: data['user']['nickname'],
        avatar: data['user']['avatar'],
      ));
    } catch (e) {
      emit(AuthError('登录失败，请检查手机号和密码'));
    }
  }

  Future<void> _onRegister(AuthRegisterRequested event, Emitter<AuthState> emit) async {
    emit(AuthLoading());
    try {
      final res = await apiClient.post('/auth/register', data: {
        'phone': event.phone,
        'code': event.code,
        'password': event.password,
        'nickname': event.nickname,
      });
      final data = res.data;
      await apiClient.setTokens(data['accessToken'], data['refreshToken']);
      emit(AuthAuthenticated(
        userId: data['user']['id'],
        phone: data['user']['phone'],
        nickname: data['user']['nickname'],
      ));
    } catch (e) {
      emit(AuthError('注册失败'));
    }
  }

  Future<void> _onSmsLogin(AuthSmsLoginRequested event, Emitter<AuthState> emit) async {
    emit(AuthLoading());
    try {
      final res = await apiClient.post('/auth/sms-login', data: {
        'phone': event.phone,
        'code': event.code,
      });
      final data = res.data;
      await apiClient.setTokens(data['accessToken'], data['refreshToken']);
      emit(AuthAuthenticated(
        userId: data['user']['id'],
        phone: data['user']['phone'],
        nickname: data['user']['nickname'],
      ));
    } catch (e) {
      emit(AuthError('验证码登录失败'));
    }
  }

  Future<void> _onSendCode(AuthSendCodeRequested event, Emitter<AuthState> emit) async {
    try {
      await apiClient.post('/auth/send-code', data: {'phone': event.phone});
    } catch (e) {
      emit(AuthError('发送验证码失败'));
    }
  }

  Future<void> _onLogout(AuthLogoutRequested event, Emitter<AuthState> emit) async {
    await apiClient.clearTokens();
    await storage.clear();
    emit(AuthUnauthenticated());
  }
}