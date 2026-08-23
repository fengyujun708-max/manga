import { Controller, Get } from '@nestjs/common';
import { AggregateService } from './aggregate.service';

@Controller('home')
export class OfficialHomeController {
  constructor(private readonly aggregate: AggregateService) {}

  @Get()
  async home(): Promise<any> {
    return this.aggregate.getHome();
  }
}