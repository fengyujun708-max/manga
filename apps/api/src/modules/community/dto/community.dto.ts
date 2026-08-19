import { IsString, IsOptional, IsArray } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class CreatePostDto {
  @ApiProperty({ description: '标题' })
  @IsString()
  title: string;

  @ApiProperty({ description: '内容' })
  @IsString()
  content: string;

  @ApiProperty({ description: '标签', required: false })
  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  tags?: string[];

  @ApiProperty({ description: '类型', required: false })
  @IsOptional()
  @IsString()
  type?: string;
}

export class CreateCommentDto {
  @ApiProperty({ description: '评论内容' })
  @IsString()
  content: string;

  @ApiProperty({ description: '父评论 ID', required: false })
  @IsOptional()
  @IsString()
  parentId?: string;

  @ApiProperty({ description: '回复用户 ID', required: false })
  @IsOptional()
  @IsString()
  replyToUserId?: string;
}