import { Controller, Post, Body } from '@nestjs/common';
import { ApiTags, ApiOperation } from '@nestjs/swagger';
import { LogService } from './log.service';
import { Public } from '../../common/guards/auth.guard';

@ApiTags('客户端日志')
@Controller('logs')
export class LogController {
  constructor(private logService: LogService) {}

  @Public()
  @Post('report')
  @ApiOperation({ summary: '上报客户端日志（批量）' })
  async report(@Body() body: any) {
    return this.logService.report(body);
  }
}