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
exports.AdminService = void 0;
const common_1 = require("@nestjs/common");
const typeorm_1 = require("@nestjs/typeorm");
const typeorm_2 = require("typeorm");
const user_entity_1 = require("../user/entities/user.entity");
const community_entity_1 = require("../community/entities/community.entity");
let AdminService = class AdminService {
    constructor(userRepo, reportRepo, banRepo, auditRepo) {
        this.userRepo = userRepo;
        this.reportRepo = reportRepo;
        this.banRepo = banRepo;
        this.auditRepo = auditRepo;
    }
    async getDashboard() {
        const totalUsers = await this.userRepo.count();
        const activeUsers = await this.userRepo.count({ where: { status: 'active' } });
        const pendingReports = await this.reportRepo.count({ where: { status: 'pending' } });
        return { totalUsers, activeUsers, pendingReports };
    }
    async getUsers(page = 1, limit = 20) {
        const [items, total] = await this.userRepo.findAndCount({ skip: (page - 1) * limit, take: limit, order: { createdAt: 'DESC' } });
        return { items, total, page, limit, totalPages: Math.ceil(total / limit) };
    }
    async banUser(adminId, userId, reason) {
        await this.userRepo.update(userId, { status: 'suspended' });
        await this.banRepo.save({ userId, handledBy: adminId, reason, startAt: new Date(), isActive: true });
        await this.auditRepo.save({ userId: adminId, action: 'ban_user', resourceType: 'user', resourceId: userId });
        return { message: '已封禁' };
    }
    async getReports() { return this.reportRepo.find({ where: { status: 'pending' }, order: { createdAt: 'DESC' } }); }
    async resolveReport(id) {
        await this.reportRepo.update(id, { status: 'resolved' });
        return { message: '已处理' };
    }
};
exports.AdminService = AdminService;
exports.AdminService = AdminService = __decorate([
    (0, common_1.Injectable)(),
    __param(0, (0, typeorm_1.InjectRepository)(user_entity_1.User)),
    __param(1, (0, typeorm_1.InjectRepository)(community_entity_1.Report)),
    __param(2, (0, typeorm_1.InjectRepository)(community_entity_1.Ban)),
    __param(3, (0, typeorm_1.InjectRepository)(community_entity_1.AuditLog)),
    __metadata("design:paramtypes", [typeof (_a = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _a : Object, typeof (_b = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _b : Object, typeof (_c = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _c : Object, typeof (_d = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _d : Object])
], AdminService);
//# sourceMappingURL=admin.service.js.map