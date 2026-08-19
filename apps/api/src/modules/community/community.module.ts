import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { CommunityController } from './community.controller';
import { CommunityService } from './community.service';
import { Post, PostComment, PostLike, PostComicRef } from './entities/community.entity';

@Module({
  imports: [TypeOrmModule.forFeature([Post, PostComment, PostLike, PostComicRef])],
  controllers: [CommunityController],
  providers: [CommunityService],
  exports: [TypeOrmModule],
})
export class CommunityModule {}