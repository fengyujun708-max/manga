import { Controller, Get, Post, Body, Param, Put, Delete, Query } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiQuery } from '@nestjs/swagger';
import { Public } from '../../common/guards/auth.guard';
import { SourceRegistryService } from './source-registry.service';
import { CreateSourceRegistryDto } from './dto/create-source-registry.dto';

@ApiTags('源注册表')
@Controller('sources')
export class SourceRegistryController {
  constructor(private service: SourceRegistryService) {}

  /** 公开接口：客户端获取启用的源注册表（用于同步） */
  @Public()
  @Get('registry')
  @ApiOperation({ summary: '获取启用的源注册表' })
  async registry() {
    return this.service.publicRegistry();
  }

  /** 公开接口：获取单个源信息 */
  @Public()
  @Get('registry/:id')
  @ApiOperation({ summary: '获取单个源信息' })
  async getOne(@Param('id') id: string) {
    return this.service.getById(id);
  }

  /** 管理后台：列出所有源 */
  @Get()
  @ApiOperation({ summary: '列出所有源（管理后台）' })
  @ApiQuery({ name: 'enabledOnly', required: false, type: Boolean })
  async list(@Query('enabledOnly') enabledOnly?: boolean) {
    return this.service.list(enabledOnly);
  }

  /** 管理后台：创建/更新源 */
  @Post()
  @ApiOperation({ summary: '创建或更新源' })
  async upsert(@Body() dto: CreateSourceRegistryDto) {
    return this.service.upsert(dto);
  }

  /** 管理后台：启用/禁用源 */
  @Put(':id/enable')
  @ApiOperation({ summary: '启用/禁用源' })
  async toggle(@Param('id') id: string, @Body('enabled') enabled: boolean) {
    return this.service.toggle(id, enabled);
  }

  /** 管理后台：同步内置源到注册表 */
  @Post('sync-vetted')
  @ApiOperation({ summary: '同步内置源到注册表' })
  async syncVetted() {
    return this.service.syncVetted();
  }
}
