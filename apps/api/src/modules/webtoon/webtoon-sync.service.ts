/**
 * Webtoon 同步服务
 * - 定时任务（默认每日 03:00）：抓排行/分类页 → upsert official_series（增量）
 * - syncSeries：抓详情页 → 完整系列信息 + 章节元数据（免费/付费标记）
 * - 章节图片：懒加载（用户请求时抓 episode 页 → 压缩落盘），磁盘配额 LRU 保护
 */
import { Injectable, Logger } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { OfficialChannel } from '../official/entities/channel.entity';
import { OfficialSeries } from '../official/entities/series.entity';
import { OfficialEpisode } from '../official/entities/episode.entity';
import { WebtoonClient } from './webtoon-client.service';
import { ImageStore, StoredPage } from './image-store.service';
import { parseRankingCards, parseDetail, parseEpisodeImages, WtDetail } from './webtoon-parser';

const CHANNEL_SLUG = 'webtoon';
const FREE_ONLY = process.env.WEBTOON_FREE_ONLY !== 'false';

@Injectable()
export class WebtoonSyncService {
  private readonly logger = new Logger(WebtoonSyncService.name);
  private readonly genres = (process.env.WEBTOON_SYNC_GENRES || 'all,romance,action,comedy,slice_of_life,fantasy').split(',');

  constructor(
    @InjectRepository(OfficialChannel)
    private readonly channelRepo: Repository<OfficialChannel>,
    @InjectRepository(OfficialSeries)
    private readonly seriesRepo: Repository<OfficialSeries>,
    @InjectRepository(OfficialEpisode)
    private readonly episodeRepo: Repository<OfficialEpisode>,
    private readonly client: WebtoonClient,
    private readonly store: ImageStore,
  ) {}

  private async channel(): Promise<OfficialChannel> {
    let ch = await this.channelRepo.findOne({ where: { slug: CHANNEL_SLUG } });
    if (!ch) {
      ch = await this.channelRepo.save(
        this.channelRepo.create({
          slug: CHANNEL_SLUG,
          displayName: 'Webtoon 官方',
          description: 'LINE Webtoon 免费公开作品（自动同步）',
          provider: 'webtoon',
          status: 'ACTIVE',
          priority: 1,
        }),
      );
    }
    return ch;
  }

  seriesKey(titleNo: number): string {
    return `webtoon_${titleNo}`;
  }

  /** 定时：每日同步排行 */
  @Cron(CronExpression.EVERY_DAY_AT_3AM, { name: 'webtoon-ranking-sync' })
  async scheduledSync() {
    if (process.env.WEBTOON_ENABLED === 'false') return;
    const started = Date.now();
    const r = await this.syncRanking();
    this.logger.log(`定时同步完成: ${r.upserted} 部, ${r.cachedCovers} 封面, ${((Date.now() - started) / 1000).toFixed(1)}s`);
  }

  /** 同步排行/分类 → 标题入库（增量 upsert） */
  async syncRanking(): Promise<{ upserted: number; cachedCovers: number }> {
    const ch = await this.channel();
    let upserted = 0;
    let cachedCovers = 0;

    for (const genre of this.genres) {
      try {
        const html = await this.client.fetchRanking({ genre });
        if (!html) continue;
        const cards = parseRankingCards(html);
        for (const card of cards) {
          const providerId = `webtoon:${card.titleNo}`;
          let series = await this.seriesRepo.findOne({ where: { channelId: ch.id, providerId } });
          if (!series) {
            series = this.seriesRepo.create({
              channelId: ch.id,
              providerId,
              title: card.title,
              coverUrl: card.cover,
              genres: card.genre ? [card.genre] : [],
              providerUrl: `https://www.webtoons.com/en/detail/${card.titleNo}`,
              language: 'en',
              status: 'ONGOING',
            });
            await this.seriesRepo.save(series);
            upserted++;
          } else if (!series.coverUrl) {
            series.coverUrl = card.cover;
            await this.seriesRepo.save(series);
          }
          // 封面压到本地（失败不阻断），成功后 coverUrl 指向本地压缩图
          if (series.coverUrl && series.coverUrl.startsWith('http')) {
            const local = await this.store.cacheCover(this.seriesKey(card.titleNo), series.coverUrl, (u) => this.client.fetchImage(u));
            if (local) {
              series.coverUrl = `${process.env.PUBLIC_BASE_URL || 'http://localhost:3000'}${local}`;
              await this.seriesRepo.save(series);
              cachedCovers++;
            }
          }
          if (upserted >= 300) return { upserted, cachedCovers }; // 单轮上限，防打爆
        }
      } catch (e) {
        this.logger.warn(`排行同步失败 ${genre}: ${(e as Error).message}`);
      }
    }
    return { upserted, cachedCovers };
  }

