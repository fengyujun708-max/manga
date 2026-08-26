import { Controller, Get, Post, Body, Param, Query, UseGuards, Patch, Delete } from '@nestjs/common';
import { SourceRegistryService } from './source-registry.service';
import { SourceRegistry } from './entities/source-registry.entity';
import { RolesGuard, Roles } from '../../common/guards/auth.guard';
import { UserRole } from '../user/entities/user.entity';

@Controller('sources')
export class SourceRegistryController {
  constructor(private readonly sourceRegistryService: SourceRegistryService) {}

  /**
   * 获取所有源（客户端同步接口）
   * GET /sources
   * 可选查询参数：onlyEnabled=true（默认只返回启用的源）
   */
  @Get()
  async findAll(@Query('onlyEnabled') onlyEnabled: boolean = true): Promise<SourceRegistry[]> {
    return this.sourceRegistryService.findAll(onlyEnabled);
  }

  /**
   * 获取单个源信息
   * GET /sources/:sourceId
   */
  @Get(':sourceId')
  async findOne(@Param('sourceId') sourceId: string): Promise<SourceRegistry | null> {
    return this.sourceRegistryService.findOne(sourceId);
  }

  /**
   * 获取源版本信息（用于客户端检查更新）
   * GET /sources/:sourceId/version
   */
  @Get(':sourceId/version')
  async getVersion(@Param('sourceId') sourceId: string): Promise<{ version: string; minAppVersion: string } | null> {
    return this.sourceRegistryService.getSourceVersion(sourceId);
  }

  /**
   * 获取源元数据
   * GET /sources/:sourceId/metadata
   */
  @Get(':sourceId/metadata')
  async getMetadata(@Param('sourceId') sourceId: string): Promise<Record<string, any> | null> {
    return this.sourceRegistryService.getSourceMetadata(sourceId);
  }

  /**
   * 检查源健康状态
   * GET /sources/:sourceId/health
   */
  @Get(':sourceId/health')
  async checkHealth(@Param('sourceId') sourceId: string): Promise<{
    healthy: boolean;
    latency: number;
    error?: string;
    timestamp: Date;
  }> {
    return this.sourceRegistryService.checkHealth(sourceId);
  }

  /**
   * 批量检查多个源的健康状态
   * POST /sources/health/batch
   */
  @Post('health/batch')
  async checkHealthBatch(@Body('sourceIds') sourceIds: string[]): Promise<Record<string, {
    healthy: boolean;
    latency: number;
    error?: string;
    timestamp: Date;
  }>> {
    return this.sourceRegistryService.checkHealthBatch(sourceIds);
  }

  /**
   * 按能力过滤源
   * GET /sources?capabilities=search,detail
   */
  @Get('by-capabilities')
  async findByCapabilities(@Query('capabilities') capabilities: string): Promise<SourceRegistry[]> {
    const capArray = capabilities?.split(',') || [];
    return this.sourceRegistryService.findByCapabilities(capArray);
  }

  /**
   * 同步 Venera 官方源（管理员接口）
   * POST /sources/sync
   * 从 Venera 官方注册表同步已审核源
   */
  @Post('sync')
  @UseGuards(RolesGuard)
  @Roles(UserRole.ADMIN, UserRole.SUPER_ADMIN)
  async syncFromVettedRegistry(): Promise<{
    added: number;
    updated: number;
    skipped: number;
  }> {
    return this.sourceRegistryService.syncFromVettedRegistry();
  }

  /**
   * 启用源（管理员接口）
   * PATCH /sources/:sourceId/enable
   */
  @Patch(':sourceId/enable')
  @UseGuards(RolesGuard)
  @Roles(UserRole.ADMIN, UserRole.SUPER_ADMIN)
  async enable(@Param('sourceId') sourceId: string): Promise<SourceRegistry | null> {
    return this.sourceRegistryService.enable(sourceId);
  }

  /**
   * 禁用源（管理员接口）
   * PATCH /sources/:sourceId/disable
   */
  @Patch(':sourceId/disable')
  @UseGuards(RolesGuard)
  @Roles(UserRole.ADMIN, UserRole.SUPER_ADMIN)
  async disable(@Param('sourceId') sourceId: string): Promise<SourceRegistry | null> {
    return this.sourceRegistryService.disable(sourceId);
  }

  /**
   * 创建或更新源（管理员接口）
   * POST /sources
   */
  @Post()
  @UseGuards(RolesGuard)
  @Roles(UserRole.ADMIN, UserRole.SUPER_ADMIN)
  async createOrUpdate(@Body() data: Partial<SourceRegistry>): Promise<SourceRegistry> {
    return this.sourceRegistryService.createOrUpdate(data);
  }

  /**
   * 删除源（管理员接口）
   * DELETE /sources/:sourceId
   */
  @Delete(':sourceId')
  @UseGuards(RolesGuard)
  @Roles(UserRole.ADMIN, UserRole.SUPER_ADMIN)
  async remove(@Param('sourceId') sourceId: string): Promise<{ success: boolean }> {
    const success = await this.sourceRegistryService.remove(sourceId);
    return { success };
  }

  /**
   * 获取统计信息
   * GET /sources/stats
   */
  @Get('stats')
  async getStats(): Promise<{
    total: number;
    active: number;
    disabled: number;
    totalDownloads: number;
  }> {
    return this.sourceRegistryService.getStats();
  }

  /**
   * 增加下载计数（客户端调用）
   * POST /sources/:sourceId/download
   */
  @Post(':sourceId/download')
  async incrementDownload(@Param('sourceId') sourceId: string): Promise<SourceRegistry | null> {
    return this.sourceRegistryService.incrementDownload(sourceId);
  }
}
