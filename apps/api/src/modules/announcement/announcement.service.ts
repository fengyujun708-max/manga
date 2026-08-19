import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, LessThanOrEqual, MoreThanOrEqual } from 'typeorm';
import { Announcement } from '../community/entities/community.entity';

@Injectable()
export class AnnouncementService {
  constructor(@InjectRepository(Announcement) private announcementRepo: Repository<Announcement>) {}

  async getActiveAnnouncements() {
    const now = new Date();
    return this.announcementRepo.find({
      where: { isActive: true, startAt: LessThanOrEqual(now), endAt: MoreThanOrEqual(now) },
      order: { priority: 'DESC', createdAt: 'DESC' },
    });
  }
}
