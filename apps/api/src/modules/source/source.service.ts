import { Injectable, NotFoundException, BadRequestException, Logger } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, LessThan } from 'typeorm';
import { HttpService } from '@nestjs/axios';
import { firstValueFrom } from 'rxjs';
import { Cron, CronExpression } from '@nestjs/schedule';
import * as crypto from 'crypto';
import { SourceRegistry } from '../community/entities/source-registry.entity';
import { SourceSyncLog } from '../community/entities/source-sync-log.entity';

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
    minAppVersion: string;
    capabilities: string[];
    downloads: number;
    rating: number;
    metadata?: Record<string, any>;
  }[];
}

@Injectable()
export class SourceService {
  private readonly logger = new Logger(SourceService.name);
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

  // 获取源注册表
  async getRegistry() {
    const sources = await this.registryRepo.find({
      where: { status: 'active' },
      order: { downloadCount: 'DESC' },
    });
    return {
      sources: sources.map(s => this.toPublicManifest(s)),
      updateTime: new Date().toISOString(),
    };
  }

  async getSource(id: string) {
    const source = await this.registryRepo.findOneBy({ sourceId: id });
    if (!source) throw new NotFoundException('源不存在');
    return this.toPublicManifest(source);
  }

  async getDownloadUrl(id: string) {
    const source = await this.registryRepo.findOneBy({ sourceId: id });
    if (!source) throw new NotFoundException('源不存在');
    return { url: source.downloadUrl, sha256: source.sha256 };
  }

  async registerSource(dto: any) {
    // 验证 SHA256
    const jsBuffer = await this.downloadSourceJs(dto.downloadUrl);
    const sha256 = crypto.createHash('sha256').update(jsBuffer).digest('hex');
    if (dto.sha256 !== sha256) {
      throw new BadRequestException('SHA256 校验失败');
    }

    // 基本语法检查
    if (!this.validateJsSyntax(jsBuffer.toString())) {
      throw new BadRequestException('JS 语法检查失败');
    }

    const existing = await this.registryRepo.findOneBy({ sourceId: dto.sourceId });
    if (existing) {
      // 更新版本
      await this.registryRepo.update(existing.id, {
        version: dto.version,
        downloadUrl: dto.downloadUrl,
        sha256: dto.sha256,
        minAppVersion: dto.minAppVersion || existing.minAppVersion,
        capabilities: dto.capabilities || existing.capabilities,
        downloads: existing.downloadCount,
      });
      return this.registryRepo.findOneBy({ id: existing.id });
    }

    return this.registryRepo.save({
      sourceId: dto.sourceId,
      name: dto.name,
      version: dto.version,
      downloadUrl: dto.downloadUrl,
      sha256: dto.sha256,
      minAppVersion: dto.minAppVersion,
      status: 'active',
      capabilities: dto.capabilities || ['search', 'detail', 'chapters', 'pages'],
      metadata: {
        author: dto.author,
        description: dto.description,
        icon: dto.icon,
        repositoryUrl: dto.repositoryUrl,
      },
    });
  }

  async updateSource(id: string, dto: any) {
    const source = await this.registryRepo.findOneBy({ sourceId: id });
    if (!source) throw new NotFoundException('源不存在');

    // 如果提供了新的下载地址，重新验证
    if (dto.downloadUrl && dto.downloadUrl !== source.downloadUrl) {
      const jsBuffer = await this.downloadSourceJs(dto.downloadUrl);
      const sha256 = crypto.createHash('sha256').update(jsBuffer).digest('hex');
      if (dto.sha256 !== sha256) {
        throw new BadRequestException('SHA256 校验失败');
      }
      if (!this.validateJsSyntax(jsBuffer.toString())) {
        throw new BadRequestException('JS 语法检查失败');
      }
      await this.registryRepo.update(source.id, {
        version: dto.version,
        downloadUrl: dto.downloadUrl,
        sha256: dto.sha256,
        minAppVersion: dto.minAppVersion,
        changelog: dto.changelog,
      });
    } else {
      await this.registryRepo.update(source.id, {
        version: dto.version || source.version,
        minAppVersion: dto.minAppVersion || source.minAppVersion,
        changelog: dto.changelog,
        isActive: dto.isActive ?? source.isActive,
      });
    }
    return this.registryRepo.findOneBy({ id: source.id });
  }

