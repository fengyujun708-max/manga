import {
  Injectable, UnauthorizedException, BadRequestException,
  ConflictException, Logger,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Cron, CronExpression } from '@nestjs/schedule';
import * as bcrypt from 'bcryptjs';
import { v4 as uuidv4 } from 'uuid';

import { User, UserRole, UserStatus, UserSession, LoginSession } from '../user/entities/user.entity';
import { CaptchaService } from './captcha.service';

@Injectable()
export class AuthService {
  private readonly logger = new Logger(AuthService.name);

  constructor(
    @InjectRepository(User)
    private userRepo: Repository<User>,
    @InjectRepository(UserSession)
    private sessionRepo: Repository<UserSession>,
    @InjectRepository(LoginSession)
    private loginSessionRepo: Repository<LoginSession>,
    private jwtService: JwtService,
    private config: ConfigService,
    private captchaService: CaptchaService,
  ) {}

  // ====== 注册（图片验证码） ======

  async register(phone: string, password: string, confirmPassword: string, nickname: string) {
    // 密码一致性校验
    if (password !== confirmPassword) {
      throw new BadRequestException('两次密码输入不一致');
    }

    // 手机号格式校验
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      throw new BadRequestException('手机号格式不正确（需要11位手机号）');
    }

    const existing = await this.userRepo.findOneBy({ phone });
    if (existing) throw new ConflictException('手机号已注册');

    const passwordHash = await bcrypt.hash(password, 10);

    const user = await this.userRepo.save({
      phone,
      passwordHash,
      nickname,
      phoneVerified: true,
      role: UserRole.USER,
      status: UserStatus.ACTIVE,
    });

    return this.generateTokens(user);
  }

  // ====== 登录 ======

  async login(phone: string, password: string, ip: string = "") {
    const user = await this.userRepo.findOne({
      where: { phone },
      select: ['id', 'phone', 'passwordHash', 'nickname', 'avatar', 'role', 'status'],
    });

    if (!user) {
      await this.logLogin(phone, null, ip, false, '用户不存在');
      throw new UnauthorizedException('手机号或密码错误');
    }

    if (user.status !== UserStatus.ACTIVE) {
      throw new UnauthorizedException('账号已被禁用');
    }

    const valid = await bcrypt.compare(user.passwordHash, password);
    if (!valid) {
      await this.logLogin(phone, user.id, ip, false, '密码错误');
      throw new UnauthorizedException('手机号或密码错误');
    }

    await this.userRepo.update(user.id, { lastLoginAt: new Date(), lastLoginIp: ip });
    await this.logLogin(phone, user.id, ip, true);

    return this.generateTokens(user);
  }

  async smsLogin(phone: string, code: string, ip: string = '') {
    // 使用图片验证码替代短信验证码
    const valid = await this.captchaService.verify(code, code);
    if (!valid) throw new BadRequestException('验证码错误');

    let user = await this.userRepo.findOneBy({ phone });
    if (!user) {
      // 自动注册
      user = await this.userRepo.save({
        phone,
        passwordHash: '',
        nickname: `用户${phone.slice(-4)}`,
        phoneVerified: true,
        role: UserRole.USER,
        status: UserStatus.ACTIVE,
      });
    }

    await this.userRepo.update(user.id, { lastLoginAt: new Date(), lastLoginIp: ip });
    return this.generateTokens(user);
  }

  // ====== Token 管理 ======

  private async generateTokens(user: User) {
    const payload = { sub: user.id, phone: user.phone, role: user.role };

    const accessToken = this.jwtService.sign(payload, {
      secret: this.config.get('JWT_SECRET', 'manjie-secret-dev'),
      expiresIn: '15m',
    });

    const refreshToken = uuidv4();
    const expiresAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);

    await this.sessionRepo.save({
      userId: user.id,
      refreshToken: await bcrypt.hash(refreshToken, 10),
      deviceId: 'default',
      expiresAt,
    });

    return {
      accessToken,
      refreshToken,
      expiresIn: 900,
      user: {
        id: user.id,
        phone: user.phone,
        nickname: user.nickname,
        avatar: user.avatar,
        role: user.role,
      },
    };
  }

  async refreshTokens(refreshToken: string) {
    const sessions = await this.sessionRepo.find({
      where: { isRevoked: false },
      select: ['id', 'userId', 'refreshToken', 'expiresAt'],
    });

    for (const session of sessions) {
      const valid = await bcrypt.compare(session.refreshToken, refreshToken);
      if (valid) {
        if (session.expiresAt < new Date()) {
          throw new UnauthorizedException('Refresh token 已过期');
        }
        const user = await this.userRepo.findOneBy({ id: session.userId });
        if (!user) throw new UnauthorizedException('用户不存在');

        // 轮换 refresh token
        await this.sessionRepo.update(session.id, { isRevoked: true });
        return this.generateTokens(user);
      }
    }
    throw new UnauthorizedException('Invalid refresh token');
  }

  async logout(userId: string, deviceId?: string) {
    const query: any = { userId };
    if (deviceId) query.deviceId = deviceId;
    await this.sessionRepo.update(query, { isRevoked: true });
  }

  async logoutAllDevices(userId: string) {
    await this.sessionRepo.update({ userId }, { isRevoked: true });
  }

  // ====== 密码管理 ======

  async resetPassword(phone: string, code: string, newPassword: string) {
    // 使用图片验证码
    const valid = await this.captchaService.verify(code, code);
    if (!valid) throw new BadRequestException('验证码错误');

    const passwordHash = await bcrypt.hash(newPassword, 10);
    await this.userRepo.update({ phone }, { passwordHash });
    // 踢出所有设备
    const user = await this.userRepo.findOneBy({ phone });
    if (user) await this.sessionRepo.update({ userId: user.id }, { isRevoked: true });
  }

  async changePassword(userId: string, oldPassword: string, newPassword: string) {
    const user = await this.userRepo.findOne({
      where: { id: userId },
      select: ['id', 'passwordHash'],
    });
    if (!user) throw new UnauthorizedException('用户不存在');

    const valid = await bcrypt.compare(user.passwordHash, oldPassword);
    if (!valid) throw new BadRequestException('旧密码错误');

    const passwordHash = await bcrypt.hash(newPassword, 10);
    await this.userRepo.update(userId, { passwordHash });
  }

  // ====== 辅助 ======

  private async logLogin(phone: string, userId: string | null, ip: string, success: boolean, failReason?: string) {
    await this.loginSessionRepo.save({ phone, userId: userId || undefined, ip, success, failReason } as any);
  }

  // 验证码由 CaptchaService 管理
  // 清理逻辑已在 CaptchaService 中处理
}