export declare class CreatePostDto {
    title: string;
    content: string;
    tags?: string[];
    type?: string;
}
export declare class CreateCommentDto {
    content: string;
    parentId?: string;
    replyToUserId?: string;
}
