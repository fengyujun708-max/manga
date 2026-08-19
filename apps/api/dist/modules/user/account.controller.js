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
exports.AccountController = void 0;
const common_1 = require("@nestjs/common");
const swagger_1 = require("@nestjs/swagger");
const account_service_1 = require("./account.service");
const auth_guard_1 = require("../../common/guards/auth.guard");
let AccountController = class AccountController {
    constructor(accountService) {
        this.accountService = accountService;
    }
    async deleteAccount(userId) { return this.accountService.deleteAccount(userId); }
    async getDevices(userId) { return this.accountService.getDevices(userId); }
    async logoutOtherDevices(userId) { return this.accountService.logoutOtherDevices(userId); }
};
exports.AccountController = AccountController;
__decorate([
    (0, common_1.Delete)(),
    (0, swagger_1.ApiOperation)({ summary: '注销账号' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], AccountController.prototype, "deleteAccount", null);
__decorate([
    (0, common_1.Get)('devices'),
    (0, swagger_1.ApiOperation)({ summary: '设备列表' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], AccountController.prototype, "getDevices", null);
__decorate([
    (0, common_1.Post)('devices/logout'),
    (0, swagger_1.ApiOperation)({ summary: '退出其他设备' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], AccountController.prototype, "logoutOtherDevices", null);
exports.AccountController = AccountController = __decorate([
    (0, swagger_1.ApiTags)('账号'),
    (0, common_1.Controller)('account'),
    (0, common_1.UseGuards)(auth_guard_1.JwtAuthGuard),
    (0, swagger_1.ApiBearerAuth)(),
    __metadata("design:paramtypes", [account_service_1.AccountService])
], AccountController);
//# sourceMappingURL=account.controller.js.map