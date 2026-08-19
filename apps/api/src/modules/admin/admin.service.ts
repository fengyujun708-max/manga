import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { User } from '../user/entities/user.entity';
import { Report, Ban, AuditLog } from '../community/entities/community.entity';

@Injectable()
export class AdminService {
  constructor(
    @InjectRepository(User) private userRepo: Repository<User>,
    @InjectRepository(Report) private reportRepo: Repository<Report>,
    @InjectRepository(Ban) private banRepo: Repository<Ban>,
    @InjectRepository(AuditLog) private auditRepo: Repository<AuditLog>,
  ) {}

  async getDashboard() {
    const totalUsers = await this.userRepo.count();
    const activeUsers = await this.userRepo.count({ where: { status: 'active' as any } });
    const pendingReports = await this.reportRepo.count({ where: { status: 'pending' } });
    return { totalUsers, activeUsers, pendingReports };
  }

  async getUsers(page = 1, limit = 20) {
    const [items, total] = await this.userRepo.findAndCount({ skip: (page - 1) * limit, take: limit, order: { createdAt: 'DESC' } });
    return { items, total, page, limit, totalPages: Math.ceil(total / limit) };
  }

  async banUser(adminId: string, userId: string, reason: string) {
    await this.userRepo.update(userId, { status: 'suspended' as any });
    await this.banRepo.save({ userId, handledBy: adminId, reason, startAt: new Date(), isActive: true });
    await this.auditRepo.save({ userId: adminId, action: 'ban_user', resourceType: 'user', resourceId: userId });
    return { message: '已封禁' };
  }

  async getReports() { return this.reportRepo.find({ where: { status: 'pending' }, order: { createdAt: 'DESC' } }); }

  async resolveReport(id: string) {
    await this.reportRepo.update(id, { status: 'resolved' });
    return { message: '已处理' };
  }
}
