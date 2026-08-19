export declare class PaginationDto {
    page?: number;
    limit?: number;
}
export declare class SearchDto extends PaginationDto {
    q: string;
}
export declare class DiscoverDto extends PaginationDto {
    category?: string;
    sort?: string;
    tag?: string;
}
export declare class AddFavoriteDto {
    comicId: string;
    folderId?: string;
}
export declare class CreateFolderDto {
    name: string;
}
export declare class UpdateHistoryDto {
    comicId: string;
    chapterId: string;
    page: number;
    progress: number;
}
