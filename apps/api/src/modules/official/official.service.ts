import { Injectable, Logger, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Between, LessThan, MoreThan, ILike } from 'typeorm';
import { Cron, CronExpression } from '@nestjs/schedule';
import { promises as fs } from 'fs';
import * as path from 'path';
import { OfficialChannel } from './entities/channel.entity';
import { OfficialSeries } from './entities/series.entity';
import { OfficialEpisode } from './entities/episode.entity';

@Injectable()
export class OfficialService {
  constructor(
    @InjectRepository(OfficialChannel)
    private readonly channelRepo: Repository<OfficialChannel>,
    @InjectRepository(OfficialSeries)
    private readonly seriesRepo: Repository<OfficialSeries>,
    @InjectRepository(OfficialEpisode)
    private readonly episodeRepo: Repository<OfficialEpisode>,
  ) {}

  // ===== 频道 =====
  async findAllChannels(): Promise<OfficialChannel[]> {
    return this.channelRepo.find({ order: { priority: 'ASC' } });
  }

  async findChannelBySlug(slug: string): Promise<OfficialChannel | null> {
    return this.channelRepo.findOne({ where: { slug, status: 'ACTIVE' } });
  }

  // ===== 系列 =====
  async findSeriesByChannel(channelId: string, options?: { limit?: number; offset?: number }): Promise<[OfficialSeries[], number]> {
    return this.seriesRepo.findAndCount({
      where: { channelId },
      order: { updatedAt: 'DESC' },
      take: options?.limit || 50,
      skip: options?.offset || 0,
    });
  }

  async findSeriesById(id: string): Promise<OfficialSeries | null> {
    return this.seriesRepo.findOne({ where: { id }, relations: ['episodes'] });
  }

  // ===== 官方优先搜索 =====
  async search(query: string, limit = 30) {
    const q = query.trim();
    if (!q) return { items: [] };
    const [series] = await this.seriesRepo.findAndCount({
      where: [
        { title: ILike(`%${q}%`) },
        { altTitle: ILike(`%${q}%`) },
        { author: ILike(`%${q}%`) },
      ],
      order: { updatedAt: 'DESC' },
      take: Math.min(Math.max(limit, 1), 50),
    });
    return { items: series.map(s => ({
      id: s.id, title: s.title, cover: s.coverUrl || '', author: s.author || '',
      sourceId: 'manjie_official', sourceName: '漫界官方', official: true,
    })) };
  }

  // ===== 首页 =====
  async getHomeFeed(): Promise<HomeFeed> {
    const channels = await this.channelRepo.find({ where: { status: 'ACTIVE' }, order: { priority: 'ASC' } });
    const channelIds = channels.map(c => c.id);

    // Hero: 取第一个活跃频道的第一个系列
    let hero: HeroSection | null = null;
    if (channelIds.length > 0) {
      const [series] = await this.seriesRepo.findAndCount({
        where: { channelId: channelIds[0] },
        order: { updatedAt: 'DESC' },
        take: 1,
        relations: ['episodes'],
      });
      if (series.length > 0) {
        const s = series[0];
        hero = {
          id: s.id,
          title: s.title,
          author: s.author || '',
          description: s.description || '',
          cover: s.coverUrl || '',
          genres: s.genres || [],
          status: s.status,
          episodeCount: s.episodes?.length || 0,
        };
      }
    }

    // Official: 取所有活跃频道的系列，混排
    const [officialSeries] = await this.seriesRepo.findAndCount({
      order: { updatedAt: 'DESC' },
      take: 30,
      relations: ['episodes'],
    });
    const officialCards: Card[] = officialSeries.map(s => ({
      id: s.id,
      title: s.title,
      author: s.author,
      cover: s.coverUrl || '',
      genres: s.genres || [],
      episodeCount: s.episodes?.length || 0,
      tag: 'UPDATE',
    }));

    // 今日更新: 24小时内更新的系列
    const yesterday = new Date(Date.now() - 24 * 60 * 60 * 1000);
    const [updatedSeries] = await this.seriesRepo.findAndCount({
      where: { updatedAt: MoreThan(yesterday) },
      order: { updatedAt: 'DESC' },
      take: 10,
      relations: ['episodes'],
    });
    const updateCards: Card[] = updatedSeries.map(s => ({
      id: s.id,
      title: s.title,
      author: s.author,
      cover: s.coverUrl || '',
      genres: s.genres || [],
      episodeCount: s.episodes?.length || 0,
      tag: 'NEW',
    }));

    return {
      hero,
      official: { title: '漫界官方', cards: officialCards.slice(0, 6) },
      updates: { title: '今日更新', cards: updateCards.slice(0, 10) },
      trending: { title: '热门', cards: officialCards.slice(0, 6) },
    };
  }

