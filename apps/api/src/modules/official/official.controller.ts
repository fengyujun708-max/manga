import { Controller, Get, Param, Query, NotFoundException } from '@nestjs/common';
import { OfficialService } from './official.service';

@Controller('v1/official')
export class OfficialController {
  constructor(private readonly svc: OfficialService) {}

  @Get('channels')
  async listChannels() {
    return this.svc.findAllChannels();
  }

  @Get('channels/:slug')
  async getChannel(@Param('slug') slug: string) {
    const ch = await this.svc.findChannelBySlug(slug);
    if (!ch) throw new NotFoundException();
    const [series] = await this.svc.findSeriesByChannel(ch.id);
    return { channel: ch, series };
  }

  @Get('series/:id')
  async getSeries(@Param('id') id: string) {
    const s = await this.svc.findSeriesById(id);
    if (!s) throw new NotFoundException();
    return s;
  }
}