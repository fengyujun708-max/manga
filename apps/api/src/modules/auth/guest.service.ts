import { Injectable, Logger } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { v4 as uuidv4 } from 'uuid';
import { User, UserRole, UserStatus, UserSession } from '../user/entities/user.entity';

@Injectable()
export class GuestService {
  private readonly logger = new Logger(GuestService.name);

  private readonly guestLimits = {
    maxReadChapters: 20,
    maxFavorites: 10,
    noCommunity: true,
    noSync: true,
    expiresIn: '7d',
  };

  constructor(
    @InjectRepository(User)
    private userRepo: Repository<User>,
    @InjectRepository(UserSession)
    private sessionRepo: Repository<UserSession>,
    private jwtService: JwtService,
    private config: ConfigService,
  ) {}

  async createGuest(): Promise<Record<string, any>> {
    const guestId = 'guest_' + uuidv4().replace(/-/g, '').substring(0, 12);
    const phoneShort = 'g' + guestId.substring(0, 14);

    const user = await this.userRepo.save({
      phone: phoneShort,
      nickname: '游客_' + guestId.substring(0, 6),
      phoneVerified: false,
      passwordHash: '',
      role: UserRole.USER,
      status: UserStatus.ACTIVE,
    });

    const payload = { sub: user.id, phone: user.phone, role: 'guest' };
    const accessToken = this.jwtService.sign(payload, {
      secret: this.config.get('JWT_SECRET', 'manjie-secret-dev'),
      expiresIn: '7d',
    });
    const refreshToken = uuidv4();
    const expiresAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);

    await this.sessionRepo.save({
      userId: user.id,
      refreshToken: refreshToken,
      deviceId: 'guest',
      expiresAt,
    });

    return {
      accessToken,
      refreshToken,
      expiresIn: 604800,
      user: { id: user.id, nickname: user.nickname, role: 'guest' },
      limits: this.guestLimits,
    };
  }

  getGuestLimits() {
    return this.guestLimits;
  }
}
