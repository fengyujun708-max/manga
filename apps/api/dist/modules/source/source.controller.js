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
exports.SourceController = void 0;
const common_1 = require("@nestjs/common");
const swagger_1 = require("@nestjs/swagger");
const source_service_1 = require("./source.service");
const auth_guard_1 = require("../../common/guards/auth.guard");
const auth_guard_2 = require("../../common/guards/auth.guard");
let SourceController = class SourceController {
    constructor(sourceService) {
        this.sourceService = sourceService;
    }
    async getRegistry() { return this.sourceService.getRegistry(); }
    async getSource(id) { return this.sourceService.getSource(id); }
    async getDownloadUrl(id) { return this.sourceService.getDownloadUrl(id); }
    async registerSource(dto) { return this.sourceService.registerSource(dto); }
};
exports.SourceController = SourceController;
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Get)(),
    (0, swagger_1.ApiOperation)({ summary: '获取源注册表' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SourceController.prototype, "getRegistry", null);
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Get)(':id'),
    (0, swagger_1.ApiOperation)({ summary: '获取源详情' }),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], SourceController.prototype, "getSource", null);
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Get)(':id/download'),
    (0, swagger_1.ApiOperation)({ summary: '源 JS 文件下载地址' }),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], SourceController.prototype, "getDownloadUrl", null);
__decorate([
    (0, common_1.UseGuards)(auth_guard_1.JwtAuthGuard, auth_guard_2.RolesGuard),
    (0, auth_guard_2.Roles)('super_admin', 'source_manager'),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.Post)(),
    (0, swagger_1.ApiOperation)({ summary: '注册源' }),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], SourceController.prototype, "registerSource", null);
exports.SourceController = SourceController = __decorate([
    (0, swagger_1.ApiTags)('源注册表'),
    (0, common_1.Controller)('sources'),
    __metadata("design:paramtypes", [source_service_1.SourceService])
], SourceController);
//# sourceMappingURL=source.controller.js.map