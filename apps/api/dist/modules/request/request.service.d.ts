import { Repository } from 'typeorm';
import { MangaRequest, SourceRequest } from '../community/entities/community.entity';
export declare class RequestService {
    private mangaRepo;
    private sourceRepo;
    constructor(mangaRepo: Repository<MangaRequest>, sourceRepo: Repository<SourceRequest>);
    createMangaRequest(userId: string, dto: any): Promise<any>;
    getMangaRequests(page?: number, limit?: number, status?: string): Promise<{
        items: any;
        total: any;
        page: number;
        limit: number;
        totalPages: number;
    }>;
    getMangaRequest(id: string): Promise<any>;
    createSourceRequest(userId: string, dto: any): Promise<any>;
    getSourceRequests(page?: number, limit?: number): Promise<{
        items: any;
        total: any;
        page: number;
        limit: number;
        totalPages: number;
    }>;
}
