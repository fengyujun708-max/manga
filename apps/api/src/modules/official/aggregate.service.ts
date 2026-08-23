import { Injectable, Logger } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { firstValueFrom } from 'rxjs';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, MoreThan } from 'typeorm';
import { OfficialSeries } from './entities/series.entity';
import { OfficialEpisode } from './entities/episode.entity';

/** 可达源列表（服务器能访问的） */
const SOURCES = [
  { id: 'copy_manga', name: '拷贝漫画', weight: 1.0 },
  { id: 'baozi', name: '包子漫画', weight: 0.9 },
  { id: 'ccc', name: 'CCC追漫台', weight: 0.8 },
  { id: 'zaimanhua', name: '再漫画', weight: 0.7 },
  { id: 'manhuagui', name: '漫画柜', weight: 0.6 },
];

interface Card {
  title: string;
  cover: string;
  author?: string;
  genres?: string[];
  sourceId?: string;
  sourceName?: string;
  comicId?: string;
  seriesId?: string;
  tag?: string;
  score?: number;
}

@Injectable()
export class AggregateService {
  private readonly logger = new Logger(AggregateService.name);
  private cache: { cards: Card[]; heroes: Card[]; ts: number } | null = null;
  private readonly TTL = 10 * 60 * 1000;

  constructor(
    private readonly httpService: HttpService,
    @InjectRepository(OfficialSeries)
    private readonly seriesRepo: Repository<OfficialSeries>,
    @InjectRepository(OfficialEpisode)
    private readonly episodeRepo: Repository<OfficialEpisode>,
  ) {}

  /** 主入口：获取首页完整数据 */
  async getHome() {
    // 并行获取官方内容和聚合推荐
    const [official, aggregate] = await Promise.all([
      this.getOfficial(),
      this.getAggregate().catch(() => ({ cards: [], heroes: [] })),
    ]);

    return {
      hero: aggregate.heroes[0] || official.hero,
      official: official.section,     // 漫界官漫（用户上传）
      recommended: { title: '为你推荐', cards: aggregate.cards.slice(0, 20) },
      updates: official.updates,
      trending: { title: '热门', cards: aggregate.cards.slice(20, 35) },
    };
  }

  /** 漫界官漫（用户上传到服务器的漫画） */
  private async getOfficial() {
    const [series] = await this.seriesRepo.findAndCount({
      order: { updatedAt: 'DESC' },
      take: 10,
    });

    const cards: Card[] = series.map(s => ({
      title: s.title,
      cover: s.coverUrl || '',
      author: s.author || '',
      genres: s.genres || [],
      seriesId: s.id,
      tag: '官方',
    }));

    const hero = cards.length > 0 ? {
      ...cards[0],
      description: series[0]?.description || '',
      episodeCount: await this.episodeRepo.count({ where: { seriesId: series[0].id } }),
    } : null;

    // 24h 更新
    const yesterday = new Date(Date.now() - 24 * 60 * 60 * 1000);
    const recent = series.filter(s => s.updatedAt > yesterday);

    return {
      hero,
      section: { title: '漫界官漫', subtitle: '官方精选 · 独家内容', cards },
      updates: {
        title: '今日更新',
        cards: cards.filter(c => recent.some(r => r.id === c.seriesId)),
      },
    };
  }

  /** 聚合多源探索 */
  private async getAggregate(): Promise<{ cards: Card[]; heroes: Card[] }> {
    if (this.cache && Date.now() - this.cache.ts < this.TTL) {
      return { cards: this.cache.cards, heroes: this.cache.heroes };
    }

    const results = await Promise.allSettled(SOURCES.map(s => this.fetchExplore(s.id)));
    const seen = new Set<string>();
    const cards: Card[] = [];

    results.forEach((r, i) => {
      if (r.status !== 'fulfilled' || !Array.isArray(r.value)) return;
      for (const item of r.value) {
        if (!item?.title) continue;
        const key = item.title.toLowerCase().replace(/\s+/g, '');
        if (seen.has(key)) continue;
        seen.add(key);
        cards.push({
          title: item.title,
          cover: item.cover || '',
          author: item.author || item.subTitle || '',
          sourceId: SOURCES[i].id,
          sourceName: SOURCES[i].name,
          comicId: item.id || '',
          score: SOURCES[i].weight,
        });
      }
    });

    cards.sort((a, b) => (b.score ?? 0) - (a.score ?? 0));
    const heroes = cards.filter(c => c.cover).slice(0, 6);
    this.cache = { cards: cards.slice(0, 50), heroes, ts: Date.now() };
    return { cards: cards.slice(0, 50), heroes };
  }

  private async fetchExplore(sourceId: string): Promise<any[]> {
    try {
      const res = await firstValueFrom(
        this.httpService.get(`http://localhost:3000/v1/source-proxy/${sourceId}/explore`, { timeout: 15000 }),
      );
      const items: any[] = [];
      for (const sec of res.data?.sections || []) {
        if (Array.isArray(sec.items)) items.push(...sec.items);
      }
      return items;
    } catch { return []; }
  }
}