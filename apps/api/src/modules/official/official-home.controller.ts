import { Controller, Get } from '@nestjs/common';
import { OfficialService } from './official.service';

@Controller('home')
export class OfficialHomeController {
  constructor(private readonly svc: OfficialService) {}

  @Get()
  async home() {
    return this.svc.getHomeFeed();
  }
}