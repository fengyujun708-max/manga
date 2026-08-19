import { Injectable, Logger } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { HttpService } from '@nestjs/axios';
import { firstValueFrom } from 'rxjs';
import * as crypto from 'crypto';
import { SourceRegistry } from '../entities/source-registry.entity';
import { SourceSyncLog } from '../entities/source-sync-log.entity';

interface UpstreamRegistry {
  sources: {
    id: string;
    name: string;
    version: string;
    author?: string;
    description?: string;
    icon?: string;
    repositoryUrl?: string;
    downloadUrl: string;
    sha256: string;
    minAppVersion?: string;
    capabilities?: string[];
  }[];
}

@Injectable()
export class SourceSyncService {
  private readonly logger = new Logger(SourceSyncService.name);

  private readonly upstreams = [
    'https://raw.githubusercontent.com/deretame/Breeze/main/plugin-registry.json',
    'https://raw.githubusercontent.com/venera-app/venera-configs/main/registry.json',
  ];

  constructor(
    @InjectRepository(SourceRegistry) private registryRepo: Repository<SourceRegistry>,
    @InjectRepository(SourceSyncLog) private syncLogRepo: Repository<SourceSyncLog>,
    private httpService: HttpService,
  ) {}

  @Cron(CronExpression.EVERY_6_HOURS)
  async syncSources() {
    this.logger.log('开始同步上游源...');
    for (const upstreamUrl of this.upstreams) {
      try {
        await this.syncFromUpstream(upstreamUrl);
      } catch (error) {
        this.logger.error(`同步 ${upstreamUrl} 失败: ${error.message}`);
      }
    }
    this.logger.log('源同步完成');
  }

  async manualSync() {
    await this.syncSources();
    return { message: '同步完成' };
  }

  private async syncFromUpstream(upstreamUrl: string) {
    const response = await firstValueFrom(
      this.httpService.get<UpstreamRegistry>(upstreamUrl, { timeout: 30000 }),
    );
    const registry = response.data;
    if (!registry?.sources) return;

    let success = 0;
    let failed = 0;
    for (const source of registry.sources) {
      try {
        await this.syncSource(source);
        success++;
      } catch (error) {
        failed++;
        this.logger.warn(`同步源 ${source.id} 失败: ${error.message}`);
      }
    }
    await this.syncLogRepo.save({
      sourceId: 'upstream',
      action: 'sync',
      status: failed === 0 ? 'success' : 'partial',
      message: `成功: ${success}, 失败: ${failed}`,
      duration: 0,
    });
  }

  private async syncSource(source: any) {
    const existing = await this.registryRepo.findOneBy({ sourceId: source.id });
    if (existing && existing.version === source.version) return;

    const jsBuffer = await this.downloadSourceJs(source.downloadUrl);
    const sha256 = crypto.createHash('sha256').update(jsBuffer).digest('hex');

    if (source.sha256 && sha256 !== source.sha256) {
      throw new Error(`SHA256 不匹配 (${source.id})`);
    }
    if (!this.validateJsSyntax(jsBuffer.toString())) {
      throw new Error(`JS 语法检查失败 (${source.id})`);
    }

    const data = {
      name: source.name,
      version: source.version,
      downloadUrl: source.downloadUrl,
      sha256,
      minAppVersion: source.minAppVersion,
      capabilities: source.capabilities || ['search', 'detail', 'chapters', 'pages'],
      metadata: { author: source.author, description: source.description, icon: source.icon },
    };

    if (existing) {
      await this.registryRepo.update(existing.id, data);
    } else {
      await this.registryRepo.save({ sourceId: source.id, status: 'active', ...data });
    }
  }

  private async downloadSourceJs(url: string): Promise<Buffer> {
    const response = await firstValueFrom(
      this.httpService.get<ArrayBuffer>(url, { responseType: 'arraybuffer', timeout: 30000 }),
    );
    return Buffer.from(response.data);
  }

  private validateJsSyntax(code: string): boolean {
    try {
      if (!code.includes('const source') && !code.includes('var source') && !code.includes('let source')) return false;
      if (!code.includes('search') || !code.includes('getDetail') || !code.includes('getPages')) return false;
      return true;
    } catch {
      return false;
    }
  }
}
