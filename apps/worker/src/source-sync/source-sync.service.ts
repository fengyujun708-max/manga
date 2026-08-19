import { Injectable, Logger } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, LessThan } from 'typeorm';
import { HttpService } from '@nestjs/axios';
import { firstValueFrom } from 'rxjs';
import { SourceRegistry } from '../../api/src/modules/community/entities/source-registry.entity';
import { SourceSyncLog } from '../../api/src/modules/community/entities/source-sync-log.entity';
import * as crypto from 'crypto';

interface UpstreamRegistry {
  sources: {
    id: string;
    name: string;
    version: string;
    author: string;
    description: string;
    icon: string;
    repositoryUrl: string;
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
    @InjectRepository(SourceRegistry)
    private registryRepo: Repository<SourceRegistry>,
    @InjectRepository(SourceSyncLog)
    private syncLogRepo: Repository<SourceSyncLog>,
    private httpService: HttpService,
  ) {}

  // 每 6 小时同步一次
  @Cron(CronExpression.EVERY_6_HOURS)
  async syncSources() {
    this.logger.log('开始同步上游源...');

    for (const upstreamUrl of this.upstreams) {
      try {
        await this.syncFromUpstream(upstreamUrl);
      } catch (error) {
        this.logger.error(`同步 ${upstreamUrl} 失败: ${error.message}`);
        await this.logSync(upstreamUrl, 'sync', 'failed', error.message);
      }
    }

    this.logger.log('源同步完成');
  }

  // 手动触发同步
  async manualSync() {
    await this.syncSources();
    return { message: '同步完成' };
  }

  private async syncFromUpstream(upstreamUrl: string) {
    this.logger.log(`同步上游源: ${upstreamUrl}`);
    const startTime = Date.now();

    let response;
    try {
      response = await firstValueFrom(
        this.httpService.get<{ sources: any[] }>(upstreamUrl, { timeout: 30000 }),
      );
    } catch (error) {
      await this.logSync(upstreamUrl, 'sync', 'failed', `下载失败: ${error.message}`, Date.now() - startTime);
      throw error;
    }

    const registry = response.data;
    if (!registry?.sources) {
      await this.logSync(upstreamUrl, 'sync', 'failed', '无效的注册表格式', Date.now() - startTime);
      return;
    }

    let successCount = 0;
    let failCount = 0;

    for (const upstreamSource of registry.sources) {
      try {
        await this.syncSource(upstreamSource);
        successCount++;
      } catch (error) {
        failCount++;
        this.logger.warn(`同步源 ${upstreamSource.id} 失败: ${error.message}`);
      }
    }

    await this.logSync(upstreamUrl, 'sync', failCount === 0 ? 'success' : 'partial', 
      `成功: ${successCount}, 失败: ${failCount}`, Date.now() - startTime);
  }

  private async syncSource(upstreamSource: any) {
    const existing = await this.registryRepo.findOneBy({ sourceId: upstreamSource.id });

    if (existing && existing.version === upstreamSource.version) {
      return; // 版本相同，跳过
    }

    // 下载并校验源 JS
    const jsBuffer = await this.downloadSourceJs(upstreamSource.downloadUrl);
    const sha256 = crypto.createHash('sha256').update(jsBuffer).digest('hex');

    // 验证 SHA256
    if (upstreamSource.sha256 && sha256 !== upstreamSource.sha256) {
      throw new Error(`源 ${upstreamSource.id} SHA256 不匹配`);
    }

    // 基本 JS 语法检查
    if (!this.validateJsSyntax(jsBuffer.toString())) {
      throw new Error('JS 语法检查失败');
    }

    // 保存到数据库
    if (existing) {
      await this.registryRepo.update(existing.id, {
        version: upstreamSource.version,
        downloadUrl: upstreamSource.downloadUrl,
        sha256,
        minAppVersion: upstreamSource.minAppVersion || existing.minAppVersion,
        capabilities: upstreamSource.capabilities || existing.capabilities,
        metadata: {
          author: upstreamSource.author,
          description: upstreamSource.description,
          icon: upstreamSource.icon,
          repositoryUrl: upstreamSource.repositoryUrl,
        },
      });
    } else {
      await this.registryRepo.save({
        sourceId: upstreamSource.id,
        name: upstreamSource.name,
        version: upstreamSource.version,
        downloadUrl: upstreamSource.downloadUrl,
        sha256,
        minAppVersion: upstreamSource.minAppVersion,
        status: 'active',
        capabilities: upstreamSource.capabilities || ['search', 'detail', 'chapters', 'pages'],
        metadata: {
          author: upstreamSource.author,
          description: upstreamSource.description,
          icon: upstreamSource.icon,
          repositoryUrl: upstreamSource.repositoryUrl,
        },
      });
    }
  }

  private async downloadSourceJs(url: string): Promise<Buffer> {
    const response = await firstValueFrom(
      this.httpService.get(url, {
        responseType: 'arraybuffer',
        timeout: 30000,
      }),
    );
    return Buffer.from(response.data);
  }

  private validateJsSyntax(code: string): boolean {
    try {
      if (!code.includes('const source') && !code.includes('var source') && !code.includes('let source')) {
        return false;
      }
      if (!code.includes('search') || !code.includes('getDetail') || !code.includes('getPages')) {
        return false;
      }
      return true;
    } catch {
      return false;
    }
  }

  private async logSync(upstreamUrl: string, action: string, status: string, message: string, duration: number) {
    await this.syncLogRepo.save({
      sourceId: upstreamUrl,
      action,
      status,
      message,
      duration,
    });
  }
}