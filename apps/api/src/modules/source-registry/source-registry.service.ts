import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, In } from 'typeorm';
import { SourceRegistry } from './entities/source-registry.entity';
import { CreateSourceRegistryDto } from './dto/create-source-registry.dto';
import { VettedSources } from '../../constants/vetted-sources';

@Injectable()
export class SourceRegistryService {
  constructor(
    @InjectRepository(SourceRegistry)
    private repo: Repository<SourceRegistry>,
  ) {}

  /** 公开接口：返回客户端启用的源列表（用于客户端注册表同步） */
  async publicRegistry() {
    const rows = await this.repo.find({
      where: { enabled: true },
      order: { displayName: 'ASC' },
    });
    return rows.map((r) => ({
      id: r.id,
      name: r.displayName,
      version: r.version,
      enabled: r.enabled,
      vetted: r.vetted,
      networkType: r.networkType,
      updatedAt: r.updatedAt,
    }));
  }

  /** 完整列表（管理后台） */
  async list(enabledOnly = false) {
    return this.repo.find({
      where: enabledOnly ? { enabled: true } : undefined,
      order: { updatedAt: 'DESC' },
      relations: { },
    });
  }

  async getById(id: string) {
    const r = await this.repo.findOne({ where: { id } });
    if (!r) throw new NotFoundException(`源 ${id} 不存在`);
    return r;
  }

  async upsert(dto: CreateSourceRegistryDto) {
    const existing = await this.repo.findOne({ where: { id: dto.id } });
    if (existing) {
      await this.repo.update({ id: dto.id }, {
        displayName: dto.displayName,
        description: dto.description,
        author: dto.author,
        repositoryUrl: dto.repositoryUrl,
        version: dto.version,
        enabled: dto.enabled ?? existing.enabled,
        vetted: dto.vetted ?? existing.vetted,
        minAppVersion: dto.minAppVersion,
        downloadUrl: dto.downloadUrl,
        iconUrl: dto.iconUrl,
        metadata: dto.metadata,
        tags: dto.tags,
        capabilities: dto.capabilities,
        networkType: dto.networkType || existing.networkType,
      });
      return await this.repo.findOne({ where: { id: dto.id } });
    }
    const row = this.repo.create({
      id: dto.id,
      displayName: dto.displayName,
      description: dto.description,
      author: dto.author,
      repositoryUrl: dto.repositoryUrl,
      version: dto.version,
      enabled: dto.enabled ?? true,
      vetted: dto.vetted ?? false,
      minAppVersion: dto.minAppVersion,
      downloadUrl: dto.downloadUrl,
      iconUrl: dto.iconUrl,
      metadata: dto.metadata,
      tags: dto.tags,
      capabilities: dto.capabilities,
      networkType: dto.networkType || 'HTTP',
    });
    return this.repo.save(row);
  }

  async toggle(id: string, enabled: boolean) {
    const r = await this.repo.findOne({ where: { id } });
    if (!r) throw new NotFoundException(`源 ${id} 不存在`);
    r.enabled = enabled;
    return this.repo.save(r);
  }

  async updateHealth(id: string, status: string, error?: string) {
    await this.repo.update({ id }, { healthStatus: status, healthError: error, healthCheckedAt: new Date() });
    return this.repo.findOne({ where: { id } });
  }

  /** 同步内置源清单到注册表（用于初始化） */
  async syncVetted() {
    let count = 0;
    for (final id of VettedSources) {
      const existing = await this.repo.findOne({ where: { id } });
      if (!existing) {
        await this.repo.save(this.repo.create({
          id,
          displayName: id,
          version: '1.0.0',
          enabled: true,
          vetted: true,
          networkType: 'HTTP',
        }));
        count++;
      }
    }
    return count;
  }
}
