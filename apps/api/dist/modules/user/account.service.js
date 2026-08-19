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
var _a, _b, _c;
Object.defineProperty(exports, "__esModule", { value: true });
exports.AccountService = void 0;
const common_1 = require("@nestjs/common");
const typeorm_1 = require("@nestjs/typeorm");
const typeorm_2 = require("typeorm");
const user_entity_1 = require("./entities/user.entity");
const user_entity_2 = require("./entities/user.entity");
let AccountService = class AccountService {
    constructor(userRepo, sessionRepo, deviceRepo) {
        this.userRepo = userRepo;
        this.sessionRepo = sessionRepo;
        this.deviceRepo = deviceRepo;
    }
    async deleteAccount(userId) {
        await this.userRepo.update(userId, { status: 'deleted', phone: `deleted_${userId}`, nickname: '已注销用户' });
        await this.sessionRepo.update({ userId }, { isRevoked: true });
        await this.deviceRepo.delete({ userId });
        return { message: '账号已注销' };
    }
    async getDevices(userId) {
        return this.deviceRepo.find({ where: { userId }, order: { lastActiveAt: 'DESC' } });
    }
    async logoutOtherDevices(userId) {
        await this.sessionRepo.update({ userId }, { isRevoked: true });
        return { message: '已退出其他设备' };
    }
};
exports.AccountService = AccountService;
exports.AccountService = AccountService = __decorate([
    (0, common_1.Injectable)(),
    __param(0, (0, typeorm_1.InjectRepository)(user_entity_1.User)),
    __param(1, (0, typeorm_1.InjectRepository)(user_entity_2.UserSession)),
    __param(2, (0, typeorm_1.InjectRepository)(user_entity_2.UserDevice)),
    __metadata("design:paramtypes", [typeof (_a = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _a : Object, typeof (_b = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _b : Object, typeof (_c = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _c : Object])
], AccountService);
//# sourceMappingURL=account.service.js.map