  // ============================================================
  // 官方内容管理（后台用户上传的漫画，仅元数据 + 图片 URL 清单）
  // ============================================================

  private async officialChannel(): Promise<OfficialChannel> {
    let ch = await this.channelRepo.findOne({ where: { slug: 'official' } });
    if (!ch) {
      ch = await this.channelRepo.save(
        this.channelRepo.create({
          slug: 'official',
          displayName: '漫界官方',
          description: '漫界官方内容（用户后台上传）',
          provider: 'manjie',
          status: 'ACTIVE',
          priority: 0,
        }),
      );
    }
    return ch;
  }

  /** 后台上传：创建漫画（元数据） */
  async createSeries(input: {
    title: string;
    author?: string;
    artist?: string;
    description?: string;
    genres?: string[];
    coverUrl?: string;
    status?: string;
  }): Promise<OfficialSeries> {
    if (!input.title?.trim()) throw new NotFoundException('标题必填');
    const ch = await this.officialChannel();
    const series = await this.seriesRepo.save(
      this.seriesRepo.create({
        channelId: ch.id,
        providerId: `official:${Date.now().toString(36)}`,
        title: input.title.trim(),
        author: input.author,
        artist: input.artist,
        description: input.description,
        genres: input.genres || [],
        coverUrl: input.coverUrl,
        status: (input.status || 'ONGOING').toUpperCase() === 'COMPLETED' ? 'COMPLETED' : 'ONGOING',
        language: 'zh',
      }),
    );
    return series;
  }

  /** 官方内容列表（分页） */
  async listSeries(options: { page?: number; limit?: number; sort?: string }): Promise<{ items: OfficialSeries[]; total: number; page: number; limit: number }> {
    const page = Math.max(1, options.page || 1);
    const limit = Math.min(50, Math.max(1, options.limit || 20));
    const order: any = options.sort === 'latest' ? { updatedAt: 'DESC' } : { createdAt: 'DESC' };
    const [items, total] = await this.seriesRepo.findAndCount({
      order,
      take: limit,
      skip: (page - 1) * limit,
    });
    // 同步章节数（轻量，列表展示用）
    const withCount = await Promise.all(
      items.map(async (s) => ({
        ...s,
        episodeCount: await this.episodeRepo.count({ where: { seriesId: s.id } }),
      })),
    );
    return { items: withCount, total, page, limit };
  }

  /** 后台上传：添加/更新章节（images 为图片 URL 列表） */
  async addEpisodes(seriesId: string, episodes: { epNumber?: number; title?: string; images?: string[] }[]) {
    const series = await this.seriesRepo.findOne({ where: { id: seriesId } });
    if (!series) throw new NotFoundException('漫画不存在');
    const saved = [];
    for (const ep of episodes) {
      const epNumber = Number(ep.epNumber || 0);
      if (epNumber <= 0) continue;
      let episode = await this.episodeRepo.findOne({ where: { seriesId, epNumber } });
      if (episode) {
        if (ep.title) episode.title = ep.title;
        if (Array.isArray(ep.images)) episode.metadata = { ...(episode.metadata || {}), images: ep.images };
      } else {
        episode = this.episodeRepo.create({
          seriesId,
          epNumber,
          title: ep.title || `第 ${epNumber} 话`,
          subTitle: ep.title || '',
          availability: 'FREE',
          assetType: 'url',
          metadata: Array.isArray(ep.images) ? { images: ep.images } : {},
        });
      }
      saved.push(await this.episodeRepo.save(episode));
    }
    if (saved.length > 0) {
      series.updatedAt = new Date();
      await this.seriesRepo.save(series);
    }
    return saved;
  }

  /** 删除漫画：删章节 + 清理服务器本地图片文件 */
  async deleteSeries(seriesId: string): Promise<{ deleted: boolean }> {
    const series = await this.seriesRepo.findOne({ where: { id: seriesId } });
    if (!series) throw new NotFoundException('漫画不存在');
    const episodes = await this.episodeRepo.find({ where: { seriesId } });
    // 收集本地静态文件（/static/uploads/ 前缀 → UPLOAD_DIR 对应路径）
    const files = new Set<string>();
    for (const ep of episodes) {
      const imgs = ((ep.metadata as any)?.images as string[]) || [];
      for (const u of imgs) this.collectLocalFile(files, u);
    }
    this.collectLocalFile(files, series.coverUrl || '');
    await this.episodeRepo.delete({ seriesId });
    await this.seriesRepo.delete({ id: seriesId });
    // 异步清理文件（不影响响应）
    this.removeFiles([...files]).catch(() => {});
    return { deleted: true };
  }

