import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { MangaRequest, SourceRequest } from '../community/entities/community.entity';

@Injectable()
export class RequestService {
  constructor(
    @InjectRepository(MangaRequest) private mangaRepo: Repository<MangaRequest>,
    @InjectRepository(SourceRequest) private sourceRepo: Repository<SourceRequest>,
  ) {}

  async createMangaRequest(userId: string, dto: any) {
    return this.mangaRepo.save({ userId, ...dto });
  }

  async getMangaRequests(page = 1, limit = 20, status?: string) {
    const where: any = {};
    if (status) where.status = status;
    const [items, total] = await this.mangaRepo.findAndCount({
      where, skip: (page - 1) * limit, take: limit,
      order: { createdAt: 'DESC' },
    });
    return { items, total, page, limit, totalPages: Math.ceil(total / limit) };
  }

  async getMangaRequest(id: string) {
    const req = await this.mangaRepo.findOneBy({ id });
    if (!req) throw new NotFoundException('求漫不存在');
    return req;
  }

  async createSourceRequest(userId: string, dto: any) {
    return this.sourceRepo.save({ userId, ...dto });
  }

  async getSourceRequests(page = 1, limit = 20) {
    const [items, total] = await this.sourceRepo.findAndCount({
      skip: (page - 1) * limit, take: limit,
      order: { createdAt: 'DESC' },
    });
    return { items, total, page, limit, totalPages: Math.ceil(total / limit) };
  }
}