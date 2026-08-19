import { IsString, IsOptional, IsInt, Min, Max } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';
import { Type } from 'class-transformer';

export class PaginationDto {
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  page?: number = 1;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(100)
  limit?: number = 20;
}

export class SearchDto extends PaginationDto {
  @ApiProperty({ description: '搜索关键词' })
  @IsString()
  q: string;
}

export class DiscoverDto extends PaginationDto {
  @ApiProperty({ description: '分类', required: false })
  @IsOptional()
  @IsString()
  category?: string;

  @ApiProperty({ description: '排序: latest/popular/rating', required: false })
  @IsOptional()
  @IsString()
  sort?: string = 'latest';

  @ApiProperty({ description: '标签', required: false })
  @IsOptional()
  @IsString()
  tag?: string;
}

export class AddFavoriteDto {
  @ApiProperty({ description: '漫画 ID' })
  @IsString()
  comicId: string;

  @ApiProperty({ description: '收藏夹 ID', required: false })
  @IsOptional()
  @IsString()
  folderId?: string;
}

export class CreateFolderDto {
  @ApiProperty({ description: '收藏夹名称' })
  @IsString()
  name: string;
}

export class UpdateHistoryDto {
  @ApiProperty({ description: '漫画 ID' })
  @IsString()
  comicId: string;

  @ApiProperty({ description: '章节 ID' })
  @IsString()
  chapterId: string;

  @ApiProperty({ description: '页码' })
  @Type(() => Number)
  @IsInt()
  @Min(0)
  page: number;

  @ApiProperty({ description: '阅读进度 0-1' })
  @Type(() => Number)
  progress: number;
}