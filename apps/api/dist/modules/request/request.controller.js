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
exports.RequestController = void 0;
const common_1 = require("@nestjs/common");
const swagger_1 = require("@nestjs/swagger");
const request_service_1 = require("./request.service");
const auth_guard_1 = require("../../common/guards/auth.guard");
const class_validator_1 = require("class-validator");
class CreateMangaRequestDto {
}
__decorate([
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], CreateMangaRequestDto.prototype, "mangaName", void 0);
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], CreateMangaRequestDto.prototype, "altName", void 0);
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], CreateMangaRequestDto.prototype, "author", void 0);
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], CreateMangaRequestDto.prototype, "description", void 0);
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], CreateMangaRequestDto.prototype, "notes", void 0);
class CreateSourceRequestDto {
}
__decorate([
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], CreateSourceRequestDto.prototype, "sourceName", void 0);
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], CreateSourceRequestDto.prototype, "sourceUrl", void 0);
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], CreateSourceRequestDto.prototype, "description", void 0);
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], CreateSourceRequestDto.prototype, "notes", void 0);
let RequestController = class RequestController {
    constructor(requestService) {
        this.requestService = requestService;
    }
    async createMangaRequest(userId, dto) {
        return this.requestService.createMangaRequest(userId, dto);
    }
    async getMangaRequests(page = 1, limit = 20, status) {
        return this.requestService.getMangaRequests(page, limit, status);
    }
    async getMangaRequest(id) {
        return this.requestService.getMangaRequest(id);
    }
    async createSourceRequest(userId, dto) {
        return this.requestService.createSourceRequest(userId, dto);
    }
    async getSourceRequests(page = 1, limit = 20) {
        return this.requestService.getSourceRequests(page, limit);
    }
};
exports.RequestController = RequestController;
__decorate([
    (0, common_1.UseGuards)(auth_guard_1.JwtAuthGuard),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.Post)('manga'),
    (0, swagger_1.ApiOperation)({ summary: '发布求漫' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, CreateMangaRequestDto]),
    __metadata("design:returntype", Promise)
], RequestController.prototype, "createMangaRequest", null);
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Get)('manga'),
    (0, swagger_1.ApiOperation)({ summary: '求漫列表' }),
    __param(0, (0, common_1.Query)('page')),
    __param(1, (0, common_1.Query)('limit')),
    __param(2, (0, common_1.Query)('status')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, Object, String]),
    __metadata("design:returntype", Promise)
], RequestController.prototype, "getMangaRequests", null);
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Get)('manga/:id'),
    (0, swagger_1.ApiOperation)({ summary: '求漫详情' }),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], RequestController.prototype, "getMangaRequest", null);
__decorate([
    (0, common_1.UseGuards)(auth_guard_1.JwtAuthGuard),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.Post)('source'),
    (0, swagger_1.ApiOperation)({ summary: '发布求源' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, CreateSourceRequestDto]),
    __metadata("design:returntype", Promise)
], RequestController.prototype, "createSourceRequest", null);
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Get)('source'),
    (0, swagger_1.ApiOperation)({ summary: '求源列表' }),
    __param(0, (0, common_1.Query)('page')),
    __param(1, (0, common_1.Query)('limit')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, Object]),
    __metadata("design:returntype", Promise)
], RequestController.prototype, "getSourceRequests", null);
exports.RequestController = RequestController = __decorate([
    (0, swagger_1.ApiTags)('求漫/求源'),
    (0, common_1.Controller)('request'),
    __metadata("design:paramtypes", [request_service_1.RequestService])
], RequestController);
//# sourceMappingURL=request.controller.js.map