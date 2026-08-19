import { Controller, Get } from '@nestjs/common';
import { ApiTags, ApiOperation } from '@nestjs/swagger';
import { AnnouncementService } from './announcement.service';
import { Public } from '../../common/guards/auth.guard';

@ApiTags('公告')
@Controller('announcements')
export class AnnouncementController {
  constructor(private announcementService: AnnouncementService) {}

  @Public() @Get() @ApiOperation({ summary: '活跃公告列表' })
  async getActiveAnnouncements() { return this.announcementService.getActiveAnnouncements(); }
}
