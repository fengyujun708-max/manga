import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, LessThanOrEqual, MoreThanOrEqual } from 'typeorm';
import { AppVersion, RemoteConfig, Announcement } from '../community/entities/community.entity';

@Injectable()
export class UpdateService {
  constructor(
    @InjectRepository(AppVersion) private versionRepo: Repository<AppVersion>,
    @InjectRepository(RemoteConfig) private configRepo: Repository<RemoteConfig>,
    @InjectRepository(Announcement) private announcementRepo: Repository<Announcement>,
  ) {}

  async checkUpdate(version: string, platform: string) {
    const currentCode = this.parseVersion(version);
    const latest = await this.versionRepo.findOne({
      where: { platform, isActive: true },
      order: { buildNumber: 'DESC' },
    });
    if (!latest) return { hasUpdate: false };
    const hasUpdate = latest.buildNumber > currentCode;
    const isForceUpdate = latest.isForceUpdate && latest.minVersion != null
      && currentCode <= this.parseVersion(latest.minVersion);
    return {
      hasUpdate, isForceUpdate,
      latestVersion: { version: latest.version, buildNumber: latest.buildNumber, platform: latest.platform },
      updateUrl: latest.downloadUrl, changelog: latest.changelog,
      message: isForceUpdate ? '当前版本已停止支持，请更新后继续使用' : null,
    };
  }

  async getRemoteConfig() {
    const configs = await this.configRepo.find({ where: { isActive: true } });
    const result: Record<string, any> = {};
    for (const config of configs) result[config.key] = config.value;
    result['registration_enabled'] ??= true;
    result['community_enabled'] ??= true;
    result['maintenance_mode'] ??= false;
    return result;
  }

  async getActiveAnnouncements() {
    const now = new Date();
    return this.announcementRepo.find({
      where: { isActive: true, startAt: LessThanOrEqual(now), endAt: MoreThanOrEqual(now) },
      order: { priority: 'DESC', createdAt: 'DESC' },
    });
  }

  private parseVersion(version: string): number {
    try {
      const parts = version.split('.').map(p => parseInt(p, 10));
      return parts[0] * 10000 + (parts[1] || 0) * 100 + (parts[2] || 0);
    } catch { return 0; }
  }
}
