import { Controller, Get, Post, Put, Delete, Body, Param, Query, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { ComicService } from './comic.service';
import { JwtAuthGuard, Public, CurrentUser } from '../../common/guards/auth.guard';
import { SearchDto, DiscoverDto, AddFavoriteDto, CreateFolderDto, UpdateHistoryDto } from './dto/comic.dto';

@ApiTags('漫画')
@Controller('comic')
export class ComicController {
  constructor(private comicService: ComicService) {}

  // ====== 首页 ======
  @Public()
  @Get('home')
  @ApiOperation({ summary: '首页数据（Banner + 推荐板块）' })
  async getHomeFeed() {
    return this.comicService.getHomeFeed();
  }

  // ====== 搜索 ======
  @Public()
  @Get('search')
  @ApiOperation({ summary: '搜索漫画' })
  async search(@Query() dto: SearchDto) {
    return this.comicService.search(dto.q, dto.page, dto.limit);
  }

  // ====== 发现 ======
  @Public()
  @Get('discover')
  @ApiOperation({ summary: '发现页漫画列表' })
  async discover(@Query() dto: DiscoverDto) {
    return this.comicService.discover(dto);
  }

  @Public()
  @Get('categories')
  @ApiOperation({ summary: '分类列表' })
  async getCategories() {
    return this.comicService.getCategories();
  }

  // ====== 分类列表（必须在 :id 之前）=====
  // ====== 漫画详情 ======
  @Public()
  @Get(':id')
  @ApiOperation({ summary: '漫画详情' })
  async getComicDetail(@Param('id') id: string) {
    return this.comicService.getComicDetail(id);
  }

  @Public()
  @Get(':id/chapters')
  @ApiOperation({ summary: '章节列表' })
  async getChapters(@Param('id') id: string, @Query('page') page = 1, @Query('limit') limit = 50) {
    return this.comicService.getChapters(id, page, limit);
  }

  @Public()
  @Get(':id/recommend')
  @ApiOperation({ summary: '相关推荐' })
  async getRecommendations(@Param('id') id: string) {
    return this.comicService.getRecommendations(id);
  }

  // ====== 收藏 ======
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @Post('favorite')
  @ApiOperation({ summary: '添加收藏' })
  async addFavorite(@CurrentUser('id') userId: string, @Body() dto: AddFavoriteDto) {
    return this.comicService.addFavorite(userId, dto.comicId, dto.folderId);
  }

  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @Delete('favorite/:comicId')
  @ApiOperation({ summary: '取消收藏' })
  async removeFavorite(@CurrentUser('id') userId: string, @Param('comicId') comicId: string) {
    return this.comicService.removeFavorite(userId, comicId);
  }

  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @Get('favorites/list')
  @ApiOperation({ summary: '收藏列表' })
  async getFavorites(@CurrentUser('id') userId: string, @Query('folderId') folderId?: string) {
    return this.comicService.getFavorites(userId, folderId);
  }

  // ====== 收藏夹 ======
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @Post('favorites/folders')
  @ApiOperation({ summary: '创建收藏夹' })
  async createFolder(@CurrentUser('id') userId: string, @Body() dto: CreateFolderDto) {
    return this.comicService.createFolder(userId, dto.name);
  }

  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @Get('favorites/folders')
  @ApiOperation({ summary: '收藏夹列表' })
  async getFolders(@CurrentUser('id') userId: string) {
    return this.comicService.getFolders(userId);
  }

  // ====== 阅读历史 ======
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @Post('history')
  @ApiOperation({ summary: '更新阅读进度' })
  async updateHistory(@CurrentUser('id') userId: string, @Body() dto: UpdateHistoryDto) {
    return this.comicService.updateHistory(userId, dto);
  }

  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @Get('history/list')
  @ApiOperation({ summary: '阅读历史列表' })
  async getHistory(@CurrentUser('id') userId: string) {
    return this.comicService.getHistory(userId);
  }

  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @Delete('history/:comicId')
  @ApiOperation({ summary: '删除阅读记录' })
  async deleteHistory(@CurrentUser('id') userId: string, @Param('comicId') comicId: string) {
    return this.comicService.deleteHistory(userId, comicId);
  }
}