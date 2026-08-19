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
exports.FavoriteFolder = exports.Favorite = exports.ReadingHistory = exports.Chapter = exports.ComicSource = exports.Comic = void 0;
const typeorm_1 = require("typeorm");
let Comic = class Comic {
};
exports.Comic = Comic;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], Comic.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 255 }),
    __metadata("design:type", String)
], Comic.prototype, "title", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 255, nullable: true }),
    __metadata("design:type", String)
], Comic.prototype, "altTitle", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'text', nullable: true }),
    __metadata("design:type", String)
], Comic.prototype, "description", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 500, nullable: true }),
    __metadata("design:type", String)
], Comic.prototype, "coverUrl", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 255, nullable: true }),
    __metadata("design:type", String)
], Comic.prototype, "author", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 255, nullable: true }),
    __metadata("design:type", String)
], Comic.prototype, "artist", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'simple-array', nullable: true }),
    __metadata("design:type", Array)
], Comic.prototype, "tags", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 50, nullable: true }),
    __metadata("design:type", String)
], Comic.prototype, "status", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'int', default: 0 }),
    __metadata("design:type", Number)
], Comic.prototype, "chapterCount", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'float', default: 0 }),
    __metadata("design:type", Number)
], Comic.prototype, "rating", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'int', default: 0 }),
    __metadata("design:type", Number)
], Comic.prototype, "views", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'int', default: 0 }),
    __metadata("design:type", Number)
], Comic.prototype, "favoritesCount", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], Comic.prototype, "createdAt", void 0);
__decorate([
    (0, typeorm_1.UpdateDateColumn)(),
    __metadata("design:type", Date)
], Comic.prototype, "updatedAt", void 0);
exports.Comic = Comic = __decorate([
    (0, typeorm_1.Entity)('comics')
], Comic);
let ComicSource = class ComicSource {
};
exports.ComicSource = ComicSource;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], ComicSource.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], ComicSource.prototype, "comicId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 100 }),
    __metadata("design:type", String)
], ComicSource.prototype, "sourceId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 500 }),
    __metadata("design:type", String)
], ComicSource.prototype, "sourceUrl", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 255, nullable: true }),
    __metadata("design:type", String)
], ComicSource.prototype, "sourceComicId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'boolean', default: true }),
    __metadata("design:type", Boolean)
], ComicSource.prototype, "isActive", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], ComicSource.prototype, "createdAt", void 0);
__decorate([
    (0, typeorm_1.UpdateDateColumn)(),
    __metadata("design:type", Date)
], ComicSource.prototype, "updatedAt", void 0);
exports.ComicSource = ComicSource = __decorate([
    (0, typeorm_1.Entity)('comic_sources'),
    (0, typeorm_1.Index)(['comicId', 'sourceId'], { unique: true })
], ComicSource);
let Chapter = class Chapter {
};
exports.Chapter = Chapter;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], Chapter.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], Chapter.prototype, "comicId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 100 }),
    __metadata("design:type", String)
], Chapter.prototype, "sourceId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 255 }),
    __metadata("design:type", String)
], Chapter.prototype, "title", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'float', nullable: true }),
    __metadata("design:type", Number)
], Chapter.prototype, "chapterNumber", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'int', nullable: true }),
    __metadata("design:type", Number)
], Chapter.prototype, "pageCount", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 500, nullable: true }),
    __metadata("design:type", String)
], Chapter.prototype, "sourceUrl", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'boolean', default: false }),
    __metadata("design:type", Boolean)
], Chapter.prototype, "isDownloaded", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'timestamp', nullable: true }),
    __metadata("design:type", Date)
], Chapter.prototype, "sourceUpdatedAt", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], Chapter.prototype, "createdAt", void 0);
__decorate([
    (0, typeorm_1.UpdateDateColumn)(),
    __metadata("design:type", Date)
], Chapter.prototype, "updatedAt", void 0);
exports.Chapter = Chapter = __decorate([
    (0, typeorm_1.Entity)('chapters')
], Chapter);
let ReadingHistory = class ReadingHistory {
};
exports.ReadingHistory = ReadingHistory;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], ReadingHistory.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], ReadingHistory.prototype, "userId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], ReadingHistory.prototype, "comicId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid', nullable: true }),
    __metadata("design:type", String)
], ReadingHistory.prototype, "chapterId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'int', default: 0 }),
    __metadata("design:type", Number)
], ReadingHistory.prototype, "page", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'float', default: 0 }),
    __metadata("design:type", Number)
], ReadingHistory.prototype, "progress", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'timestamp' }),
    __metadata("design:type", Date)
], ReadingHistory.prototype, "lastReadAt", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'int', default: 0 }),
    __metadata("design:type", Number)
], ReadingHistory.prototype, "totalReadTime", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], ReadingHistory.prototype, "createdAt", void 0);
__decorate([
    (0, typeorm_1.UpdateDateColumn)(),
    __metadata("design:type", Date)
], ReadingHistory.prototype, "updatedAt", void 0);
exports.ReadingHistory = ReadingHistory = __decorate([
    (0, typeorm_1.Entity)('reading_history'),
    (0, typeorm_1.Index)(['userId', 'comicId'], { unique: true })
], ReadingHistory);
let Favorite = class Favorite {
};
exports.Favorite = Favorite;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], Favorite.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], Favorite.prototype, "userId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], Favorite.prototype, "comicId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid', nullable: true }),
    __metadata("design:type", String)
], Favorite.prototype, "folderId", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], Favorite.prototype, "createdAt", void 0);
exports.Favorite = Favorite = __decorate([
    (0, typeorm_1.Entity)('favorites'),
    (0, typeorm_1.Index)(['userId', 'comicId', 'folderId'], { unique: true })
], Favorite);
let FavoriteFolder = class FavoriteFolder {
};
exports.FavoriteFolder = FavoriteFolder;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], FavoriteFolder.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], FavoriteFolder.prototype, "userId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 100 }),
    __metadata("design:type", String)
], FavoriteFolder.prototype, "name", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'int', default: 0 }),
    __metadata("design:type", Number)
], FavoriteFolder.prototype, "sortOrder", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], FavoriteFolder.prototype, "createdAt", void 0);
__decorate([
    (0, typeorm_1.UpdateDateColumn)(),
    __metadata("design:type", Date)
], FavoriteFolder.prototype, "updatedAt", void 0);
exports.FavoriteFolder = FavoriteFolder = __decorate([
    (0, typeorm_1.Entity)('favorite_folders')
], FavoriteFolder);
//# sourceMappingURL=comic.entity.js.map