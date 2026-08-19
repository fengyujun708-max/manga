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
exports.AuthController = void 0;
const common_1 = require("@nestjs/common");
const swagger_1 = require("@nestjs/swagger");
const auth_service_1 = require("./auth.service");
const auth_guard_1 = require("../../common/guards/auth.guard");
const auth_guard_2 = require("../../common/guards/auth.guard");
const auth_dto_1 = require("./dto/auth.dto");
let AuthController = class AuthController {
    constructor(authService) {
        this.authService = authService;
    }
    async sendCode(dto) {
        await this.authService.sendCode(dto.phone);
        return { message: '验证码已发送' };
    }
    async register(dto, ip) {
        return this.authService.register(dto.phone, dto.code, dto.password, dto.nickname);
    }
    async login(dto, ip) {
        return this.authService.login(dto.phone, dto.password, ip);
    }
    async smsLogin(dto, ip) {
        return this.authService.smsLogin(dto.phone, dto.code, ip);
    }
    async refresh(dto) {
        return this.authService.refreshTokens(dto.refreshToken);
    }
    async logout(userId) {
        await this.authService.logout(userId);
        return { message: '已退出登录' };
    }
    async logoutAll(userId) {
        await this.authService.logoutAllDevices(userId);
        return { message: '已退出所有设备' };
    }
    async resetPassword(dto) {
        await this.authService.resetPassword(dto.phone, dto.code, dto.newPassword);
        return { message: '密码已重置' };
    }
    async changePassword(userId, dto) {
        await this.authService.changePassword(userId, dto.oldPassword, dto.newPassword);
        return { message: '密码已修改' };
    }
};
exports.AuthController = AuthController;
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Post)('send-code'),
    (0, swagger_1.ApiOperation)({ summary: '发送验证码' }),
    (0, common_1.HttpCode)(common_1.HttpStatus.OK),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [auth_dto_1.SendCodeDto]),
    __metadata("design:returntype", Promise)
], AuthController.prototype, "sendCode", null);
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Post)('register'),
    (0, swagger_1.ApiOperation)({ summary: '注册' }),
    (0, common_1.HttpCode)(common_1.HttpStatus.CREATED),
    __param(0, (0, common_1.Body)()),
    __param(1, (0, common_1.Ip)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [auth_dto_1.RegisterDto, String]),
    __metadata("design:returntype", Promise)
], AuthController.prototype, "register", null);
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Post)('login'),
    (0, swagger_1.ApiOperation)({ summary: '密码登录' }),
    (0, common_1.HttpCode)(common_1.HttpStatus.OK),
    __param(0, (0, common_1.Body)()),
    __param(1, (0, common_1.Ip)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [auth_dto_1.LoginDto, String]),
    __metadata("design:returntype", Promise)
], AuthController.prototype, "login", null);
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Post)('sms-login'),
    (0, swagger_1.ApiOperation)({ summary: '验证码登录' }),
    (0, common_1.HttpCode)(common_1.HttpStatus.OK),
    __param(0, (0, common_1.Body)()),
    __param(1, (0, common_1.Ip)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [auth_dto_1.SmsLoginDto, String]),
    __metadata("design:returntype", Promise)
], AuthController.prototype, "smsLogin", null);
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Post)('refresh'),
    (0, swagger_1.ApiOperation)({ summary: '刷新 Token' }),
    (0, common_1.HttpCode)(common_1.HttpStatus.OK),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [auth_dto_1.RefreshTokenDto]),
    __metadata("design:returntype", Promise)
], AuthController.prototype, "refresh", null);
__decorate([
    (0, common_1.UseGuards)(auth_guard_2.JwtAuthGuard),
    (0, common_1.Post)('logout'),
    (0, swagger_1.ApiBearerAuth)(),
    (0, swagger_1.ApiOperation)({ summary: '退出登录' }),
    (0, common_1.HttpCode)(common_1.HttpStatus.OK),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], AuthController.prototype, "logout", null);
__decorate([
    (0, common_1.UseGuards)(auth_guard_2.JwtAuthGuard),
    (0, common_1.Post)('logout-all'),
    (0, swagger_1.ApiBearerAuth)(),
    (0, swagger_1.ApiOperation)({ summary: '退出所有设备' }),
    (0, common_1.HttpCode)(common_1.HttpStatus.OK),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], AuthController.prototype, "logoutAll", null);
__decorate([
    (0, auth_guard_1.Public)(),
    (0, common_1.Post)('reset-password'),
    (0, swagger_1.ApiOperation)({ summary: '重置密码' }),
    (0, common_1.HttpCode)(common_1.HttpStatus.OK),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [auth_dto_1.ResetPasswordDto]),
    __metadata("design:returntype", Promise)
], AuthController.prototype, "resetPassword", null);
__decorate([
    (0, common_1.UseGuards)(auth_guard_2.JwtAuthGuard),
    (0, common_1.Post)('change-password'),
    (0, swagger_1.ApiBearerAuth)(),
    (0, swagger_1.ApiOperation)({ summary: '修改密码' }),
    (0, common_1.HttpCode)(common_1.HttpStatus.OK),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, auth_dto_1.ChangePasswordDto]),
    __metadata("design:returntype", Promise)
], AuthController.prototype, "changePassword", null);
exports.AuthController = AuthController = __decorate([
    (0, swagger_1.ApiTags)('认证'),
    (0, common_1.Controller)('auth'),
    __metadata("design:paramtypes", [auth_service_1.AuthService])
], AuthController);
//# sourceMappingURL=auth.controller.js.map