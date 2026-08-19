import { ComicService } from './comic.service';
import { SearchDto, DiscoverDto, AddFavoriteDto, CreateFolderDto, UpdateHistoryDto } from './dto/comic.dto';
export declare class ComicController {
    private comicService;
    constructor(comicService: ComicService);
    getHomeFeed(): Promise<{
        banner: any;
        sections: {
            id: string;
            title: string;
            type: string;
            items: any;
        }[];
    }>;
    search(dto: SearchDto): Promise<{
        items: any;
        total: any;
        page: number;
        limit: number;
        totalPages: number;
    }>;
    discover(dto: DiscoverDto): Promise<{
        items: any;
        total: any;
        page: number;
        limit: number;
    }>;
    getCategories(): Promise<{
        id: string;
        name: string;
        icon: string;
    }[]>;
    getComicDetail(id: string): Promise<any>;
    getChapters(id: string, page?: number, limit?: number): Promise<{
        items: any;
        total: any;
        page: number;
        limit: number;
        totalPages: number;
    }>;
    getRecommendations(id: string): Promise<any>;
    addFavorite(userId: string, dto: AddFavoriteDto): Promise<{
        message: string;
    }>;
    removeFavorite(userId: string, comicId: string): Promise<{
        message: string;
    }>;
    getFavorites(userId: string, folderId?: string): Promise<any>;
    createFolder(userId: string, dto: CreateFolderDto): Promise<any>;
    getFolders(userId: string): Promise<any>;
    updateHistory(userId: string, dto: UpdateHistoryDto): Promise<any>;
    getHistory(userId: string): Promise<any>;
    deleteHistory(userId: string, comicId: string): Promise<{
        message: string;
    }>;
}
