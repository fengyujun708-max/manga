import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { HttpModule } from '@nestjs/axios';
import { ScheduleModule } from '@nestjs/schedule';
import { SourceController } from './source.controller';
import { SourceService } from './source.service';
import { SourceRegistry } from '../community/entities/community.entity';
import { SourceSyncLog } from '../community/entities/source-sync-log.entity';

@Module({
  imports: [
    TypeOrmModule.forFeature([SourceRegistry, SourceSyncLog]),
    HttpModule,
    ScheduleModule.forRoot(),
  ],
  controllers: [SourceController],
  providers: [SourceService],
  exports: [SourceService],
})
export class SourceModule {}
