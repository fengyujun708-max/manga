import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { RequestController } from './request.controller';
import { RequestService } from './request.service';
import { MangaRequest, SourceRequest } from '../community/entities/community.entity';

@Module({
  imports: [TypeOrmModule.forFeature([MangaRequest, SourceRequest])],
  controllers: [RequestController],
  providers: [RequestService],
  exports: [TypeOrmModule],
})
export class RequestModule {}
