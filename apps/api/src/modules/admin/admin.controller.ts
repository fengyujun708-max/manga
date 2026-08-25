import { Controller, Get, Put, Post, Delete, Param, Body, Query, UseGuards, UseInterceptors, UploadedFile, Logger } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth, ApiConsumes } from '@nestjs/swagger';
import { FileInterceptor } from '@nestjs/platform-express';
import { diskStorage } from 'multer';
import { promises as fs } from 'fs';
import * as path from 'path';
import { randomUUID } from 'crypto';
import { AdminService } from './admin.service';
import { OfficialService } from '../official/official.service';
import { JwtAuthGuard, Roles, CurrentUser } from '../../common/guards/auth.guard';
import { RolesGuard } from '../../common/guards/auth.guard';

@ApiTags('管理后台')
@Controller('admin')
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles('super_admin', 'admin')
@ApiBearerAuth()
export class AdminController {
  private readonly logger = new Logger(AdminController.name);
  constructor(
    private adminService: AdminService,
    private officialService: OfficialService,
  ) {}

  @Get('dashboard') @ApiOperation({ summary: '仪表盘' })
  async getDashboard() { return this.adminService.getDashboard(); }

  @Get('users') @ApiOperation({ summary: '用户列表' })
  async getUsers(@Query('page') page = 1, @Query('limit') limit = 20) { return this.adminService.getUsers(page, limit); }

  @Put('users/:id/ban') @ApiOperation({ summary: '封禁用户' })
  async banUser(@CurrentUser('id') adminId: string, @Param('id') userId: string, @Body('reason') reason: string) {
    return this.adminService.banUser(adminId, userId, reason);
  }

  @Get('reports') @ApiOperation({ summary: '举报列表' })
  async getReports() { return this.adminService.getReports(); }

  @Put('reports/:id/resolve') @ApiOperation({ summary: '处理举报' })
  async resolveReport(@Param('id') id: string) { return this.adminService.resolveReport(id); }

  // ====== 官方内容管理（漫界官方源）======

  @Get('official/status') @ApiOperation({ summary: '官方内容统计' })
  async officialStatus() {
    const list = await this.officialService.listSeries({ page: 1, limit: 1 });
    return { series: list.total };
  }

  @Get('official/series') @ApiOperation({ summary: '官方漫画列表' })
  async officialSeries(@Query('page') page = 1, @Query('limit') limit = 20) {
    return this.officialService.listSeries({ page: Number(page), limit: Number(limit), sort: 'latest' });
  }

  @Post('official/series') @ApiOperation({ summary: '创建官方漫画' })
  async createSeries(@Body() dto: {
    title: string; author?: string; artist?: string; description?: string;
    genres?: string[]; coverUrl?: string; status?: string;
  }) {
    const s = await this.officialService.createSeries(dto);
    return { ok: true, id: s.id, title: s.title };
  }

  @Post('official/series/:id/episodes') @ApiOperation({ summary: '添加/更新章节（images=图片URL列表）' })
  async addEpisodes(
    @Param('id') id: string,
    @Body() dto: { episodes: { epNumber?: number; title?: string; images?: string[] }[] },
  ) {
    const saved = await this.officialService.addEpisodes(id, dto.episodes || []);
    return { ok: true, saved: saved.length };
  }

  @Delete('official/series/:id') @ApiOperation({ summary: '删除官方漫画（含本地图片文件）' })
  async deleteSeries(@Param('id') id: string) {
    return this.officialService.deleteSeries(id);
  }

  @Post('official/upload') @ApiOperation({ summary: '上传图片（封面/章节图）' })
  @ApiConsumes('multipart/form-data')
  @UseInterceptors(FileInterceptor('file', {
    storage: diskStorage({
      destination: async (_req, _file, cb) => {
        const dir = process.env.UPLOAD_DIR || '/data/uploads';
        const gallery = path.join(dir, 'gallery');
        await fs.mkdir(gallery, { recursive: true });
        cb(null, gallery);
      },
      filename: (_req, file, cb) => {
        const ext = (path.extname(file.originalname) || '.jpg').toLowerCase();
        cb(null, `${Date.now().toString(36)}-${randomUUID().slice(0, 8)}${ext}`);
      },
    }),
    limits: { fileSize: 20 * 1024 * 1024 },
  }))
  async uploadImage(@UploadedFile() file?: any) {
    if (!file) return { ok: false, reason: '未收到文件' };
    const base = process.env.PUBLIC_BASE_URL || 'http://localhost:3000';
    const url = `${base}/static/uploads/gallery/${file.filename}`;
    return { ok: true, url, filename: file.filename, size: file.size };
  }
}
