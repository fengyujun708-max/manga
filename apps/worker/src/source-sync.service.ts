import { Injectable, Logger } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { HttpService } from '@nestjs/axios';
import { firstValueFrom } from 'rxjs';
import * as crypto from 'crypto';
import { SourceRegistry } from '../community/entities/community.entity';

interface UpstreamRegistry {
  sources: {
    id: string;
    name: string;
    version: string;
    url: string;
    sha256: string;
    minAppVersion?: string;
  }[];
}

@Injectable()
export class SourceSyncService {
  private readonly logger = new Logger(SourceSyncService.name);

  // 上游源注册表地址（Venera-Configs / Breeze 源）
  private readonly upstreams = [
    'https://raw.githubusercontent.com/deretame/Breeze/main/plugin-registry.json',
    'https://raw.githubusercontent.com/venera-app/venera-configs/main/registry.json',
  ];

  constructor(
    @InjectRepository(SourceRegistry)
    private registryRepo: Repository<SourceRegistry>,
    private httpService: HttpService,
  ) {}

  // 每 6 小时同步一次
  @Cron('0 */6 * * *')
  async syncSources() {
    this.logger.log('开始同步上游源...');

    for (const upstreamUrl of this.upstreams) {
      try {
        await this.syncFromUpstream(upstreamUrl);
      } catch (error) {
        this.logger.error(`同步 ${upstreamUrl} 失败: ${(error as Error).message}`);
      }
    }

    this.logger.log('源同步完成');
  }

  private async syncFromUpstream(upstreamUrl: string) {
    const response = await firstValueFrom(
      this.httpService.get<UpstreamRegistry>(upstreamUrl, {
        timeout: 30000,
      }),
    );

    const registry = response.data;
    if (!registry?.sources) return;

    for (const upstreamSource of registry.sources) {
      try {
        await this.syncSource(upstreamSource);
      } catch (error) {
        this.logger.warn(`同步源 ${upstreamSource.id} 失败: ${(error as Error).message}`);
      }
    }
  }

  private async syncSource(upstreamSource: UpstreamRegistry['sources'][0]) {
    // 检查是否已存在
    const existing = await this.registryRepo.findOneBy({
      sourceId: upstreamSource.id,
    });

    if (existing && existing.version === upstreamSource.version) {
      // 版本相同，跳过
      return;
    }

    // 下载并校验源 JS
    const jsBuffer = await this.downloadSourceJs(upstreamSource.url);
    const sha256 = crypto.createHash('sha256').update(jsBuffer).digest('hex');

    // 验证 SHA256
    if (upstreamSource.sha256 && sha256 !== upstreamSource.sha256) {
      this.logger.warn(`源 ${upstreamSource.id} SHA256 不匹配，跳过`);
      return;
    }

    // 基本 JS 语法检查
    if (!this.validateJsSyntax(jsBuffer.toString())) {
      this.logger.warn(`源 ${upstreamSource.id} JS 语法检查失败，跳过`);
      return;
    }

    // 保存到数据库
    if (existing) {
      await this.registryRepo.update(existing.id, {
        version: upstreamSource.version,
        downloadUrl: upstreamSource.url,
        sha256,
        minAppVersion: upstreamSource.minAppVersion || existing.minAppVersion,
      });
    } else {
      await this.registryRepo.save({
        sourceId: upstreamSource.id,
        name: upstreamSource.name,
        version: upstreamSource.version,
        downloadUrl: upstreamSource.url,
        sha256,
        minAppVersion: upstreamSource.minAppVersion,
        status: 'active',
      });
    }

    this.logger.log(`源 ${upstreamSource.id} v${upstreamSource.version} 同步成功`);
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
      // 简单检查：必须有 source 对象定义
      if (!code.includes('const source') && !code.includes('var source') && !code.includes('let source')) {
        return false;
      }
      // 检查基本结构
      if (!code.includes('search') || !code.includes('getDetail') || !code.includes('getPages')) {
        return false;
      }
      return true;
    } catch {
      return false;
    }
  }
}