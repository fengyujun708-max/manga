import { Repository } from 'typeorm';
import { Post, PostComment, PostLike } from './entities/community.entity';
export declare class CommunityService {
    private postRepo;
    private commentRepo;
    private likeRepo;
    constructor(postRepo: Repository<Post>, commentRepo: Repository<PostComment>, likeRepo: Repository<PostLike>);
    getPosts(page?: number, limit?: number, type?: string): Promise<{
        items: any;
        total: any;
        page: number;
        limit: number;
        totalPages: number;
    }>;
    getPostDetail(id: string): Promise<any>;
    createPost(userId: string, dto: {
        title: string;
        content: string;
        tags?: string[];
        type?: string;
    }): Promise<any>;
    updatePost(userId: string, id: string, dto: {
        title: string;
        content: string;
        tags?: string[];
    }): Promise<any>;
    deletePost(userId: string, id: string): Promise<{
        message: string;
    }>;
    getComments(postId: string, page?: number, limit?: number): Promise<{
        items: any;
        total: any;
        page: number;
        limit: number;
        totalPages: number;
    }>;
    addComment(userId: string, postId: string, dto: {
        content: string;
        parentId?: string;
        replyToUserId?: string;
    }): Promise<any>;
    deleteComment(userId: string, id: string): Promise<{
        message: string;
    }>;
    toggleLike(userId: string, postId: string): Promise<{
        liked: boolean;
    }>;
}
