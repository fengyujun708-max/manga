import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { CommunityController } from './community.controller';
import { CommunityService } from './community.service';
import { SourceController } from '../source/source.controller';
import { SourceService } from '../source/source.service';
import { SourceRegistryController } from '../source/source-registry.controller';
import { SourceService as SourceRegistryService } from '../source/source.service';
import { Post, PostComment, PostLike, PostComicRef, SourceRegistry, SourceSyncLog } from './entities/community.entity';

@Module({
  imports: [
    TypeOrmModule.forFeature([Post, PostComment, PostLike, PostComicRef, SourceRegistry, SourceSyncLog]),
    HttpModule, // 为了 HttpService
  ],
  controllers: [
    CommunityController,
    SourceController,
    SourceRegistryController,
  ],
  providers: [
    CommunityService,
    SourceService,
    SourceRegistryService,
  ],
  exports: [TypeOrmModule, SourceService, SourceRegistryService],
})
export class CommunityModule {}
