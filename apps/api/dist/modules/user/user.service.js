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
var _a, _b, _c, _d;
Object.defineProperty(exports, "__esModule", { value: true });
exports.UserService = void 0;
const common_1 = require("@nestjs/common");
const typeorm_1 = require("@nestjs/typeorm");
const typeorm_2 = require("typeorm");
const user_entity_1 = require("./entities/user.entity");
const user_entity_2 = require("./entities/user.entity");
const comic_entity_1 = require("../comic/entities/comic.entity");
let UserService = class UserService {
    constructor(userRepo, deviceRepo, historyRepo, favoriteRepo) {
        this.userRepo = userRepo;
        this.deviceRepo = deviceRepo;
        this.historyRepo = historyRepo;
        this.favoriteRepo = favoriteRepo;
    }
    async getProfile(userId) {
        const user = await this.userRepo.findOneBy({ id: userId });
        if (!user)
            throw new common_1.NotFoundException('用户不存在');
        return user;
    }
    async updateProfile(userId, dto) {
        await this.userRepo.update(userId, { nickname: dto.nickname });
        return this.getProfile(userId);
    }
    async updatePhone(userId, phone, code) {
        const existing = await this.userRepo.findOneBy({ phone });
        if (existing)
            throw new common_1.BadRequestException('手机号已被使用');
        await this.userRepo.update(userId, { phone, phoneVerified: true });
        return this.getProfile(userId);
    }
    async updateAvatar(userId, url) {
        await this.userRepo.update(userId, { avatar: url });
        return this.getProfile(userId);
    }
    async getDevices(userId) {
        return this.deviceRepo.find({
            where: { userId },
            order: { lastActiveAt: 'DESC' },
        });
    }
    async getStats(userId) {
        const history = await this.historyRepo.find({ where: { userId } });
        const totalReadTime = history.reduce((sum, h) => sum + h.totalReadTime, 0);
        const favorites = await this.favoriteRepo.count({ where: { userId } });
        return {
            totalReadTime,
            totalRead: history.length,
            totalFavorites: favorites,
        };
    }
};
exports.UserService = UserService;
exports.UserService = UserService = __decorate([
    (0, common_1.Injectable)(),
    __param(0, (0, typeorm_1.InjectRepository)(user_entity_1.User)),
    __param(1, (0, typeorm_1.InjectRepository)(user_entity_2.UserDevice)),
    __param(2, (0, typeorm_1.InjectRepository)(comic_entity_1.ReadingHistory)),
    __param(3, (0, typeorm_1.InjectRepository)(comic_entity_1.Favorite)),
    __metadata("design:paramtypes", [typeof (_a = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _a : Object, typeof (_b = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _b : Object, typeof (_c = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _c : Object, typeof (_d = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _d : Object])
], UserService);
//# sourceMappingURL=user.service.js.map