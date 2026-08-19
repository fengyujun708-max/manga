import { Controller, Get, Post, Put, Delete, Param, Body, Query, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { SourceService } from './source.service';
import { JwtAuthGuard, Public, Roles, CurrentUser } from '../../common/guards/auth.guard';
import { RolesGuard } from '../../common/guards/auth.guard';
import { IsString, IsOptional, IsUrl, IsInt, Min, Max } from 'class-validator';

class CreateSourceDto {
  @IsString() @ApiOperation({ summary: '源ID' }) sourceId: string;
  @IsString() name: string;
  @IsString() version: string;
  @IsOptional() @IsString() author?: string;
  @IsOptional() @IsString() description?: string;
  @IsOptional() @IsString() icon?: string;
  @IsString() @IsUrl() downloadUrl: string;
  @IsString() sha256: string;
  @IsOptional() @IsString() minAppVersion?: string;
  @IsOptional() @IsArray() capabilities?: string[];
}

class UpdateSourceDto {
  @IsOptional() @IsString() version?: string;
  @IsOptional() @IsString() downloadUrl?: string;
  @IsOptional() @IsString() sha256?: string;
  @IsOptional() @IsString() changelog?: string;
  @IsOptional() @IsBoolean() isActive?: boolean;
}

class TestSourceDto {
  @IsString() sourceId: string;
}

@ApiTags('源注册表')
@Controller('sources')
export class SourceRegistryController {
  constructor(private sourceService: SourceService) {}

  @Public()
  @Get()
  @ApiOperation({ summary: '获取源注册表列表' })
  async getRegistry() {
    return this.sourceService.getRegistry();
  }

  @Public()
  @Get(':id')
  @ApiOperation({ summary: '获取源详情' })
  async getSource(@Param('id') id: string) {
    return this.sourceService.getSource(id);
  }

  @Public()
  @Get(':id/download')
  @ApiOperation({ summary: '获取源下载地址' })
  async getDownloadUrl(@Param('id') id: string) {
    return this.sourceService.getDownloadUrl(id);
  }

  @Public()
  @Get(':id/test')
  @ApiOperation({ summary: '测试源连接' })
  async testSource(@Param('id') id: string) {
    return this.sourceService.testSource(id);
  }

  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('super_admin', 'source_manager')
  @ApiBearerAuth()
  @Post()
  @ApiOperation({ summary: '注册新源' })
  async registerSource(@Body() dto: CreateSourceDto) {
    return this.sourceService.registerSource(dto);
  }

  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('super_admin', 'source_manager')
  @ApiBearerAuth()
  @Put(':id')
  @ApiOperation({ summary: '更新源信息' })
  async updateSource(@Param('id') id: string, @Body() dto: UpdateSourceDto) {
    return this.sourceService.updateSource(id, dto);
  }

  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('super_admin', 'source_manager')
  @ApiBearerAuth()
  @Delete(':id')
  @ApiOperation({ summary: '删除源' })
  async deleteSource(@Param('id') id: string) {
    return this.sourceService.deleteSource(id);
  }

  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('super_admin', 'source_manager')
  @ApiBearerAuth()
  @Post(':id/test')
  @ApiOperation({ summary: '测试源连接' })
  async testSource(@Param('id') id: string) {
    return this.sourceService.testSource(id);
  }

  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('super_admin')
  @ApiBearerAuth()
  @Post('sync')
  @ApiOperation({ summary: '手动触发同步' })
  async syncSources() {
    return this.sourceService.syncFromUpstream();
  }

  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('super_admin')
  @ApiBearerAuth()
  @Get('sync/status')
  @ApiOperation({ summary: '获取同步状态' })
  async getSyncStatus() {
    return this.sourceService.getSyncStatus();
  }
}