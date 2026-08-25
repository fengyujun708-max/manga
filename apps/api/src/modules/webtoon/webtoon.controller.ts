/**
 * Webtoon 控制接口
 * - POST /webtoon/sync           手动触发排行同步
 * - POST /webtoon/sync/:titleNo  同步单个系列详情
 * - GET  /webtoon/status         同步状态/配额统计
 * - GET  /webtoon/episode/:id/content   章节内容（未缓存则触发准备）
 */
import { Controller, Get, Post, Param, NotFoundException, Logger } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { OfficialSeries } from '../official/entities/series.entity';
import { OfficialEpisode } from '../official/entities/episode.entity';
import { OfficialChannel } from '../official/entities/channel.entity';
import { WebtoonSyncService } from './webtoon-sync.service';
import { ImageStore } from './image-store.service';

@Controller('webtoon')
export class WebtoonController {
  private readonly logger = new Logger(WebtoonController.name);

  constructor(
    private readonly sync: WebtoonSyncService,
    private readonly store: ImageStore,
    @InjectRepository(OfficialEpisode)
    private readonly episodeRepo: Repository<OfficialEpisode>,
    @InjectRepository(OfficialSeries)
    private readonly seriesRepo: Repository<OfficialSeries>,
    @InjectRepository(OfficialChannel)
    private readonly channelRepo: Repository<OfficialChannel>,
  ) {}

  @Post('sync')
  async triggerSync() {
    const r = await this.sync.syncRanking();
    return { ok: true, ...r };
  }

  @Post('sync/:titleNo')
  async triggerSeriesSync(@Param('titleNo') titleNo: string) {
    const t = Number(titleNo);
    if (!Number.isFinite(t) || t <= 0) throw new NotFoundException();
    const r = await this.sync.syncSeries(t);
    if (!r.series) return { ok: false, reason: '抓取或解析失败（详情页可能被网关拦截）' };
    return { ok: true, seriesId: r.series.id, episodes: r.episodes };
  }

  @Get('status')
  async status() {
    const ch = await this.channelRepo.findOne({ where: { slug: 'webtoon' } });
    if (!ch) return { enabled: process.env.WEBTOON_ENABLED !== 'false', series: 0, episodes: 0, quotaGb: process.env.WEBTOON_IMAGE_QUOTA_GB || 10 };
    const [series, total] = await this.seriesRepo.findAndCount({ where: { channelId: ch.id } });
    const episodes = await this.episodeRepo
      .createQueryBuilder('e')
      .innerJoin('e.series', 's')
      .where('s.channelId = :cid', { cid: ch.id })
      .getCount();
    return {
      enabled: process.env.WEBTOON_ENABLED !== 'false',
      series: total,
      episodes,
      quoteGb: process.env.WEBTOON_IMAGE_QUOTA_GB || 10,
      imageDir: this.store.rootDir,
      recent: series.slice(0, 5).map((s) => ({ id: s.id, title: s.title, coverUrl: s.coverUrl })),
    };
  }

  /** 章节内容：返回已压缩图片 URL 列表；未缓存则触发下载准备（首访可能较慢） */
  @Get('episode/:id/content')
  async episodeContent(@Param('id') id: string) {
    const ep = await this.episodeRepo.findOne({ where: { id } });
    if (!ep) throw new NotFoundException('章节不存在');
    const series = await this.seriesRepo.findOne({ where: { id: ep.seriesId } });
    if (!series) throw new NotFoundException('系列不存在');

    const { ready, pages } = await this.sync.ensureEpisodeCached(series, ep);
    if (!ready) {
      return { ready: false, episodeId: ep.id, title: ep.title, reason: '图片准备中或源站不可达' };
    }
    return {
      ready: true,
      episodeId: ep.id,
      title: ep.title,
      pages: pages.map((p) => p.url),
    };
  }
}