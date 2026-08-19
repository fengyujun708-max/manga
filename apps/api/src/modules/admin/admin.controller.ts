import { Controller, Get, Put, Param, Body, Query, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { AdminService } from './admin.service';
import { JwtAuthGuard, Roles, CurrentUser } from '../../common/guards/auth.guard';
import { RolesGuard } from '../../common/guards/auth.guard';

@ApiTags('管理后台')
@Controller('admin')
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles('super_admin', 'admin')
@ApiBearerAuth()
export class AdminController {
  constructor(private adminService: AdminService) {}

  @Get('dashboard') @ApiOperation({ summary: '仪表盘' })
  async getDashboard() { return this.adminService.getDashboard(); }

  @Get('users') @ApiOperation({ summary: '用户列表' })
  async getUsers(@Query('page') page = 1, @Query('limit') limit = 20) { return this.adminService.getUsers(page, limit); }

  @Put('users/:id/ban') @ApiOperation({ summary: '封禁用户' })
  async banUser(@CurrentUser('id') adminId: string, @Param('id') userId: string, @Body('reason') reason: string) {
    return this.adminService.banUser(adminId, userId, reason);
  }

  @Get('reports') @ApiOperation({ summary: '举报列表' })
  async getReports() { return this.adminService.getReports(); }

  @Put('reports/:id/resolve') @ApiOperation({ summary: '处理举报' })
  async resolveReport(@Param('id') id: string) { return this.adminService.resolveReport(id); }
}
