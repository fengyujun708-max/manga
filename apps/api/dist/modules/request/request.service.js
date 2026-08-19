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
var _a, _b;
Object.defineProperty(exports, "__esModule", { value: true });
exports.RequestService = void 0;
const common_1 = require("@nestjs/common");
const typeorm_1 = require("@nestjs/typeorm");
const typeorm_2 = require("typeorm");
const community_entity_1 = require("../community/entities/community.entity");
let RequestService = class RequestService {
    constructor(mangaRepo, sourceRepo) {
        this.mangaRepo = mangaRepo;
        this.sourceRepo = sourceRepo;
    }
    async createMangaRequest(userId, dto) {
        return this.mangaRepo.save({ userId, ...dto });
    }
    async getMangaRequests(page = 1, limit = 20, status) {
        const where = {};
        if (status)
            where.status = status;
        const [items, total] = await this.mangaRepo.findAndCount({
            where, skip: (page - 1) * limit, take: limit,
            order: { createdAt: 'DESC' },
        });
        return { items, total, page, limit, totalPages: Math.ceil(total / limit) };
    }
    async getMangaRequest(id) {
        const req = await this.mangaRepo.findOneBy({ id });
        if (!req)
            throw new common_1.NotFoundException('求漫不存在');
        return req;
    }
    async createSourceRequest(userId, dto) {
        return this.sourceRepo.save({ userId, ...dto });
    }
    async getSourceRequests(page = 1, limit = 20) {
        const [items, total] = await this.sourceRepo.findAndCount({
            skip: (page - 1) * limit, take: limit,
            order: { createdAt: 'DESC' },
        });
        return { items, total, page, limit, totalPages: Math.ceil(total / limit) };
    }
};
exports.RequestService = RequestService;
exports.RequestService = RequestService = __decorate([
    (0, common_1.Injectable)(),
    __param(0, (0, typeorm_1.InjectRepository)(community_entity_1.MangaRequest)),
    __param(1, (0, typeorm_1.InjectRepository)(community_entity_1.SourceRequest)),
    __metadata("design:paramtypes", [typeof (_a = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _a : Object, typeof (_b = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _b : Object])
], RequestService);
//# sourceMappingURL=request.service.js.map