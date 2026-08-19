export declare class Post {
    id: string;
    userId: string;
    title: string;
    content: string;
    tags: string[];
    likesCount: number;
    commentsCount: number;
    favoritesCount: number;
    views: number;
    type: string;
    status: string;
    isPinned: boolean;
    isLocked: boolean;
    createdAt: Date;
    updatedAt: Date;
}
export declare class PostComicRef {
    id: string;
    postId: string;
    sourceId: string;
    comicId: string;
    comicTitle: string;
    comicCover: string;
    chapterId: string;
    chapterTitle: string;
    sortOrder: number;
    createdAt: Date;
}
export declare class PostComment {
    id: string;
    postId: string;
    userId: string;
    parentId: string;
    replyToUserId: string;
    content: string;
    likesCount: number;
    repliesCount: number;
    status: string;
    createdAt: Date;
    updatedAt: Date;
}
export declare class PostLike {
    id: string;
    userId: string;
    postId: string;
    commentId: string;
    createdAt: Date;
}
export declare class MangaRequest {
    id: string;
    userId: string;
    mangaName: string;
    altName: string;
    author: string;
    description: string;
    imageUrl: string;
    notes: string;
    status: string;
    resolvedComicId: string;
    supportCount: number;
    createdAt: Date;
    updatedAt: Date;
}
export declare class SourceRequest {
    id: string;
    userId: string;
    sourceName: string;
    sourceUrl: string;
    description: string;
    notes: string;
    status: string;
    testResult: string;
    createdAt: Date;
    updatedAt: Date;
}
export declare class SourceRegistry {
    id: string;
    sourceId: string;
    name: string;
    description: string;
    version: string;
    downloadUrl: string;
    sha256: string;
    status: string;
    minAppVersion: string;
    downloadCount: number;
    metadata: Record<string, any>;
    createdAt: Date;
    updatedAt: Date;
}
export declare class Notification {
    id: string;
    userId: string;
    type: string;
    title: string;
    body: string;
    data: Record<string, any>;
    isRead: boolean;
    createdAt: Date;
}
export declare class Announcement {
    id: string;
    title: string;
    content: string;
    priority: string;
    coverUrl: string;
    isForceRead: boolean;
    startAt: Date;
    endAt: Date;
    isActive: boolean;
    targetVersion: string;
    createdAt: Date;
    updatedAt: Date;
}
export declare class AppVersion {
    id: string;
    version: string;
    buildNumber: number;
    platform: string;
    downloadUrl: string;
    changelog: string;
    isForceUpdate: boolean;
    minVersion: string;
    isActive: boolean;
    createdAt: Date;
    updatedAt: Date;
}
export declare class Report {
    id: string;
    reporterId: string;
    targetType: string;
    targetId: string;
    reason: string;
    description: string;
    status: string;
    handledBy: string;
    createdAt: Date;
    updatedAt: Date;
}
export declare class Ban {
    id: string;
    userId: string;
    handledBy: string;
    reason: string;
    description: string;
    startAt: Date;
    endAt: Date;
    isPermanent: boolean;
    isActive: boolean;
    createdAt: Date;
    updatedAt: Date;
}
export declare class AuditLog {
    id: string;
    userId: string;
    action: string;
    resourceType: string;
    resourceId: string;
    details: Record<string, any>;
    ip: string;
    createdAt: Date;
}
export declare class RemoteConfig {
    id: string;
    key: string;
    value: any;
    description: string;
    isActive: boolean;
    createdAt: Date;
    updatedAt: Date;
}
