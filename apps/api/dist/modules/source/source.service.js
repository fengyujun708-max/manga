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
var _a;
Object.defineProperty(exports, "__esModule", { value: true });
exports.SourceService = void 0;
const common_1 = require("@nestjs/common");
const typeorm_1 = require("@nestjs/typeorm");
const typeorm_2 = require("typeorm");
const community_entity_1 = require("../community/entities/community.entity");
let SourceService = class SourceService {
    constructor(registryRepo) {
        this.registryRepo = registryRepo;
    }
    async getRegistry() {
        const sources = await this.registryRepo.find({ where: { status: 'active' }, order: { downloadCount: 'DESC' } });
        return { sources: sources.map(s => ({ id: s.sourceId, name: s.name, version: s.version, url: s.downloadUrl, sha256: s.sha256 })), updateTime: new Date().toISOString() };
    }
    async getSource(id) {
        const s = await this.registryRepo.findOneBy({ sourceId: id });
        if (!s)
            throw new common_1.NotFoundException('源不存在');
        return s;
    }
    async getDownloadUrl(id) {
        const s = await this.registryRepo.findOneBy({ sourceId: id });
        if (!s)
            throw new common_1.NotFoundException('源不存在');
        return { url: s.downloadUrl, sha256: s.sha256 };
    }
    async registerSource(dto) {
        const existing = await this.registryRepo.findOneBy({ sourceId: dto.sourceId });
        if (existing) {
            await this.registryRepo.update(existing.id, { version: dto.version, downloadUrl: dto.downloadUrl, sha256: dto.sha256 });
            return this.registryRepo.findOneBy({ id: existing.id });
        }
        return this.registryRepo.save({ sourceId: dto.sourceId, name: dto.name, version: dto.version, downloadUrl: dto.downloadUrl, sha256: dto.sha256, status: 'active' });
    }
};
exports.SourceService = SourceService;
exports.SourceService = SourceService = __decorate([
    (0, common_1.Injectable)(),
    __param(0, (0, typeorm_1.InjectRepository)(community_entity_1.SourceRegistry)),
    __metadata("design:paramtypes", [typeof (_a = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _a : Object])
], SourceService);
//# sourceMappingURL=source.service.js.map