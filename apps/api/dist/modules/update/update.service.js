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
exports.UpdateService = void 0;
const common_1 = require("@nestjs/common");
const typeorm_1 = require("@nestjs/typeorm");
const typeorm_2 = require("typeorm");
const community_entity_1 = require("../community/entities/community.entity");
let UpdateService = class UpdateService {
    constructor(versionRepo, configRepo, announcementRepo) {
        this.versionRepo = versionRepo;
        this.configRepo = configRepo;
        this.announcementRepo = announcementRepo;
    }
    async checkUpdate(version, platform) {
        const currentCode = this.parseVersion(version);
        const latest = await this.versionRepo.findOne({
            where: { platform, isActive: true },
            order: { buildNumber: 'DESC' },
        });
        if (!latest)
            return { hasUpdate: false };
        const hasUpdate = latest.buildNumber > currentCode;
        const isForceUpdate = latest.isForceUpdate && latest.minVersion != null
            && currentCode <= this.parseVersion(latest.minVersion);
        return {
            hasUpdate, isForceUpdate,
            latestVersion: { version: latest.version, buildNumber: latest.buildNumber, platform: latest.platform },
            updateUrl: latest.downloadUrl, changelog: latest.changelog,
            message: isForceUpdate ? '当前版本已停止支持，请更新后继续使用' : null,
        };
    }
    async getRemoteConfig() {
        const configs = await this.configRepo.find({ where: { isActive: true } });
        const result = {};
        for (const config of configs)
            result[config.key] = config.value;
        result['registration_enabled'] ??= true;
        result['community_enabled'] ??= true;
        result['maintenance_mode'] ??= false;
        return result;
    }
    async getActiveAnnouncements() {
        const now = new Date();
        return this.announcementRepo.find({
            where: { isActive: true, startAt: (0, typeorm_2.LessThanOrEqual)(now), endAt: (0, typeorm_2.MoreThanOrEqual)(now) },
            order: { priority: 'DESC', createdAt: 'DESC' },
        });
    }
    parseVersion(version) {
        try {
            const parts = version.split('.').map(p => parseInt(p, 10));
            return parts[0] * 10000 + (parts[1] || 0) * 100 + (parts[2] || 0);
        }
        catch {
            return 0;
        }
    }
};
exports.UpdateService = UpdateService;
exports.UpdateService = UpdateService = __decorate([
    (0, common_1.Injectable)(),
    __param(0, (0, typeorm_1.InjectRepository)(community_entity_1.AppVersion)),
    __param(1, (0, typeorm_1.InjectRepository)(community_entity_1.RemoteConfig)),
    __param(2, (0, typeorm_1.InjectRepository)(community_entity_1.Announcement)),
    __metadata("design:paramtypes", [typeof (_a = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _a : Object, typeof (_b = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _b : Object, typeof (_c = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _c : Object])
], UpdateService);
//# sourceMappingURL=update.service.js.map