import { Controller, Get, Put, Param, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { NotificationService } from './notification.service';
import { JwtAuthGuard, CurrentUser } from '../../common/guards/auth.guard';

@ApiTags('通知')
@Controller('notifications')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class NotificationController {
  constructor(private notificationService: NotificationService) {}

  @Get() @ApiOperation({ summary: '通知列表' })
  async getNotifications(@CurrentUser('id') userId: string) { return this.notificationService.getNotifications(userId); }

  @Put(':id/read') @ApiOperation({ summary: '标记已读' })
  async markRead(@CurrentUser('id') userId: string, @Param('id') id: string) { return this.notificationService.markRead(userId, id); }

  @Get('unread-count') @ApiOperation({ summary: '未读通知数' })
  async getUnreadCount(@CurrentUser('id') userId: string) { return this.notificationService.getUnreadCount(userId); }
}
