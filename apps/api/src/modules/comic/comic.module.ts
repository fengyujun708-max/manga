import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ComicController } from './comic.controller';
import { ComicService } from './comic.service';
import { Comic, ComicSource, Chapter, ReadingHistory, Favorite, FavoriteFolder } from './entities/comic.entity';

@Module({
  imports: [TypeOrmModule.forFeature([Comic, ComicSource, Chapter, ReadingHistory, Favorite, FavoriteFolder])],
  controllers: [ComicController],
  providers: [ComicService],
  exports: [TypeOrmModule],
})
export class ComicModule {}
