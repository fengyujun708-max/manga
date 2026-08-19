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
var _a, _b, _c;
Object.defineProperty(exports, "__esModule", { value: true });
exports.CommunityService = void 0;
const common_1 = require("@nestjs/common");
const typeorm_1 = require("@nestjs/typeorm");
const typeorm_2 = require("typeorm");
const community_entity_1 = require("./entities/community.entity");
let CommunityService = class CommunityService {
    constructor(postRepo, commentRepo, likeRepo) {
        this.postRepo = postRepo;
        this.commentRepo = commentRepo;
        this.likeRepo = likeRepo;
    }
    async getPosts(page = 1, limit = 20, type) {
        const where = { status: 'published' };
        if (type)
            where.type = type;
        const [items, total] = await this.postRepo.findAndCount({
            where,
            skip: (page - 1) * limit,
            take: limit,
            order: { isPinned: 'DESC', createdAt: 'DESC' },
        });
        return { items, total, page, limit, totalPages: Math.ceil(total / limit) };
    }
    async getPostDetail(id) {
        const post = await this.postRepo.findOneBy({ id });
        if (!post)
            throw new common_1.NotFoundException('帖子不存在');
        await this.postRepo.increment({ id }, 'views', 1);
        return post;
    }
    async createPost(userId, dto) {
        const post = await this.postRepo.save({
            userId,
            title: dto.title,
            content: dto.content,
            tags: dto.tags || [],
            type: dto.type || 'normal',
        });
        return post;
    }
    async updatePost(userId, id, dto) {
        const post = await this.postRepo.findOneBy({ id });
        if (!post)
            throw new common_1.NotFoundException('帖子不存在');
        if (post.userId !== userId)
            throw new common_1.ForbiddenException('无权编辑');
        await this.postRepo.update(id, {
            title: dto.title,
            content: dto.content,
            tags: dto.tags || [],
        });
        return this.postRepo.findOneBy({ id });
    }
    async deletePost(userId, id) {
        const post = await this.postRepo.findOneBy({ id });
        if (!post)
            throw new common_1.NotFoundException('帖子不存在');
        if (post.userId !== userId)
            throw new common_1.ForbiddenException('无权删除');
        await this.postRepo.update(id, { status: 'deleted' });
        return { message: '已删除' };
    }
    async getComments(postId, page = 1, limit = 20) {
        const [items, total] = await this.commentRepo.findAndCount({
            where: { postId, status: 'published', parentId: null },
            skip: (page - 1) * limit,
            take: limit,
            order: { createdAt: 'DESC' },
        });
        return { items, total, page, limit, totalPages: Math.ceil(total / limit) };
    }
    async addComment(userId, postId, dto) {
        const post = await this.postRepo.findOneBy({ id: postId });
        if (!post)
            throw new common_1.NotFoundException('帖子不存在');
        const comment = await this.commentRepo.save({
            postId,
            userId,
            content: dto.content,
            parentId: dto.parentId || null,
            replyToUserId: dto.replyToUserId || null,
        });
        await this.postRepo.increment({ id: postId }, 'commentsCount', 1);
        return comment;
    }
    async deleteComment(userId, id) {
        const comment = await this.commentRepo.findOneBy({ id });
        if (!comment)
            throw new common_1.NotFoundException('评论不存在');
        if (comment.userId !== userId)
            throw new common_1.ForbiddenException('无权删除');
        await this.commentRepo.update(id, { status: 'deleted' });
        await this.postRepo.decrement({ id: comment.postId }, 'commentsCount', 1);
        return { message: '已删除' };
    }
    async toggleLike(userId, postId) {
        const existing = await this.likeRepo.findOneBy({ userId, postId });
        if (existing) {
            await this.likeRepo.delete(existing.id);
            await this.postRepo.decrement({ id: postId }, 'likesCount', 1);
            return { liked: false };
        }
        await this.likeRepo.save({ userId, postId });
        await this.postRepo.increment({ id: postId }, 'likesCount', 1);
        return { liked: true };
    }
};
exports.CommunityService = CommunityService;
exports.CommunityService = CommunityService = __decorate([
    (0, common_1.Injectable)(),
    __param(0, (0, typeorm_1.InjectRepository)(community_entity_1.Post)),
    __param(1, (0, typeorm_1.InjectRepository)(community_entity_1.PostComment)),
    __param(2, (0, typeorm_1.InjectRepository)(community_entity_1.PostLike)),
    __metadata("design:paramtypes", [typeof (_a = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _a : Object, typeof (_b = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _b : Object, typeof (_c = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _c : Object])
], CommunityService);
//# sourceMappingURL=community.service.js.map