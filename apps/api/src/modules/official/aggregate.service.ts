import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, MoreThan } from 'typeorm';
import { OfficialSeries } from './entities/series.entity';
import { OfficialEpisode } from './entities/episode.entity';

interface Card {
  id: string;
  title: string;
  cover: string;
  author?: string;
  genres?: string[];
  seriesId?: string;
  tag?: string;
  sourceId?: string;
  sourceName?: string;
}

/** 标题归一化（首页聚合去重键） */
function normalizeTitle(t: string): string {
  return t
    .toLowerCase()
    .replace(/[\s\-_.,:;!@#$%^&*()[\]{}<>?/\\|`~·「」『』【】"'']+/g, '');
}

@Injectable()
export class AggregateService {
  constructor(
    @InjectRepository(OfficialSeries)
    private readonly seriesRepo: Repository<OfficialSeries>,
    @InjectRepository(OfficialEpisode)
    private readonly episodeRepo: Repository<OfficialEpisode>,
  ) {}

  /** 聚合首页：漫界官方（后台上传内容，实时读取 + 标题去重） */
  async getHome() {
    const official = await this.getOfficial();

    // 标题去重（保留最新一条）
    const seen = new Set<string>();
    const uniq = official.cards.filter((c) => {
      const k = normalizeTitle(c.title || '');
      if (!k || seen.has(k)) return false;
      seen.add(k);
      return true;
    });

    const updateIds = new Set(official.updateCards.map((c) => c.id));
    return {
      hero: official.hero,
      official: { title: '漫界官方', subtitle: '官方精选 · 实时更新', cards: uniq.slice(0, 20) },
      updates: { title: '今日更新', cards: uniq.filter((c) => updateIds.has(c.id)).slice(0, 10) },
      trending: { title: '热门', cards: uniq.slice(0, 12) },
      updatedAt: new Date().toISOString(),
    };
  }

  /** 官方内容：全部系列（最新排序）+ hero + 24h 更新 */
  private async getOfficial() {
    const [series] = await this.seriesRepo.findAndCount({
      order: { updatedAt: 'DESC' },
      take: 200,
    });

    const cards: Card[] = series.map((s) => ({
      id: s.id,
      title: s.title,
      cover: s.coverUrl || '',
      author: s.author || '',
      genres: s.genres || [],
      seriesId: s.id,
      tag: '官方',
      sourceId: 'manjie_official',
      sourceName: '漫界官方',
    }));

    const hero = cards.length > 0
      ? {
          ...cards[0],
          description: series[0]?.description || '',
          episodeCount: await this.episodeRepo.count({ where: { seriesId: series[0].id } }),
        }
      : null;

    const yesterday = new Date(Date.now() - 24 * 60 * 60 * 1000);
    const updateCards = await this.seriesRepo.find({
      where: { updatedAt: MoreThan(yesterday) },
      order: { updatedAt: 'DESC' },
      take: 10,
    });
    const updates: Card[] = updateCards.map((s) => ({
      id: s.id,
      title: s.title,
      cover: s.coverUrl || '',
      author: s.author || '',
      seriesId: s.id,
      tag: 'UPDATED',
      sourceId: 'manjie_official',
      sourceName: '漫界官方',
    }));

    return {
      hero,
      cards,
      updateCards: updates,
      section: { title: '漫界官方', subtitle: '官方精选 · 实时更新', cards },
    };
  }
}