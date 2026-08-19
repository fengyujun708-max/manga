import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Notification } from '../community/entities/community.entity';

@Injectable()
export class NotificationService {
  constructor(@InjectRepository(Notification) private notifRepo: Repository<Notification>) {}

  async getNotifications(userId: string) {
    return this.notifRepo.find({ where: { userId }, order: { createdAt: 'DESC' }, take: 50 });
  }

  async markRead(userId: string, id: string) {
    await this.notifRepo.update({ id, userId }, { isRead: true });
    return { message: '已标记已读' };
  }

  async getUnreadCount(userId: string) {
    const count = await this.notifRepo.count({ where: { userId, isRead: false } });
    return { count };
  }
}
