import { RequestService } from './request.service';
declare class CreateMangaRequestDto {
    mangaName: string;
    altName?: string;
    author?: string;
    description?: string;
    notes?: string;
}
declare class CreateSourceRequestDto {
    sourceName: string;
    sourceUrl?: string;
    description?: string;
    notes?: string;
}
export declare class RequestController {
    private requestService;
    constructor(requestService: RequestService);
    createMangaRequest(userId: string, dto: CreateMangaRequestDto): Promise<any>;
    getMangaRequests(page?: number, limit?: number, status?: string): Promise<{
        items: any;
        total: any;
        page: number;
        limit: number;
        totalPages: number;
    }>;
    getMangaRequest(id: string): Promise<any>;
    createSourceRequest(userId: string, dto: CreateSourceRequestDto): Promise<any>;
    getSourceRequests(page?: number, limit?: number): Promise<{
        items: any;
        total: any;
        page: number;
        limit: number;
        totalPages: number;
    }>;
}
export {};
