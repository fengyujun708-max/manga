import { Controller, Get, Post, Body, Query, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { UpdateService } from './update.service';
import { JwtAuthGuard, Public, CurrentUser } from '../../common/guards/auth.guard';
import { IsString, IsOptional } from 'class-validator';

class RegisterDeviceDto {
  @IsString() deviceId: string;
  @IsOptional() @IsString() deviceName?: string;
  @IsOptional() @IsString() pushToken?: string;
}

@ApiTags('应用更新')
@Controller('app')
export class UpdateController {
  constructor(private updateService: UpdateService) {}

  @Public() @Get('check-update') @ApiOperation({ summary: '检查更新' })
  async checkUpdate(@Query('version') version: string, @Query('platform') platform: string) {
    return this.updateService.checkUpdate(version, platform);
  }

  @Public() @Get('config') @ApiOperation({ summary: '获取远程配置' })
  async getRemoteConfig() { return this.updateService.getRemoteConfig(); }

  @Public() @Get('announcements') @ApiOperation({ summary: '获取公告' })
  async getAnnouncements() { return this.updateService.getActiveAnnouncements(); }

  @UseGuards(JwtAuthGuard) @ApiBearerAuth()
  @Post('register-device') @ApiOperation({ summary: '注册设备推送' })
  async registerDevice(@CurrentUser('id') userId: string, @Body() dto: RegisterDeviceDto) {
    return { message: '设备已注册' };
  }
}
