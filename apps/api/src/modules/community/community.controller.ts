import { Controller, Get, Post, Put, Delete, Body, Param, Query, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { CommunityService } from './community.service';
import { JwtAuthGuard, Public, CurrentUser } from '../../common/guards/auth.guard';
import { IsString, IsOptional, IsArray } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

class CreatePostDto {
  @IsString() title: string;
  @IsString() content: string;
  @IsOptional() @IsArray() @IsString({ each: true }) tags?: string[];
  @IsOptional() @IsString() type?: string;
}

class CreateCommentDto {
  @IsString() content: string;
  @IsOptional() @IsString() parentId?: string;
  @IsOptional() @IsString() replyToUserId?: string;
}

@ApiTags('社区')
@Controller('community')
export class CommunityController {
  constructor(private communityService: CommunityService) {}

  @Public() @Get('posts') @ApiOperation({ summary: '帖子列表' })
  async getPosts(@Query('page') page = 1, @Query('limit') limit = 20, @Query('type') type?: string) {
    return this.communityService.getPosts(page, limit, type);
  }

  @Public() @Get('posts/:id') @ApiOperation({ summary: '帖子详情' })
  async getPostDetail(@Param('id') id: string) { return this.communityService.getPostDetail(id); }

  @UseGuards(JwtAuthGuard) @ApiBearerAuth()
  @Post('posts') @ApiOperation({ summary: '创建帖子' })
  async createPost(@CurrentUser('id') userId: string, @Body() dto: CreatePostDto) {
    return this.communityService.createPost(userId, dto);
  }

  @UseGuards(JwtAuthGuard) @ApiBearerAuth()
  @Put('posts/:id') @ApiOperation({ summary: '编辑帖子' })
  async updatePost(@CurrentUser('id') userId: string, @Param('id') id: string, @Body() dto: CreatePostDto) {
    return this.communityService.updatePost(userId, id, dto);
  }

  @UseGuards(JwtAuthGuard) @ApiBearerAuth()
  @Delete('posts/:id') @ApiOperation({ summary: '删除帖子' })
  async deletePost(@CurrentUser('id') userId: string, @Param('id') id: string) {
    return this.communityService.deletePost(userId, id);
  }

  @Public() @Get('posts/:id/comments') @ApiOperation({ summary: '评论列表' })
  async getComments(@Param('id') postId: string, @Query('page') page = 1, @Query('limit') limit = 20) {
    return this.communityService.getComments(postId, page, limit);
  }

  @UseGuards(JwtAuthGuard) @ApiBearerAuth()
  @Post('posts/:id/comments') @ApiOperation({ summary: '添加评论' })
  async addComment(@CurrentUser('id') userId: string, @Param('id') postId: string, @Body() dto: CreateCommentDto) {
    return this.communityService.addComment(userId, postId, dto);
  }

  @UseGuards(JwtAuthGuard) @ApiBearerAuth()
  @Delete('comments/:id') @ApiOperation({ summary: '删除评论' })
  async deleteComment(@CurrentUser('id') userId: string, @Param('id') id: string) {
    return this.communityService.deleteComment(userId, id);
  }

  @UseGuards(JwtAuthGuard) @ApiBearerAuth()
  @Post('posts/:id/like') @ApiOperation({ summary: '点赞/取消点赞' })
  async toggleLike(@CurrentUser('id') userId: string, @Param('id') postId: string) {
    return this.communityService.toggleLike(userId, postId);
  }
}