import { Injectable, Logger } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, In, Not, IsNull } from 'typeorm';
import { SourceRegistry } from './entities/source-registry.entity';

@Injectable()
export class SourceRegistryService {
  private readonly logger = new Logger(SourceRegistryService.name);

  constructor(
    @InjectRepository(SourceRegistry)
    private readonly sourceRegistryRepository: Repository<SourceRegistry>,
  ) {}

  /**
   * 获取所有源，可按启用状态过滤
   * 客户端同步使用的主接口
   */
  async findAll(onlyEnabled: boolean = true): Promise<SourceRegistry[]> {
    const query = this.sourceRegistryRepository.createQueryBuilder('s');
    
    if (onlyEnabled) {
      query.andWhere('s.status = :enabled', { enabled: 'active' });
    }
    
    return query
      .orderBy('s.name', 'ASC')
      .getMany();
  }

  /**
   * 按 sourceId 查找单个源
   */
  async findOne(sourceId: string): Promise<SourceRegistry | null> {
    return this.sourceRegistryRepository.findOne({
      where: { sourceId },
    });
  }

  /**
   * 创建或更新源
   */
  async createOrUpdate(data: Partial<SourceRegistry>): Promise<SourceRegistry> {
    const existing = await this.sourceRegistryRepository.findOne({
      where: { sourceId: data.sourceId },
    });

    if (existing) {
      return this.sourceRegistryRepository.save({
        ...existing,
        ...data,
        uuid: existing.uuid, // 保持 uuid 不变
      });
    }

    return this.sourceRegistryRepository.save(data);
  }

  /**
   * 启用源
   */
  async enable(sourceId: string): Promise<SourceRegistry | null> {
    const source = await this.findOne(sourceId);
    if (!source) return null;

    return this.sourceRegistryRepository.save({
      ...source,
      status: 'active',
    });
  }

  /**
   * 禁用源
   */
  async disable(sourceId: string): Promise<SourceRegistry | null> {
    const source = await this.findOne(sourceId);
    if (!source) return null;

    return this.sourceRegistryRepository.save({
      ...source,
      status: 'disabled',
    });
  }

  /**
   * 增加下载计数
   */
  async incrementDownload(sourceId: string): Promise<SourceRegistry | null> {
    const source = await this.findOne(sourceId);
    if (!source) return null;

    return this.sourceRegistryRepository.save({
      ...source,
      downloadCount: source.downloadCount + 1,
    });
  }

  /**
   * 从 Venera 官方注册表同步已审核源
   * 这是 Venera Compatibility Layer 的核心
   */
  async syncFromVettedRegistry(): Promise<{
    added: number;
    updated: number;
    skipped: number;
  }> {
    const vettedSources = await this.fetchVettedSources();
    
    let added = 0;
    let updated = 0;
    let skipped = 0;

    for (const vetted of vettedSources) {
      const existing = await this.findOne(vetted.sourceId);

      if (existing) {
        // 检查版本是否需要更新
        if (existing.version !== vetted.version) {
          await this.sourceRegistryRepository.save({
            ...existing,
            ...vetted,
            uuid: existing.uuid,
          });
          updated++;
        } else {
          skipped++;
        }
      } else {
        await this.sourceRegistryRepository.save(vetted);
        added++;
      }
    }

    return { added, updated, skipped };
  }

  /**
   * 从 Venera 官方获取已审核源列表
   * 这是 Venera Compatibility Layer 的关键接口
   */
  private async fetchVettedSources(): Promise<Partial<SourceRegistry>[]> {
    // Venera 官方源注册表 URL
    const registryUrl = 'https://raw.githubusercontent.com/venera-app/Venera-Sources/main/registry.json';
    
    try {
      const response = await fetch(registryUrl, {
        headers: { 'User-Agent': 'Manjie-Source-Registry/1.0' },
      });

      if (!response.ok) {
        this.logger.warn(`Failed to fetch Venera registry: ${response.status}`);
        return [];
      }

      const registry = await response.json();
      const sources: Partial<SourceRegistry>[] = [];

      for (const [sourceId, sourceData] of Object.entries(registry.sources || {})) {
        sources.push({
          sourceId,
          name: sourceData.name,
          version: sourceData.version,
          author: sourceData.author,
          description: sourceData.description || '',
          icon: sourceData.icon || '',
          downloadUrl: sourceData.url || sourceData.downloadUrl || '',
          sha256: sourceData.sha256,
          minAppVersion: sourceData.minAppVersion || '1.0.0',
          capabilities: JSON.stringify(sourceData.capabilities || []),
          metadata: sourceData,
          status: 'active',
        });
      }

      return sources;
    } catch (error) {
      this.logger.error(`Error fetching Venera registry: ${error}`);
      return [];
    }
  }

