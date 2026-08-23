import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Between, LessThan, MoreThan } from 'typeorm';
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