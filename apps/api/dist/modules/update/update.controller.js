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
exports.UpdateController = void 0;
const common_1 = require("@nestjs/common");
const swagger_1 = require("@nestjs/swagger");
const update_service_1 = require("./update.service");
const auth_guard_1 = require("../../common/guards/auth.guard");
const class_validator_1 = require("class-validator");
class RegisterDeviceDto {
}
__decorate([
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], RegisterDeviceDto.prototype, "deviceId", void 0);
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], RegisterDeviceDto.prototype, "deviceName", void 0);
__decorate([
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], RegisterDeviceDto.prototype, "pushToken", void 0);
let UpdateController = class UpdateController {
    constructor(updateService) {
        this.updateService = updateService;
    }
    async checkUpdate(version, platform) {
        return this.updateService.checkUpdate(version, platform);
    }
    async getRemoteConfig() { return this.updateService.getRemoteConfig(); }
    async getAnnouncements() { return this.updateService.getActiveAnnouncements(); }
    async registerDevice(userId, dto) {
        return { message: '设备已注册' };
    }
};
exports.UpdateController = UpdateController;
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Get)('check-update'),
    (0, swagger_1.ApiOperation)({ summary: '检查更新' }),
    __param(0, (0, common_1.Query)('version')),
    __param(1, (0, common_1.Query)('platform')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String]),
    __metadata("design:returntype", Promise)
], UpdateController.prototype, "checkUpdate", null);
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Get)('config'),
    (0, swagger_1.ApiOperation)({ summary: '获取远程配置' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], UpdateController.prototype, "getRemoteConfig", null);
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Get)('announcements'),
    (0, swagger_1.ApiOperation)({ summary: '获取公告' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], UpdateController.prototype, "getAnnouncements", null);
__decorate([
    (0, common_1.UseGuards)(auth_guard_1.JwtAuthGuard),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.Post)('register-device'),
    (0, swagger_1.ApiOperation)({ summary: '注册设备推送' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, RegisterDeviceDto]),
    __metadata("design:returntype", Promise)
], UpdateController.prototype, "registerDevice", null);
exports.UpdateController = UpdateController = __decorate([
    (0, swagger_1.ApiTags)('应用更新'),
    (0, common_1.Controller)('app'),
    __metadata("design:paramtypes", [update_service_1.UpdateService])
], UpdateController);
//# sourceMappingURL=update.controller.js.map