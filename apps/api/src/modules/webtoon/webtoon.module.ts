import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { HttpModule } from '@nestjs/axios';
import { OfficialChannel } from '../official/entities/channel.entity';
import { OfficialSeries } from '../official/entities/series.entity';
import { OfficialEpisode } from '../official/entities/episode.entity';
import { WebtoonClient } from './webtoon-client.service';
import { ImageStore } from './image-store.service';
import { WebtoonSyncService } from './webtoon-sync.service';
import { WebtoonController } from './webtoon.controller';

@Module({
  imports: [
    TypeOrmModule.forFeature([OfficialChannel, OfficialSeries, OfficialEpisode]),
    HttpModule,
  ],
  controllers: [WebtoonController],
  providers: [WebtoonClient, ImageStore, WebtoonSyncService],
  exports: [WebtoonSyncService],
})
export class WebtoonModule {}