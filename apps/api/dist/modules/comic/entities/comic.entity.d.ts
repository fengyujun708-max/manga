export declare class Comic {
    id: string;
    title: string;
    altTitle: string;
    description: string;
    coverUrl: string;
    author: string;
    artist: string;
    tags: string[];
    status: string;
    chapterCount: number;
    rating: number;
    views: number;
    favoritesCount: number;
    createdAt: Date;
    updatedAt: Date;
}
export declare class ComicSource {
    id: string;
    comicId: string;
    sourceId: string;
    sourceUrl: string;
    sourceComicId: string;
    isActive: boolean;
    createdAt: Date;
    updatedAt: Date;
}
export declare class Chapter {
    id: string;
    comicId: string;
    sourceId: string;
    title: string;
    chapterNumber: number;
    pageCount: number;
    sourceUrl: string;
    isDownloaded: boolean;
    sourceUpdatedAt: Date;
    createdAt: Date;
    updatedAt: Date;
}
export declare class ReadingHistory {
    id: string;
    userId: string;
    comicId: string;
    chapterId: string;
    page: number;
    progress: number;
    lastReadAt: Date;
    totalReadTime: number;
    createdAt: Date;
    updatedAt: Date;
}
export declare class Favorite {
    id: string;
    userId: string;
    comicId: string;
    folderId: string;
    createdAt: Date;
}
export declare class FavoriteFolder {
    id: string;
    userId: string;
    name: string;
    sortOrder: number;
    createdAt: Date;
    updatedAt: Date;
}