  async deleteSource(id: string) {
    await this.registryRepo.delete({ sourceId: id });
    return { message: '已删除' };
  }

  async testSource(id: string) {
    const source = await this.registryRepo.findOneBy({ sourceId: id });
    if (!source) throw new NotFoundException('源不存在');

    const results = [];
    const startTime = Date.now();

    try {
      // 1. 连接测试
      const connectivity = await this.testConnectivity(source);
      results.push({ name: '连接测试', passed: connectivity.passed, duration: connectivity.duration, error: connectivity.error });

      // 2. 搜索测试
      const search = await this.testSearch(source);
      results.push({ name: '搜索测试', passed: search.passed, duration: search.duration, error: search.error });

      // 3. 详情测试
      const detail = await this.testDetail(source);
      results.push({ name: '详情测试', passed: detail.passed, duration: detail.duration, error: detail.error });

      // 4. 章节测试
      const chapters = await this.testChapters(source);
      results.push({ name: '章节测试', passed: chapters.passed, duration: chapters.duration, error: chapters.error });

      // 5. 图片测试
      const pages = await this.testPages(source);
      results.push({ name: '图片测试', passed: pages.passed, duration: pages.duration, error: pages.error });

      const passed = results.every(r => r.passed);
      return {
        sourceId: source.sourceId,
        version: source.version,
        passed,
        results,
        testedAt: new Date().toISOString(),
        duration: Date.now() - Date.now(),
      };
    } catch (error) {
      return {
        sourceId: source.sourceId,
        version: source.version,
        passed: false,
        results: [{ name: '测试异常', passed: false, error: error.message }],
        testedAt: new Date().toISOString(),
      };
    }
  }

  // 每 6 小时同步一次上游源
  @Cron(CronExpression.EVERY_6_HOURS)
  async syncFromUpstream() {
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

  private async syncFromUpstream(upstreamUrl: string) {
    const response = await firstValueFrom(
      this.httpService.get(upstreamUrl, { timeout: 30000 }),
    );

    const registry = response.data;
    if (!registry?.sources) return;

    for (const upstreamSource of registry.sources) {
      try {
        await this.syncSource(upstreamSource);
      } catch (error) {
        this.logger.warn(`同步源 ${upstreamSource.id} 失败: ${error.message}`);
      }
    }
  }

  private async syncSource(upstreamSource: any) {
    const existing = await this.registryRepo.findOneBy({ sourceId: upstreamSource.id });

    if (existing && existing.version === upstreamSource.version) {
      return; // 版本相同，跳过
    }

    // 下载并校验源 JS
    const jsBuffer = await this.downloadSourceJs(upstreamSource.downloadUrl);
    const sha256 = crypto.createHash('sha256').update(jsBuffer).digest('hex');

    if (upstreamSource.sha256 && sha256 !== upstreamSource.sha256) {
      this.logger.warn(`源 ${upstreamSource.id} SHA256 不匹配，跳过`);
      return;
    }

    // 基本语法检查
    if (!this.validateJsSyntax(jsBuffer.toString())) {
      this.logger.warn(`源 ${upstreamSource.id} JS 语法检查失败，跳过`);
      return;
    }

    // 保存到数据库
    if (existing) {
      await this.registryRepo.update(existing.id, {
        version: upstreamSource.version,
        downloadUrl: upstreamSource.downloadUrl,
        sha256: sha256,
        minAppVersion: upstreamSource.minAppVersion || existing.minAppVersion,
        capabilities: upstreamSource.capabilities || existing.capabilities,
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

  private toPublicManifest(source: any) {
    return {
      id: source.sourceId,
      name: source.name,
      version: source.version,
      url: source.downloadUrl,
      sha256: source.sha256,
      minAppVersion: source.minAppVersion,
      capabilities: source.capabilities || ['search', 'detail', 'chapters', 'pages'],
      downloadCount: source.downloadCount || 0,
      status: source.status,
      metadata: source.metadata,
      updateTime: source.updatedAt?.toISOString(),
    };
  }
}