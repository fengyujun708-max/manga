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
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.ComicController = void 0;
const common_1 = require("@nestjs/common");
const swagger_1 = require("@nestjs/swagger");
const comic_service_1 = require("./comic.service");
const auth_guard_1 = require("../../common/guards/auth.guard");
const comic_dto_1 = require("./dto/comic.dto");
let ComicController = class ComicController {
    constructor(comicService) {
        this.comicService = comicService;
    }
    async getHomeFeed() {
        return this.comicService.getHomeFeed();
    }
    async search(dto) {
        return this.comicService.search(dto.q, dto.page, dto.limit);
    }
    async discover(dto) {
        return this.comicService.discover(dto);
    }
    async getCategories() {
        return this.comicService.getCategories();
    }
    async getComicDetail(id) {
        return this.comicService.getComicDetail(id);
    }
    async getChapters(id, page = 1, limit = 50) {
        return this.comicService.getChapters(id, page, limit);
    }
    async getRecommendations(id) {
        return this.comicService.getRecommendations(id);
    }
    async addFavorite(userId, dto) {
        return this.comicService.addFavorite(userId, dto.comicId, dto.folderId);
    }
    async removeFavorite(userId, comicId) {
        return this.comicService.removeFavorite(userId, comicId);
    }
    async getFavorites(userId, folderId) {
        return this.comicService.getFavorites(userId, folderId);
    }
    async createFolder(userId, dto) {
        return this.comicService.createFolder(userId, dto.name);
    }
    async getFolders(userId) {
        return this.comicService.getFolders(userId);
    }
    async updateHistory(userId, dto) {
        return this.comicService.updateHistory(userId, dto);
    }
    async getHistory(userId) {
        return this.comicService.getHistory(userId);
    }
    async deleteHistory(userId, comicId) {
        return this.comicService.deleteHistory(userId, comicId);
    }
};
exports.ComicController = ComicController;
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Get)('home'),
    (0, swagger_1.ApiOperation)({ summary: '首页数据（Banner + 推荐板块）' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], ComicController.prototype, "getHomeFeed", null);
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Get)('search'),
    (0, swagger_1.ApiOperation)({ summary: '搜索漫画' }),
    __param(0, (0, common_1.Query)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [comic_dto_1.SearchDto]),
    __metadata("design:returntype", Promise)
], ComicController.prototype, "search", null);
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Get)('discover'),
    (0, swagger_1.ApiOperation)({ summary: '发现页漫画列表' }),
    __param(0, (0, common_1.Query)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [comic_dto_1.DiscoverDto]),
    __metadata("design:returntype", Promise)
], ComicController.prototype, "discover", null);
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Get)('categories'),
    (0, swagger_1.ApiOperation)({ summary: '分类列表' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], ComicController.prototype, "getCategories", null);
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Get)(':id'),
    (0, swagger_1.ApiOperation)({ summary: '漫画详情' }),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], ComicController.prototype, "getComicDetail", null);
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Get)(':id/chapters'),
    (0, swagger_1.ApiOperation)({ summary: '章节列表' }),
    __param(0, (0, common_1.Param)('id')),
    __param(1, (0, common_1.Query)('page')),
    __param(2, (0, common_1.Query)('limit')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object, Object]),
    __metadata("design:returntype", Promise)
], ComicController.prototype, "getChapters", null);
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Get)(':id/recommend'),
    (0, swagger_1.ApiOperation)({ summary: '相关推荐' }),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], ComicController.prototype, "getRecommendations", null);
__decorate([
    (0, common_1.UseGuards)(auth_guard_1.JwtAuthGuard),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.Post)('favorite'),
    (0, swagger_1.ApiOperation)({ summary: '添加收藏' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, comic_dto_1.AddFavoriteDto]),
    __metadata("design:returntype", Promise)
], ComicController.prototype, "addFavorite", null);
__decorate([
    (0, common_1.UseGuards)(auth_guard_1.JwtAuthGuard),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.Delete)('favorite/:comicId'),
    (0, swagger_1.ApiOperation)({ summary: '取消收藏' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __param(1, (0, common_1.Param)('comicId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String]),
    __metadata("design:returntype", Promise)
], ComicController.prototype, "removeFavorite", null);
__decorate([
    (0, common_1.UseGuards)(auth_guard_1.JwtAuthGuard),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.Get)('favorites/list'),
    (0, swagger_1.ApiOperation)({ summary: '收藏列表' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __param(1, (0, common_1.Query)('folderId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String]),
    __metadata("design:returntype", Promise)
], ComicController.prototype, "getFavorites", null);
__decorate([
    (0, common_1.UseGuards)(auth_guard_1.JwtAuthGuard),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.Post)('favorites/folders'),
    (0, swagger_1.ApiOperation)({ summary: '创建收藏夹' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, comic_dto_1.CreateFolderDto]),
    __metadata("design:returntype", Promise)
], ComicController.prototype, "createFolder", null);
__decorate([
    (0, common_1.UseGuards)(auth_guard_1.JwtAuthGuard),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.Get)('favorites/folders'),
    (0, swagger_1.ApiOperation)({ summary: '收藏夹列表' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], ComicController.prototype, "getFolders", null);
__decorate([
    (0, common_1.UseGuards)(auth_guard_1.JwtAuthGuard),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.Post)('history'),
    (0, swagger_1.ApiOperation)({ summary: '更新阅读进度' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, comic_dto_1.UpdateHistoryDto]),
    __metadata("design:returntype", Promise)
], ComicController.prototype, "updateHistory", null);
__decorate([
    (0, common_1.UseGuards)(auth_guard_1.JwtAuthGuard),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.Get)('history/list'),
    (0, swagger_1.ApiOperation)({ summary: '阅读历史列表' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], ComicController.prototype, "getHistory", null);
__decorate([
    (0, common_1.UseGuards)(auth_guard_1.JwtAuthGuard),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.Delete)('history/:comicId'),
    (0, swagger_1.ApiOperation)({ summary: '删除阅读记录' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __param(1, (0, common_1.Param)('comicId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String]),
    __metadata("design:returntype", Promise)
], ComicController.prototype, "deleteHistory", null);
exports.ComicController = ComicController = __decorate([
    (0, swagger_1.ApiTags)('漫画'),
    (0, common_1.Controller)('comic'),
    __metadata("design:paramtypes", [comic_service_1.ComicService])
], ComicController);
//# sourceMappingURL=comic.controller.js.map