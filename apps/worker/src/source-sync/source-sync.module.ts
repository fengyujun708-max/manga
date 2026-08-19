import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { SourceSyncService } from './source-sync.service';
import { SourceRegistry } from '../community/entities/source-registry.entity';
import { SourceSyncLog } from '../community/entities/source-sync-log.entity';

@Module({
  imports: [TypeOrmModule.forFeature([SourceRegistry, SourceSyncLog])],
  providers: [SourceSyncService],
  exports: [SourceSyncService],
})
export class SourceSyncModule {}