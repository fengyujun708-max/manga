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
exports.UpdateHistoryDto = exports.CreateFolderDto = exports.AddFavoriteDto = exports.DiscoverDto = exports.SearchDto = exports.PaginationDto = void 0;
const class_validator_1 = require("class-validator");
const swagger_1 = require("@nestjs/swagger");
const class_transformer_1 = require("class-transformer");
class PaginationDto {
    constructor() {
        this.page = 1;
        this.limit = 20;
    }
}
exports.PaginationDto = PaginationDto;
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_transformer_1.Type)(() => Number),
    (0, class_validator_1.IsInt)(),
    (0, class_validator_1.Min)(1),
    __metadata("design:type", Number)
], PaginationDto.prototype, "page", void 0);
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_transformer_1.Type)(() => Number),
    (0, class_validator_1.IsInt)(),
    (0, class_validator_1.Min)(1),
    (0, class_validator_1.Max)(100),
    __metadata("design:type", Number)
], PaginationDto.prototype, "limit", void 0);
class SearchDto extends PaginationDto {
}
exports.SearchDto = SearchDto;
__decorate([
    (0, swagger_1.ApiProperty)({ description: '搜索关键词' }),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], SearchDto.prototype, "q", void 0);
class DiscoverDto extends PaginationDto {
    constructor() {
        super(...arguments);
        this.sort = 'latest';
    }
}
exports.DiscoverDto = DiscoverDto;
__decorate([
    (0, swagger_1.ApiProperty)({ description: '分类', required: false }),
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], DiscoverDto.prototype, "category", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ description: '排序: latest/popular/rating', required: false }),
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], DiscoverDto.prototype, "sort", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ description: '标签', required: false }),
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], DiscoverDto.prototype, "tag", void 0);
class AddFavoriteDto {
}
exports.AddFavoriteDto = AddFavoriteDto;
__decorate([
    (0, swagger_1.ApiProperty)({ description: '漫画 ID' }),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], AddFavoriteDto.prototype, "comicId", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ description: '收藏夹 ID', required: false }),
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], AddFavoriteDto.prototype, "folderId", void 0);
class CreateFolderDto {
}
exports.CreateFolderDto = CreateFolderDto;
__decorate([
    (0, swagger_1.ApiProperty)({ description: '收藏夹名称' }),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], CreateFolderDto.prototype, "name", void 0);
class UpdateHistoryDto {
}
exports.UpdateHistoryDto = UpdateHistoryDto;
__decorate([
    (0, swagger_1.ApiProperty)({ description: '漫画 ID' }),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], UpdateHistoryDto.prototype, "comicId", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ description: '章节 ID' }),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], UpdateHistoryDto.prototype, "chapterId", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ description: '页码' }),
    (0, class_transformer_1.Type)(() => Number),
    (0, class_validator_1.IsInt)(),
    (0, class_validator_1.Min)(0),
    __metadata("design:type", Number)
], UpdateHistoryDto.prototype, "page", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ description: '阅读进度 0-1' }),
    (0, class_transformer_1.Type)(() => Number),
    __metadata("design:type", Number)
], UpdateHistoryDto.prototype, "progress", void 0);
//# sourceMappingURL=comic.dto.js.map