  /**
   * 检查源健康状态
   * 这是 Venera Compatibility Layer 的健康检查
   */
  async checkHealth(sourceId: string): Promise<{
    healthy: boolean;
    latency: number;
    error?: string;
    timestamp: Date;
  }> {
    const source = await this.findOne(sourceId);
    
    if (!source) {
      return {
        healthy: false,
        latency: 0,
        error: 'Source not found',
        timestamp: new Date(),
      };
    }

    // 尝试获取源的清单文件
    try {
      const start = Date.now();
      const response = await fetch(source.downloadUrl, {
        method: 'HEAD',
        headers: { 'User-Agent': 'Manjie-Health-Check/1.0' },
      });
      const latency = Date.now() - start;

      if (response.ok) {
        return {
          healthy: true,
          latency,
          timestamp: new Date(),
        };
      } else {
        return {
          healthy: false,
          latency,
          error: `HTTP ${response.status}`,
          timestamp: new Date(),
        };
      }
    } catch (error) {
      return {
        healthy: false,
        latency: 0,
        error: error.message,
        timestamp: new Date(),
      };
    }
  }

  /**
   * 获取源的元数据（用于客户端）
   * 这是 Venera Compatibility Layer 的元数据接口
   */
  async getSourceMetadata(sourceId: string): Promise<Record<string, any> | null> {
    const source = await this.findOne(sourceId);
    return source?.metadata || null;
  }

  /**
   * 按能力过滤源
   */
  async findByCapabilities(capabilities: string[]): Promise<SourceRegistry[]> {
    return this.sourceRegistryRepository
      .createQueryBuilder('s')
      .where('s.status = :enabled', { enabled: 'active' })
      .andWhere(':caps <@ s.capabilities::text[]', { caps: `{${capabilities.map(c => `"${c}"`).join(',')}}` })
      .getMany();
  }

  /**
   * 获取源版本信息（用于客户端检查更新）
   */
  async getSourceVersion(sourceId: string): Promise<{ version: string; minAppVersion: string } | null> {
    const source = await this.findOne(sourceId);
    if (!source) return null;

    return {
      version: source.version,
      minAppVersion: source.minAppVersion,
    };
  }

  /**
   * 批量检查多个源的健康状态
   */
  async checkHealthBatch(sourceIds: string[]): Promise<Record<string, {
    healthy: boolean;
    latency: number;
    error?: string;
    timestamp: Date;
  }>> {
    const results: Record<string, any> = {};

    for (const sourceId of sourceIds) {
      results[sourceId] = await this.checkHealth(sourceId);
    }

    return results;
  }

  /**
   * 删除源（管理员操作）
   */
  async remove(sourceId: string): Promise<boolean> {
    const result = await this.sourceRegistryRepository.delete({ sourceId });
    return result.affected > 0;
  }

  /**
   * 获取统计信息
   */
  async getStats(): Promise<{
    total: number;
    active: number;
    disabled: number;
    totalDownloads: number;
  }> {
    const [total, active, disabled, downloads] = await Promise.all([
      this.sourceRegistryRepository.count(),
      this.sourceRegistryRepository.count({ where: { status: 'active' } }),
      this.sourceRegistryRepository.count({ where: { status: 'disabled' } }),
      this.sourceRegistryRepository
        .createQueryBuilder('s')
        .select('SUM(s.downloadCount)', 'total')
        .getRawOne(),
    ]);

    return {
      total,
      active,
      disabled,
      totalDownloads: parseInt(downloads?.total || '0'),
    };
  }
}