  private collectLocalFile(set: Set<string>, url: string) {
    if (!url.includes('/static/uploads/')) return;
    const rel = url.split('/static/uploads/')[1];
    if (!rel) return;
    const dir = process.env.UPLOAD_DIR || '/data/uploads';
    set.add(path.join(dir, ...rel.split('/')));
  }

  private async removeFiles(files: string[]) {
    for (const f of files) {
      try {
        await fs.unlink(f);
      } catch {}
    }
  }

  /** 章节内容（图片 URL 列表；源内容由客户端本地源直读，不落服务器） */
  async getEpisodeContent(episodeId: string): Promise<{ images: string[]; title?: string }> {
    const ep = await this.episodeRepo.findOne({ where: { id: episodeId } });
    if (!ep) throw new NotFoundException('章节不存在');
    const images = ((ep.metadata as any)?.images as string[]) || [];
    return { images, title: ep.title };
  }

  /**
   * 过期数据自动清理（每日 04:30）
   * 1) 删除超过 7 天且仍无任何章节的草稿漫画
   * 2) 清理 UPLOAD_DIR 中未被任何封面/章节引用的孤儿文件（>24h）
   */
  @Cron('30 4 * * *', { name: 'official-cleanup' })
  async maintenance(): Promise<{ removedSeries: number; removedFiles: number }> {
    const logger = new Logger('OfficialMaintenance');
    let removedSeries = 0;
    let removedFiles = 0;

    // 1. 空草稿系列（7 天无章节）
    const weekAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000);
    const [drafts] = await this.seriesRepo.findAndCount({ where: { createdAt: LessThan(weekAgo) } });
    for (const s of drafts) {
      const cnt = await this.episodeRepo.count({ where: { seriesId: s.id } });
      if (cnt === 0) {
        await this.seriesRepo.delete({ id: s.id });
        removedSeries++;
      }
    }

    // 2. 孤儿文件清理
    try {
      const dir = process.env.UPLOAD_DIR || '/data/uploads';
      await fs.mkdir(dir, { recursive: true });
      const used = new Set<string>();
      const allSeries = await this.seriesRepo.find();
      for (const s of allSeries) {
        if (s.coverUrl?.includes('/static/uploads/')) used.add(path.basename(s.coverUrl));
      }
      const allEpisodes = await this.episodeRepo.find();
      for (const ep of allEpisodes) {
        const imgs = ((ep.metadata as any)?.images as string[]) || [];
        for (const u of imgs) {
          if (u.includes('/static/uploads/')) used.add(path.basename(u.split('?')[0]));
        }
      }
      const dayAgo = Date.now() - 24 * 60 * 60 * 1000;
      const entries = await fs.readdir(dir, { withFileTypes: true }).catch(() => []);
      for (const ent of entries) {
        if (!ent.isFile()) continue;
        if (used.has(ent.name)) continue;
        const stat = await fs.stat(path.join(dir, ent.name)).catch(() => null);
        if (stat && stat.mtimeMs < dayAgo) {
          await fs.unlink(path.join(dir, ent.name)).catch(() => {});
          removedFiles++;
        }
      }
    } catch (e) {
      logger.warn(`孤儿文件清理失败: ${(e as Error).message}`);
    }

    if (removedSeries > 0 || removedFiles > 0) {
      logger.log(`过期清理完成: 删除 ${removedSeries} 部草稿, ${removedFiles} 个孤儿文件`);
    }
    return { removedSeries, removedFiles };
  }
}

export interface HomeFeed {
  hero: HeroSection | null;
  official: CardSection;
  updates: CardSection;
  trending: CardSection;
}

export interface HeroSection {
  id: string;
  title: string;
  author: string;
  description: string;
  cover: string;
  genres: string[];
  status: string;
  episodeCount: number;
}

export interface CardSection {
  title: string;
  cards: Card[];
}

export interface Card {
  id: string;
  title: string;
  author?: string;
  cover: string;
  genres: string[];
  episodeCount: number;
  tag?: string;
}