  /** 同步单个系列详情 + 章节元数据 */
  async syncSeries(titleNo: number): Promise<{ series?: OfficialSeries; episodes: number }> {
    const ch = await this.channel();
    const html = await this.client.fetchDetail(titleNo);
    if (!html) return { episodes: 0 };
    const detail: WtDetail | null = parseDetail(html, titleNo);
    if (!detail || !detail.title) {
      this.logger.warn(`详情页解析失败或结构变化: titleNo=${titleNo}`);
      return { episodes: 0 };
    }

    let series = await this.seriesRepo.findOne({
      where: { channelId: ch.id, providerId: `webtoon:${titleNo}` },
    });
    if (!series) {
      series = this.seriesRepo.create({
        channelId: ch.id,
        providerId: `webtoon:${titleNo}`,
        title: detail.title,
        coverUrl: detail.cover,
        providerUrl: `https://www.webtoons.com/en/detail/${titleNo}`,
      });
    }
    series.title = detail.title;
    series.author = detail.author ?? series.author;
    series.artist = detail.author ?? series.artist;
    series.description = detail.description ?? series.description;
    series.genres = detail.genre ? detail.genre.split(/,\s*/) : series.genres;
    if (detail.cover) series.coverUrl = detail.cover;
    series.status = (detail.status || 'ONGOING').toUpperCase() === 'COMPLETED' ? 'COMPLETED' : 'ONGOING';
    await this.seriesRepo.save(series);

    let episodes = 0;
    for (const ep of detail.episodes) {
      if (FREE_ONLY && ep.status !== 'FREE') continue; // 只收录免费章节
      const parsed = ep.publishedAt ? new Date(ep.publishedAt) : null;
      const pub = parsed && !isNaN(parsed.getTime()) ? parsed : undefined;
      const existing = await this.episodeRepo.findOne({
        where: { seriesId: series.id, epNumber: ep.episodeNo },
      });
      if (existing) {
        existing.title = ep.title || existing.title;
        if (pub) existing.publishedAt = pub;
        existing.availability = ep.status;
        await this.episodeRepo.save(existing);
      } else {
        await this.episodeRepo.save(
          this.episodeRepo.create({
            seriesId: series.id,
            epNumber: ep.episodeNo,
            title: ep.title,
            subTitle: ep.title,
            publishedAt: pub,
            availability: ep.status,
            externalUrl: `https://www.webtoons.com/en/episode/${titleNo}/${ep.episodeNo}`,
            assetType: 'image',
            metadata: {},
          }),
        );
      }
      episodes++;
    }
    this.logger.log(`同步系列 ${detail.title} (${titleNo}): ${episodes} 章节`);
    return { series, episodes };
  }

  /** 章节图片懒缓存：抓 episode 页 → 压缩落盘（幂等） */
  async ensureEpisodeCached(series: OfficialSeries, episode: OfficialEpisode): Promise<{ ready: boolean; pages: StoredPage[] }> {
    const key = this.seriesKey(Number(series.providerId.replace('webtoon:', '')));
    const cached = await this.store.listCachedPages(key, episode.epNumber);
    if (cached.length > 0) return { ready: true, pages: cached };

    // 拉原始图片 URL（优先用已存 metadata，否则抓页）
    let urls: string[] = (episode.metadata?.images as string[]) || [];
    if (urls.length === 0) {
      const titleNo = Number(series.providerId.replace('webtoon:', ''));
      const html = await this.client.fetchEpisode(titleNo, episode.epNumber);
      if (!html) return { ready: false, pages: [] };
      urls = parseEpisodeImages(html);
      if (urls.length === 0) return { ready: false, pages: [] };
      episode.metadata = { ...(episode.metadata || {}), images: urls };
      await this.episodeRepo.save(episode);
    }

    const pages = await this.store.cacheEpisode(key, episode.epNumber, urls, (u) => this.client.fetchImage(u));
    if (pages.length > 0) {
      episode.assetUrl = pages[0].url; // 标记已缓存
      episode.metadata = { ...(episode.metadata || {}), cachedCount: pages.length };
      await this.episodeRepo.save(episode);
    }
    return { ready: pages.length > 0, pages };
  }
}