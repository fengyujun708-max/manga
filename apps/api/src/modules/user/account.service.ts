import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { User } from './entities/user.entity';
import { UserSession, UserDevice } from './entities/user.entity';

@Injectable()
export class AccountService {
  constructor(
    @InjectRepository(User) private userRepo: Repository<User>,
    @InjectRepository(UserSession) private sessionRepo: Repository<UserSession>,
    @InjectRepository(UserDevice) private deviceRepo: Repository<UserDevice>,
  ) {}

  async deleteAccount(userId: string) {
    await this.userRepo.update(userId, { status: 'deleted' as any, phone: `deleted_${userId}`, nickname: '已注销用户' });
    await this.sessionRepo.update({ userId }, { isRevoked: true });
    await this.deviceRepo.delete({ userId });
    return { message: '账号已注销' };
  }

  async getDevices(userId: string) {
    return this.deviceRepo.find({ where: { userId }, order: { lastActiveAt: 'DESC' } });
  }

  async logoutOtherDevices(userId: string) {
    await this.sessionRepo.update({ userId }, { isRevoked: true });
    return { message: '已退出其他设备' };
  }
}
