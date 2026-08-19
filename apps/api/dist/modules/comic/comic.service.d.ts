import { Repository } from 'typeorm';
import { Comic, Chapter, Favorite, FavoriteFolder, ReadingHistory, ComicSource } from './entities/comic.entity';
export declare class ComicService {
    private comicRepo;
    private chapterRepo;
    private favoriteRepo;
    private folderRepo;
    private historyRepo;
    private sourceRepo;
    constructor(comicRepo: Repository<Comic>, chapterRepo: Repository<Chapter>, favoriteRepo: Repository<Favorite>, folderRepo: Repository<FavoriteFolder>, historyRepo: Repository<ReadingHistory>, sourceRepo: Repository<ComicSource>);
    getHomeFeed(): Promise<{
        banner: any;
        sections: {
            id: string;
            title: string;
            type: string;
            items: any;
        }[];
    }>;
    search(q: string, page?: number, limit?: number): Promise<{
        items: any;
        total: any;
        page: number;
        limit: number;
        totalPages: number;
    }>;
    discover(dto: {
        category?: string;
        sort?: string;
        tag?: string;
        page?: number;
        limit?: number;
    }): Promise<{
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
    getChapters(comicId: string, page?: number, limit?: number): Promise<{
        items: any;
        total: any;
        page: number;
        limit: number;
        totalPages: number;
    }>;
    getRecommendations(comicId: string): Promise<any>;
    addFavorite(userId: string, comicId: string, folderId?: string): Promise<{
        message: string;
    }>;
    removeFavorite(userId: string, comicId: string): Promise<{
        message: string;
    }>;
    getFavorites(userId: string, folderId?: string): Promise<any>;
    createFolder(userId: string, name: string): Promise<any>;
    getFolders(userId: string): Promise<any>;
    updateHistory(userId: string, dto: {
        comicId: string;
        chapterId: string;
        page: number;
        progress: number;
    }): Promise<any>;
    getHistory(userId: string): Promise<any>;
    deleteHistory(userId: string, comicId: string): Promise<{
        message: string;
    }>;
}
