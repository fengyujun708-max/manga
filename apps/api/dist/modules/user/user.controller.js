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
exports.UserController = void 0;
const common_1 = require("@nestjs/common");
const swagger_1 = require("@nestjs/swagger");
const user_service_1 = require("./user.service");
const auth_guard_1 = require("../../common/guards/auth.guard");
const user_dto_1 = require("./dto/user.dto");
let UserController = class UserController {
    constructor(userService) {
        this.userService = userService;
    }
    async getProfile(userId) {
        return this.userService.getProfile(userId);
    }
    async updateProfile(userId, dto) {
        return this.userService.updateProfile(userId, dto);
    }
    async updatePhone(userId, dto) {
        return this.userService.updatePhone(userId, dto.phone, dto.code);
    }
    async getDevices(userId) {
        return this.userService.getDevices(userId);
    }
    async getStats(userId) {
        return this.userService.getStats(userId);
    }
    async updateAvatar(userId, dto) {
        return this.userService.updateAvatar(userId, dto.url);
    }
};
exports.UserController = UserController;
__decorate([
    (0, common_1.Get)('profile'),
    (0, swagger_1.ApiOperation)({ summary: '获取用户信息' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], UserController.prototype, "getProfile", null);
__decorate([
    (0, common_1.Put)('profile'),
    (0, swagger_1.ApiOperation)({ summary: '修改资料' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, user_dto_1.UpdateProfileDto]),
    __metadata("design:returntype", Promise)
], UserController.prototype, "updateProfile", null);
__decorate([
    (0, common_1.Put)('phone'),
    (0, swagger_1.ApiOperation)({ summary: '修改手机号' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, user_dto_1.UpdatePhoneDto]),
    __metadata("design:returntype", Promise)
], UserController.prototype, "updatePhone", null);
__decorate([
    (0, common_1.Get)('devices'),
    (0, swagger_1.ApiOperation)({ summary: '设备列表' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], UserController.prototype, "getDevices", null);
__decorate([
    (0, common_1.Get)('stats'),
    (0, swagger_1.ApiOperation)({ summary: '阅读统计' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], UserController.prototype, "getStats", null);
__decorate([
    (0, common_1.Put)('avatar'),
    (0, swagger_1.ApiOperation)({ summary: '修改头像' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, user_dto_1.UpdateAvatarDto]),
    __metadata("design:returntype", Promise)
], UserController.prototype, "updateAvatar", null);
exports.UserController = UserController = __decorate([
    (0, swagger_1.ApiTags)('用户'),
    (0, common_1.Controller)('user'),
    (0, common_1.UseGuards)(auth_guard_1.JwtAuthGuard),
    (0, swagger_1.ApiBearerAuth)(),
    __metadata("design:paramtypes", [user_service_1.UserService])
], UserController);
//# sourceMappingURL=user.controller.js.map