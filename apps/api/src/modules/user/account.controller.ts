import { Controller, Delete, Get, Post, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { AccountService } from './account.service';
import { JwtAuthGuard, CurrentUser } from '../../common/guards/auth.guard';

@ApiTags('账号')
@Controller('account')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class AccountController {
  constructor(private accountService: AccountService) {}

  @Delete() @ApiOperation({ summary: '注销账号' })
  async deleteAccount(@CurrentUser('id') userId: string) { return this.accountService.deleteAccount(userId); }

  @Get('devices') @ApiOperation({ summary: '设备列表' })
  async getDevices(@CurrentUser('id') userId: string) { return this.accountService.getDevices(userId); }

  @Post('devices/logout') @ApiOperation({ summary: '退出其他设备' })
  async logoutOtherDevices(@CurrentUser('id') userId: string) { return this.accountService.logoutOtherDevices(userId); }
}
