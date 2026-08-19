import { Controller, Get, Post, Param, Body, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { SourceService } from './source.service';
import { JwtAuthGuard, Public } from '../../common/guards/auth.guard';
import { Roles, RolesGuard } from '../../common/guards/auth.guard';

@ApiTags('源注册表')
@Controller('sources')
export class SourceController {
  constructor(private sourceService: SourceService) {}

  @Public() @Get() @ApiOperation({ summary: '获取源注册表' })
  async getRegistry() { return this.sourceService.getRegistry(); }

  @Public() @Get(':id') @ApiOperation({ summary: '获取源详情' })
  async getSource(@Param('id') id: string) { return this.sourceService.getSource(id); }

  @Public() @Get(':id/download') @ApiOperation({ summary: '源 JS 文件下载地址' })
  async getDownloadUrl(@Param('id') id: string) { return this.sourceService.getDownloadUrl(id); }

  @UseGuards(JwtAuthGuard, RolesGuard) @Roles('super_admin', 'source_manager') @ApiBearerAuth()
  @Post() @ApiOperation({ summary: '注册源' })
  async registerSource(@Body() dto: any) { return this.sourceService.registerSource(dto); }
}
