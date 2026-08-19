"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.RemoteConfig = exports.AuditLog = exports.Ban = exports.Report = exports.AppVersion = exports.Announcement = exports.Notification = exports.SourceRegistry = exports.SourceRequest = exports.MangaRequest = exports.PostLike = exports.PostComment = exports.PostComicRef = exports.Post = void 0;
const typeorm_1 = require("typeorm");
let Post = class Post {
};
exports.Post = Post;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], Post.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], Post.prototype, "userId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 200 }),
    __metadata("design:type", String)
], Post.prototype, "title", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'text' }),
    __metadata("design:type", String)
], Post.prototype, "content", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'simple-array', nullable: true }),
    __metadata("design:type", Array)
], Post.prototype, "tags", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'int', default: 0 }),
    __metadata("design:type", Number)
], Post.prototype, "likesCount", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'int', default: 0 }),
    __metadata("design:type", Number)
], Post.prototype, "commentsCount", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'int', default: 0 }),
    __metadata("design:type", Number)
], Post.prototype, "favoritesCount", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'int', default: 0 }),
    __metadata("design:type", Number)
], Post.prototype, "views", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 50, default: 'normal' }),
    __metadata("design:type", String)
], Post.prototype, "type", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 20, default: 'published' }),
    __metadata("design:type", String)
], Post.prototype, "status", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'boolean', default: false }),
    __metadata("design:type", Boolean)
], Post.prototype, "isPinned", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'boolean', default: false }),
    __metadata("design:type", Boolean)
], Post.prototype, "isLocked", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], Post.prototype, "createdAt", void 0);
__decorate([
    (0, typeorm_1.UpdateDateColumn)(),
    __metadata("design:type", Date)
], Post.prototype, "updatedAt", void 0);
exports.Post = Post = __decorate([
    (0, typeorm_1.Entity)('posts')
], Post);
let PostComicRef = class PostComicRef {
};
exports.PostComicRef = PostComicRef;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], PostComicRef.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], PostComicRef.prototype, "postId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 100 }),
    __metadata("design:type", String)
], PostComicRef.prototype, "sourceId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 255 }),
    __metadata("design:type", String)
], PostComicRef.prototype, "comicId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 255, nullable: true }),
    __metadata("design:type", String)
], PostComicRef.prototype, "comicTitle", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 500, nullable: true }),
    __metadata("design:type", String)
], PostComicRef.prototype, "comicCover", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 255, nullable: true }),
    __metadata("design:type", String)
], PostComicRef.prototype, "chapterId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 255, nullable: true }),
    __metadata("design:type", String)
], PostComicRef.prototype, "chapterTitle", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'int', default: 0 }),
    __metadata("design:type", Number)
], PostComicRef.prototype, "sortOrder", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], PostComicRef.prototype, "createdAt", void 0);
exports.PostComicRef = PostComicRef = __decorate([
    (0, typeorm_1.Entity)('post_comics_refs')
], PostComicRef);
let PostComment = class PostComment {
};
exports.PostComment = PostComment;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], PostComment.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], PostComment.prototype, "postId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], PostComment.prototype, "userId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid', nullable: true }),
    __metadata("design:type", String)
], PostComment.prototype, "parentId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid', nullable: true }),
    __metadata("design:type", String)
], PostComment.prototype, "replyToUserId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'text' }),
    __metadata("design:type", String)
], PostComment.prototype, "content", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'int', default: 0 }),
    __metadata("design:type", Number)
], PostComment.prototype, "likesCount", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'int', default: 0 }),
    __metadata("design:type", Number)
], PostComment.prototype, "repliesCount", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 20, default: 'published' }),
    __metadata("design:type", String)
], PostComment.prototype, "status", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], PostComment.prototype, "createdAt", void 0);
__decorate([
    (0, typeorm_1.UpdateDateColumn)(),
    __metadata("design:type", Date)
], PostComment.prototype, "updatedAt", void 0);
exports.PostComment = PostComment = __decorate([
    (0, typeorm_1.Entity)('post_comments')
], PostComment);
let PostLike = class PostLike {
};
exports.PostLike = PostLike;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], PostLike.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], PostLike.prototype, "userId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], PostLike.prototype, "postId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid', nullable: true }),
    __metadata("design:type", String)
], PostLike.prototype, "commentId", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], PostLike.prototype, "createdAt", void 0);
exports.PostLike = PostLike = __decorate([
    (0, typeorm_1.Entity)('post_likes'),
    (0, typeorm_1.Index)(['userId', 'postId'], { unique: true })
], PostLike);
let MangaRequest = class MangaRequest {
};
exports.MangaRequest = MangaRequest;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], MangaRequest.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], MangaRequest.prototype, "userId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 200 }),
    __metadata("design:type", String)
], MangaRequest.prototype, "mangaName", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 200, nullable: true }),
    __metadata("design:type", String)
], MangaRequest.prototype, "altName", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 200, nullable: true }),
    __metadata("design:type", String)
], MangaRequest.prototype, "author", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'text', nullable: true }),
    __metadata("design:type", String)
], MangaRequest.prototype, "description", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 500, nullable: true }),
    __metadata("design:type", String)
], MangaRequest.prototype, "imageUrl", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'text', nullable: true }),
    __metadata("design:type", String)
], MangaRequest.prototype, "notes", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 20, default: 'pending' }),
    __metadata("design:type", String)
], MangaRequest.prototype, "status", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid', nullable: true }),
    __metadata("design:type", String)
], MangaRequest.prototype, "resolvedComicId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'int', default: 0 }),
    __metadata("design:type", Number)
], MangaRequest.prototype, "supportCount", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], MangaRequest.prototype, "createdAt", void 0);
__decorate([
    (0, typeorm_1.UpdateDateColumn)(),
    __metadata("design:type", Date)
], MangaRequest.prototype, "updatedAt", void 0);
exports.MangaRequest = MangaRequest = __decorate([
    (0, typeorm_1.Entity)('manga_requests')
], MangaRequest);
let SourceRequest = class SourceRequest {
};
exports.SourceRequest = SourceRequest;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], SourceRequest.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], SourceRequest.prototype, "userId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 200 }),
    __metadata("design:type", String)
], SourceRequest.prototype, "sourceName", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 500, nullable: true }),
    __metadata("design:type", String)
], SourceRequest.prototype, "sourceUrl", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'text', nullable: true }),
    __metadata("design:type", String)
], SourceRequest.prototype, "description", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'text', nullable: true }),
    __metadata("design:type", String)
], SourceRequest.prototype, "notes", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 20, default: 'pending' }),
    __metadata("design:type", String)
], SourceRequest.prototype, "status", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'text', nullable: true }),
    __metadata("design:type", String)
], SourceRequest.prototype, "testResult", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], SourceRequest.prototype, "createdAt", void 0);
__decorate([
    (0, typeorm_1.UpdateDateColumn)(),
    __metadata("design:type", Date)
], SourceRequest.prototype, "updatedAt", void 0);
exports.SourceRequest = SourceRequest = __decorate([
    (0, typeorm_1.Entity)('source_requests')
], SourceRequest);
let SourceRegistry = class SourceRegistry {
};
exports.SourceRegistry = SourceRegistry;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], SourceRegistry.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 100, unique: true }),
    __metadata("design:type", String)
], SourceRegistry.prototype, "sourceId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 200 }),
    __metadata("design:type", String)
], SourceRegistry.prototype, "name", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'text', nullable: true }),
    __metadata("design:type", String)
], SourceRegistry.prototype, "description", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 100 }),
    __metadata("design:type", String)
], SourceRegistry.prototype, "version", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 500 }),
    __metadata("design:type", String)
], SourceRegistry.prototype, "downloadUrl", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 64 }),
    __metadata("design:type", String)
], SourceRegistry.prototype, "sha256", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 20, default: 'active' }),
    __metadata("design:type", String)
], SourceRegistry.prototype, "status", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 50, nullable: true }),
    __metadata("design:type", String)
], SourceRegistry.prototype, "minAppVersion", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'int', default: 0 }),
    __metadata("design:type", Number)
], SourceRegistry.prototype, "downloadCount", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'json', nullable: true }),
    __metadata("design:type", Object)
], SourceRegistry.prototype, "metadata", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], SourceRegistry.prototype, "createdAt", void 0);
__decorate([
    (0, typeorm_1.UpdateDateColumn)(),
    __metadata("design:type", Date)
], SourceRegistry.prototype, "updatedAt", void 0);
exports.SourceRegistry = SourceRegistry = __decorate([
    (0, typeorm_1.Entity)('source_registry')
], SourceRegistry);
let Notification = class Notification {
};
exports.Notification = Notification;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], Notification.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], Notification.prototype, "userId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 50 }),
    __metadata("design:type", String)
], Notification.prototype, "type", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 200 }),
    __metadata("design:type", String)
], Notification.prototype, "title", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'text', nullable: true }),
    __metadata("design:type", String)
], Notification.prototype, "body", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'json', nullable: true }),
    __metadata("design:type", Object)
], Notification.prototype, "data", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'boolean', default: false }),
    __metadata("design:type", Boolean)
], Notification.prototype, "isRead", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], Notification.prototype, "createdAt", void 0);
exports.Notification = Notification = __decorate([
    (0, typeorm_1.Entity)('notifications'),
    (0, typeorm_1.Index)(['userId', 'isRead'])
], Notification);
let Announcement = class Announcement {
};
exports.Announcement = Announcement;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], Announcement.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 200 }),
    __metadata("design:type", String)
], Announcement.prototype, "title", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'text' }),
    __metadata("design:type", String)
], Announcement.prototype, "content", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 50, default: 'normal' }),
    __metadata("design:type", String)
], Announcement.prototype, "priority", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 500, nullable: true }),
    __metadata("design:type", String)
], Announcement.prototype, "coverUrl", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'boolean', default: false }),
    __metadata("design:type", Boolean)
], Announcement.prototype, "isForceRead", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'timestamp', nullable: true }),
    __metadata("design:type", Date)
], Announcement.prototype, "startAt", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'timestamp', nullable: true }),
    __metadata("design:type", Date)
], Announcement.prototype, "endAt", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'boolean', default: true }),
    __metadata("design:type", Boolean)
], Announcement.prototype, "isActive", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 50, nullable: true }),
    __metadata("design:type", String)
], Announcement.prototype, "targetVersion", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], Announcement.prototype, "createdAt", void 0);
__decorate([
    (0, typeorm_1.UpdateDateColumn)(),
    __metadata("design:type", Date)
], Announcement.prototype, "updatedAt", void 0);
exports.Announcement = Announcement = __decorate([
    (0, typeorm_1.Entity)('announcements')
], Announcement);
let AppVersion = class AppVersion {
};
exports.AppVersion = AppVersion;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], AppVersion.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 20 }),
    __metadata("design:type", String)
], AppVersion.prototype, "version", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'int' }),
    __metadata("design:type", Number)
], AppVersion.prototype, "buildNumber", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 50 }),
    __metadata("design:type", String)
], AppVersion.prototype, "platform", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 500 }),
    __metadata("design:type", String)
], AppVersion.prototype, "downloadUrl", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'text', nullable: true }),
    __metadata("design:type", String)
], AppVersion.prototype, "changelog", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'boolean', default: false }),
    __metadata("design:type", Boolean)
], AppVersion.prototype, "isForceUpdate", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 20, nullable: true }),
    __metadata("design:type", String)
], AppVersion.prototype, "minVersion", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'boolean', default: true }),
    __metadata("design:type", Boolean)
], AppVersion.prototype, "isActive", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], AppVersion.prototype, "createdAt", void 0);
__decorate([
    (0, typeorm_1.UpdateDateColumn)(),
    __metadata("design:type", Date)
], AppVersion.prototype, "updatedAt", void 0);
exports.AppVersion = AppVersion = __decorate([
    (0, typeorm_1.Entity)('app_versions')
], AppVersion);
let Report = class Report {
};
exports.Report = Report;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], Report.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], Report.prototype, "reporterId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 50 }),
    __metadata("design:type", String)
], Report.prototype, "targetType", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], Report.prototype, "targetId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 50 }),
    __metadata("design:type", String)
], Report.prototype, "reason", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'text', nullable: true }),
    __metadata("design:type", String)
], Report.prototype, "description", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 20, default: 'pending' }),
    __metadata("design:type", String)
], Report.prototype, "status", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid', nullable: true }),
    __metadata("design:type", String)
], Report.prototype, "handledBy", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], Report.prototype, "createdAt", void 0);
__decorate([
    (0, typeorm_1.UpdateDateColumn)(),
    __metadata("design:type", Date)
], Report.prototype, "updatedAt", void 0);
exports.Report = Report = __decorate([
    (0, typeorm_1.Entity)('reports')
], Report);
let Ban = class Ban {
};
exports.Ban = Ban;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], Ban.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], Ban.prototype, "userId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], Ban.prototype, "handledBy", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 50 }),
    __metadata("design:type", String)
], Ban.prototype, "reason", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'text', nullable: true }),
    __metadata("design:type", String)
], Ban.prototype, "description", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'timestamp' }),
    __metadata("design:type", Date)
], Ban.prototype, "startAt", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'timestamp', nullable: true }),
    __metadata("design:type", Date)
], Ban.prototype, "endAt", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'boolean', default: false }),
    __metadata("design:type", Boolean)
], Ban.prototype, "isPermanent", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'boolean', default: false }),
    __metadata("design:type", Boolean)
], Ban.prototype, "isActive", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], Ban.prototype, "createdAt", void 0);
__decorate([
    (0, typeorm_1.UpdateDateColumn)(),
    __metadata("design:type", Date)
], Ban.prototype, "updatedAt", void 0);
exports.Ban = Ban = __decorate([
    (0, typeorm_1.Entity)('bans')
], Ban);
let AuditLog = class AuditLog {
};
exports.AuditLog = AuditLog;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], AuditLog.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid', nullable: true }),
    __metadata("design:type", String)
], AuditLog.prototype, "userId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 100 }),
    __metadata("design:type", String)
], AuditLog.prototype, "action", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 50 }),
    __metadata("design:type", String)
], AuditLog.prototype, "resourceType", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid', nullable: true }),
    __metadata("design:type", String)
], AuditLog.prototype, "resourceId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'json', nullable: true }),
    __metadata("design:type", Object)
], AuditLog.prototype, "details", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 45, nullable: true }),
    __metadata("design:type", String)
], AuditLog.prototype, "ip", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], AuditLog.prototype, "createdAt", void 0);
exports.AuditLog = AuditLog = __decorate([
    (0, typeorm_1.Entity)('audit_logs')
], AuditLog);
let RemoteConfig = class RemoteConfig {
};
exports.RemoteConfig = RemoteConfig;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], RemoteConfig.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 100, unique: true }),
    __metadata("design:type", String)
], RemoteConfig.prototype, "key", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'json' }),
    __metadata("design:type", Object)
], RemoteConfig.prototype, "value", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'text', nullable: true }),
    __metadata("design:type", String)
], RemoteConfig.prototype, "description", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'boolean', default: true }),
    __metadata("design:type", Boolean)
], RemoteConfig.prototype, "isActive", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], RemoteConfig.prototype, "createdAt", void 0);
__decorate([
    (0, typeorm_1.UpdateDateColumn)(),
    __metadata("design:type", Date)
], RemoteConfig.prototype, "updatedAt", void 0);
exports.RemoteConfig = RemoteConfig = __decorate([
    (0, typeorm_1.Entity)('remote_configs')
], RemoteConfig);
//# sourceMappingURL=community.entity.js.map