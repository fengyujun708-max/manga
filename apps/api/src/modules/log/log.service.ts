import { Injectable, Logger } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { ClientLog } from './entities/client-log.entity';

@Injectable()
export class LogService {
  private readonly logger = new Logger(LogService.name);

  constructor(
    @InjectRepository(ClientLog)
    private logRepo: Repository<ClientLog>,
  ) {}

  async report(body: any) {
    // body: { userId?, level, title, content, deviceModel?, appVersion?, platform? }
    // 或 { userId?, logs: [...] }
    try {
      const items: any[] = Array.isArray(body?.logs) ? body.logs : [body];

      for (const item of items) {
        if (!item || typeof item !== 'object') continue;
        if (!item.title && !item.content) continue;
        await this.logRepo.save({
          userId: item.userId ?? null,
          level: (item.level ?? 'info').toString().slice(0, 30),
          title: (item.title ?? '').toString().slice(0, 200),
          content: (item.content ?? '').toString().slice(0, 8192),
          deviceModel: item.deviceModel ? item.deviceModel.toString().slice(0, 50) : null,
          appVersion: item.appVersion ? item.appVersion.toString().slice(0, 30) : null,
          platform: item.platform ? item.platform.toString().slice(0, 30) : null,
        });
      }
      return { ok: true, received: items.length };
    } catch (e) {
      this.logger.error(`日志上报失败: ${e.message}`);
      return { ok: false, error: e.message };
    }
  }
}