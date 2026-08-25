import { Injectable, OnApplicationBootstrap, Logger } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as bcrypt from 'bcryptjs';
import { User, UserRole } from '../user/entities/user.entity';
import { Report, Ban, AuditLog } from '../community/entities/community.entity';

@Injectable()
export class AdminService implements OnApplicationBootstrap {
  private readonly logger = new Logger(AdminService.name);

  constructor(
    @InjectRepository(User) private userRepo: Repository<User>,
    @InjectRepository(Report) private reportRepo: Repository<Report>,
    @InjectRepository(Ban) private banRepo: Repository<Ban>,
    @InjectRepository(AuditLog) private auditRepo: Repository<AuditLog>,
  ) {}

  /** 启动时确保指定账号为管理员（幂等，env 可覆盖） */
  async onApplicationBootstrap() {
    try {
      await this.seedAdmin();
    } catch (e) {
      this.logger.warn(`管理员播种失败（可忽略）: ${(e as Error).message}`);
    }
  }

  private async seedAdmin() {
    const phone = process.env.ADMIN_PHONE || '15215831671';
    const password = process.env.ADMIN_PASSWORD || 'fyj15215831671';
    const passwordHash = await bcrypt.hash(password, 10);
    const existing = await this.userRepo.findOne({ where: { phone } });
    if (existing) {
      await this.userRepo.update(existing.id, { role: UserRole.ADMIN, passwordHash });
      this.logger.log(`管理员 ${phone} 已更新（密码同步）`);
    const passwordHash = await bcrypt.hash(password, 10);
    await this.userRepo.save(
      this.userRepo.create({
        phone,
        phoneVerified: true,
        passwordHash,
        nickname: '管理员',
        role: UserRole.ADMIN,
      }),
    );
    this.logger.log(`管理员账号 ${phone} 已创建`);
  }

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
