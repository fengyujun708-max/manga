import { Injectable, NotFoundException, ConflictException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Like } from 'typeorm';
import { Comic, Chapter, Favorite, FavoriteFolder, ReadingHistory, ComicSource } from './entities/comic.entity';

@Injectable()
export class ComicService {
  constructor(
    @InjectRepository(Comic) private comicRepo: Repository<Comic>,
    @InjectRepository(Chapter) private chapterRepo: Repository<Chapter>,
    @InjectRepository(Favorite) private favoriteRepo: Repository<Favorite>,
    @InjectRepository(FavoriteFolder) private folderRepo: Repository<FavoriteFolder>,
    @InjectRepository(ReadingHistory) private historyRepo: Repository<ReadingHistory>,
    @InjectRepository(ComicSource) private sourceRepo: Repository<ComicSource>,
  ) {}

  // ====== 首页 ======
  async getHomeFeed() {
    const sections = [
      {
        id: 'continue_reading',
        title: '继续阅读',
        type: 'horizontal',
        // 需要 userId 才能返回，这里返回空
        items: [],
      },
      {
        id: 'recent_updates',
        title: '最近更新',
        type: 'horizontal',
        items: await this.comicRepo.find({
          order: { updatedAt: 'DESC' },
          take: 10,
        }),
      },
      {
        id: 'recommended',
        title: '猜你喜欢',
        type: 'horizontal',
        items: await this.comicRepo.find({
          order: { rating: 'DESC' },
          take: 10,
        }),
      },
      {
        id: 'popular',
        title: '热门漫画',
        type: 'horizontal',
        items: await this.comicRepo.find({
          order: { views: 'DESC' },
          take: 10,
        }),
      },
    ];

    return {
      banner: await this.comicRepo.find({
        order: { views: 'DESC' },
        take: 5,
      }),
      sections,
    };
  }

  // ====== 搜索 ======
  async search(q: string, page = 1, limit = 20) {
    const [items, total] = await this.comicRepo.findAndCount({
      where: [
        { title: Like(`%${q}%`) },
        { author: Like(`%${q}%`) },
        { altTitle: Like(`%${q}%`) },
      ],
      skip: (page - 1) * limit,
      take: limit,
      order: { views: 'DESC' },
    });

    return {
      items,
      total,
      page,
      limit,
      totalPages: Math.ceil(total / limit),
    };
  }

  // ====== 发现 ======
  async discover(dto: { category?: string; sort?: string; tag?: string; page?: number; limit?: number }) {
    const where: any = {};
    if (dto.tag) where.tags = Like(`%${dto.tag}%`);

    const order: any = {};
    switch (dto.sort) {
      case 'popular': order.views = 'DESC'; break;
      case 'rating': order.rating = 'DESC'; break;
      default: order.updatedAt = 'DESC';
    }

    const [items, total] = await this.comicRepo.findAndCount({
      where,
      skip: ((dto.page || 1) - 1) * (dto.limit || 20),
      take: dto.limit || 20,
      order,
    });

    return { items, total, page: dto.page || 1, limit: dto.limit || 20 };
  }

  async getCategories() {
    // 返回预设分类（后续可改成动态标签聚合）
    return [
      { id: 'popular', name: '热门', icon: '🔥' },
      { id: 'latest', name: '最新', icon: '✨' },
      { id: 'action', name: '热血', icon: '⚔️' },
      { id: 'romance', name: '恋爱', icon: '💕' },
      { id: 'comedy', name: '搞笑', icon: '😂' },
      { id: 'horror', name: '恐怖', icon: '👻' },
      { id: 'fantasy', name: '奇幻', icon: '🧙' },
      { id: 'sci-fi', name: '科幻', icon: '🚀' },
      { id: 'school', name: '校园', icon: '📚' },
      { id: 'mystery', name: '悬疑', icon: '🔍' },
    ];
  }

  // ====== 漫画详情 ======
  async getComicDetail(id: string) {
    const comic = await this.comicRepo.findOneBy({ id });
    if (!comic) throw new NotFoundException('漫画不存在');
    return comic;
  }

  async getChapters(comicId: string, page = 1, limit = 50) {
    const [items, total] = await this.chapterRepo.findAndCount({
      where: { comicId },
      skip: (page - 1) * limit,
      take: limit,
      order: { chapterNumber: 'DESC' },
    });

    return { items, total, page, limit, totalPages: Math.ceil(total / limit) };
  }

  async getRecommendations(comicId: string) {
    const comic = await this.comicRepo.findOneBy({ id: comicId });
    if (!comic) throw new NotFoundException('漫画不存在');

    // 按标签推荐
    const tag = comic.tags?.[0];
    if (tag) {
      return this.comicRepo.find({
        where: { tags: Like(`%${tag}%`) },
        take: 6,
        order: { rating: 'DESC' },
      });
    }
    return this.comicRepo.find({ take: 6, order: { views: 'DESC' } });
  }

  // ====== 收藏 ======
  async addFavorite(userId: string, comicId: string, folderId?: string) {
    const existing = await this.favoriteRepo.findOneBy({ userId, comicId });
    if (existing) throw new ConflictException('已收藏');

    await this.favoriteRepo.save({ userId, comicId, folderId });
    await this.comicRepo.increment({ id: comicId }, 'favoritesCount', 1);
    return { message: '收藏成功' };
  }

  async removeFavorite(userId: string, comicId: string) {
    const result = await this.favoriteRepo.delete({ userId, comicId });
    if (result.affected) {
      await this.comicRepo.decrement({ id: comicId }, 'favoritesCount', 1);
    }
    return { message: '已取消收藏' };
  }

  async getFavorites(userId: string, folderId?: string) {
    const where: any = { userId };
    if (folderId) where.folderId = folderId;

    return this.favoriteRepo.find({
      where,
      relations: ['comic'],
      order: { createdAt: 'DESC' },
    });
  }

  async createFolder(userId: string, name: string) {
    const folder = await this.folderRepo.save({ userId, name });
    return folder;
  }

  async getFolders(userId: string) {
    return this.folderRepo.find({ where: { userId }, order: { sortOrder: 'ASC' } });
  }

  // ====== 阅读历史 ======
  async updateHistory(userId: string, dto: { comicId: string; chapterId: string; page: number; progress: number }) {
    const existing = await this.historyRepo.findOneBy({ userId, comicId: dto.comicId });
    if (existing) {
      existing.chapterId = dto.chapterId;
      existing.page = dto.page;
      existing.progress = dto.progress;
      existing.lastReadAt = new Date();
      existing.totalReadTime += 30; // 增加30秒阅读时长
      return this.historyRepo.save(existing);
    }
    return this.historyRepo.save({
      userId,
      comicId: dto.comicId,
      chapterId: dto.chapterId,
      page: dto.page,
      progress: dto.progress,
      lastReadAt: new Date(),
      totalReadTime: 30,
    });
  }

  async getHistory(userId: string) {
    return this.historyRepo.find({
      where: { userId },
      relations: ['comic'],
      order: { lastReadAt: 'DESC' },
      take: 50,
    });
  }

  async deleteHistory(userId: string, comicId: string) {
    await this.historyRepo.delete({ userId, comicId });
    return { message: '已删除' };
  }
}