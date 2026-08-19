"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
var _a, _b, _c, _d, _e, _f;
Object.defineProperty(exports, "__esModule", { value: true });
exports.ComicService = void 0;
const common_1 = require("@nestjs/common");
const typeorm_1 = require("@nestjs/typeorm");
const typeorm_2 = require("typeorm");
const comic_entity_1 = require("./entities/comic.entity");
let ComicService = class ComicService {
    constructor(comicRepo, chapterRepo, favoriteRepo, folderRepo, historyRepo, sourceRepo) {
        this.comicRepo = comicRepo;
        this.chapterRepo = chapterRepo;
        this.favoriteRepo = favoriteRepo;
        this.folderRepo = folderRepo;
        this.historyRepo = historyRepo;
        this.sourceRepo = sourceRepo;
    }
    async getHomeFeed() {
        const sections = [
            {
                id: 'continue_reading',
                title: '继续阅读',
                type: 'horizontal',
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
    async search(q, page = 1, limit = 20) {
        const [items, total] = await this.comicRepo.findAndCount({
            where: [
                { title: (0, typeorm_2.Like)(`%${q}%`) },
                { author: (0, typeorm_2.Like)(`%${q}%`) },
                { altTitle: (0, typeorm_2.Like)(`%${q}%`) },
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
    async discover(dto) {
        const where = {};
        if (dto.tag)
            where.tags = (0, typeorm_2.Like)(`%${dto.tag}%`);
        const order = {};
        switch (dto.sort) {
            case 'popular':
                order.views = 'DESC';
                break;
            case 'rating':
                order.rating = 'DESC';
                break;
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
    async getComicDetail(id) {
        const comic = await this.comicRepo.findOneBy({ id });
        if (!comic)
            throw new common_1.NotFoundException('漫画不存在');
        return comic;
    }
    async getChapters(comicId, page = 1, limit = 50) {
        const [items, total] = await this.chapterRepo.findAndCount({
            where: { comicId },
            skip: (page - 1) * limit,
            take: limit,
            order: { chapterNumber: 'DESC' },
        });
        return { items, total, page, limit, totalPages: Math.ceil(total / limit) };
    }
    async getRecommendations(comicId) {
        const comic = await this.comicRepo.findOneBy({ id: comicId });
        if (!comic)
            throw new common_1.NotFoundException('漫画不存在');
        const tag = comic.tags?.[0];
        if (tag) {
            return this.comicRepo.find({
                where: { tags: (0, typeorm_2.Like)(`%${tag}%`) },
                take: 6,
                order: { rating: 'DESC' },
            });
        }
        return this.comicRepo.find({ take: 6, order: { views: 'DESC' } });
    }
    async addFavorite(userId, comicId, folderId) {
        const existing = await this.favoriteRepo.findOneBy({ userId, comicId });
        if (existing)
            throw new common_1.ConflictException('已收藏');
        await this.favoriteRepo.save({ userId, comicId, folderId });
        await this.comicRepo.increment({ id: comicId }, 'favoritesCount', 1);
        return { message: '收藏成功' };
    }
    async removeFavorite(userId, comicId) {
        const result = await this.favoriteRepo.delete({ userId, comicId });
        if (result.affected) {
            await this.comicRepo.decrement({ id: comicId }, 'favoritesCount', 1);
        }
        return { message: '已取消收藏' };
    }
    async getFavorites(userId, folderId) {
        const where = { userId };
        if (folderId)
            where.folderId = folderId;
        return this.favoriteRepo.find({
            where,
            relations: ['comic'],
            order: { createdAt: 'DESC' },
        });
    }
    async createFolder(userId, name) {
        const folder = await this.folderRepo.save({ userId, name });
        return folder;
    }
    async getFolders(userId) {
        return this.folderRepo.find({ where: { userId }, order: { sortOrder: 'ASC' } });
    }
    async updateHistory(userId, dto) {
        const existing = await this.historyRepo.findOneBy({ userId, comicId });
        if (existing) {
            existing.chapterId = dto.chapterId;
            existing.page = dto.page;
            existing.progress = dto.progress;
            existing.lastReadAt = new Date();
            existing.totalReadTime += 30;
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
    async getHistory(userId) {
        return this.historyRepo.find({
            where: { userId },
            relations: ['comic'],
            order: { lastReadAt: 'DESC' },
            take: 50,
        });
    }
    async deleteHistory(userId, comicId) {
        await this.historyRepo.delete({ userId, comicId });
        return { message: '已删除' };
    }
};
exports.ComicService = ComicService;
exports.ComicService = ComicService = __decorate([
    (0, common_1.Injectable)(),
    __param(0, (0, typeorm_1.InjectRepository)(comic_entity_1.Comic)),
    __param(1, (0, typeorm_1.InjectRepository)(comic_entity_1.Chapter)),
    __param(2, (0, typeorm_1.InjectRepository)(comic_entity_1.Favorite)),
    __param(3, (0, typeorm_1.InjectRepository)(comic_entity_1.FavoriteFolder)),
    __param(4, (0, typeorm_1.InjectRepository)(comic_entity_1.ReadingHistory)),
    __param(5, (0, typeorm_1.InjectRepository)(comic_entity_1.ComicSource)),
    __metadata("design:paramtypes", [typeof (_a = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _a : Object, typeof (_b = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _b : Object, typeof (_c = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _c : Object, typeof (_d = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _d : Object, typeof (_e = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _e : Object, typeof (_f = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _f : Object])
], ComicService);
//# sourceMappingURL=comic.service.js.map