import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { User } from './entities/user.entity';
import { UserDevice } from './entities/user.entity';
import { ReadingHistory, Favorite } from '../comic/entities/comic.entity';

@Injectable()
export class UserService {
  constructor(
    @InjectRepository(User) private userRepo: Repository<User>,
    @InjectRepository(UserDevice) private deviceRepo: Repository<UserDevice>,
    @InjectRepository(ReadingHistory) private historyRepo: Repository<ReadingHistory>,
    @InjectRepository(Favorite) private favoriteRepo: Repository<Favorite>,
  ) {}

  async getProfile(userId: string) {
    const user = await this.userRepo.findOneBy({ id: userId });
    if (!user) throw new NotFoundException('用户不存在');
    return user;
  }

  async updateProfile(userId: string, dto: { nickname: string }) {
    await this.userRepo.update(userId, { nickname: dto.nickname });
    return this.getProfile(userId);
  }

  async updatePhone(userId: string, phone: string, code: string) {
    // TODO Phase 2: 验证验证码
    const existing = await this.userRepo.findOneBy({ phone });
    if (existing) throw new BadRequestException('手机号已被使用');
    await this.userRepo.update(userId, { phone, phoneVerified: true });
    return this.getProfile(userId);
  }

  async updateAvatar(userId: string, url: string) {
    await this.userRepo.update(userId, { avatar: url });
    return this.getProfile(userId);
  }

  async getDevices(userId: string) {
    return this.deviceRepo.find({
      where: { userId },
      order: { lastActiveAt: 'DESC' },
    });
  }

  async getStats(userId: string) {
    const history = await this.historyRepo.find({ where: { userId } });
    const totalReadTime = history.reduce((sum, h) => sum + h.totalReadTime, 0);
    const favorites = await this.favoriteRepo.count({ where: { userId } });
    return {
      totalReadTime,
      totalRead: history.length,
      totalFavorites: favorites,
    };
  }
}