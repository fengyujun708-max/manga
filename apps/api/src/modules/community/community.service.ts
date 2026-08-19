import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Post, PostComment, PostLike, PostComicRef } from './entities/community.entity';

@Injectable()
export class CommunityService {
  constructor(
    @InjectRepository(Post) private postRepo: Repository<Post>,
    @InjectRepository(PostComment) private commentRepo: Repository<PostComment>,
    @InjectRepository(PostLike) private likeRepo: Repository<PostLike>,
  ) {}

  // ====== 帖子 ======
  async getPosts(page = 1, limit = 20, type?: string) {
    const where: any = { status: 'published' };
    if (type) where.type = type;

    const [items, total] = await this.postRepo.findAndCount({
      where,
      skip: (page - 1) * limit,
      take: limit,
      order: { isPinned: 'DESC', createdAt: 'DESC' },
    });

    return { items, total, page, limit, totalPages: Math.ceil(total / limit) };
  }

  async getPostDetail(id: string) {
    const post = await this.postRepo.findOneBy({ id });
    if (!post) throw new NotFoundException('帖子不存在');
    await this.postRepo.increment({ id }, 'views', 1);
    return post;
  }

  async createPost(userId: string, dto: { title: string; content: string; tags?: string[]; type?: string }) {
    const post = await this.postRepo.save({
      userId,
      title: dto.title,
      content: dto.content,
      tags: dto.tags || [],
      type: dto.type || 'normal',
    });
    return post;
  }

  async updatePost(userId: string, id: string, dto: { title: string; content: string; tags?: string[] }) {
    const post = await this.postRepo.findOneBy({ id });
    if (!post) throw new NotFoundException('帖子不存在');
    if (post.userId !== userId) throw new ForbiddenException('无权编辑');

    await this.postRepo.update(id, {
      title: dto.title,
      content: dto.content,
      tags: dto.tags || [],
    });
    return this.postRepo.findOneBy({ id });
  }

  async deletePost(userId: string, id: string) {
    const post = await this.postRepo.findOneBy({ id });
    if (!post) throw new NotFoundException('帖子不存在');
    if (post.userId !== userId) throw new ForbiddenException('无权删除');

    await this.postRepo.update(id, { status: 'deleted' });
    return { message: '已删除' };
  }

  // ====== 评论 ======
  async getComments(postId: string, page = 1, limit = 20) {
    const [items, total] = await this.commentRepo.findAndCount({
      where: { postId: postId, status: 'published' as any, parentId: null as any },
      skip: (page - 1) * limit,
      take: limit,
      order: { createdAt: 'DESC' },
    });

    return { items, total, page, limit, totalPages: Math.ceil(total / limit) };
  }

  async addComment(userId: string, postId: string, dto: { content: string; parentId?: string; replyToUserId?: string }) {
    const post = await this.postRepo.findOneBy({ id: postId });
    if (!post) throw new NotFoundException('帖子不存在');

    const comment = await this.commentRepo.save({
      postId,
      userId,
      content: dto.content,
      parentId: dto.parentId || null,
      replyToUserId: dto.replyToUserId || null,
    } as any);

    await this.postRepo.increment({ id: postId }, 'commentsCount', 1);
    return comment;
  }

  async deleteComment(userId: string, id: string) {
    const comment = await this.commentRepo.findOneBy({ id });
    if (!comment) throw new NotFoundException('评论不存在');
    if (comment.userId !== userId) throw new ForbiddenException('无权删除');

    await this.commentRepo.update(id, { status: 'deleted' });
    await this.postRepo.decrement({ id: comment.postId }, 'commentsCount', 1);
    return { message: '已删除' };
  }

  // ====== 点赞 ======
  async toggleLike(userId: string, postId: string) {
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
}