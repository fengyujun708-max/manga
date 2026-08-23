import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { HttpModule } from '@nestjs/axios';
import { OfficialChannel } from './entities/channel.entity';
import { OfficialSeries } from './entities/series.entity';
import { OfficialEpisode } from './entities/episode.entity';
import { OfficialService } from './official.service';
import { AggregateService } from './aggregate.service';
import { OfficialController } from './official.controller';
import { OfficialHomeController } from './official-home.controller';

@Module({
  imports: [
    TypeOrmModule.forFeature([OfficialChannel, OfficialSeries, OfficialEpisode]),
    HttpModule,
  ],
  controllers: [OfficialController, OfficialHomeController],
  providers: [OfficialService, AggregateService],
  exports: [OfficialService, AggregateService],
})
export class OfficialModule {}