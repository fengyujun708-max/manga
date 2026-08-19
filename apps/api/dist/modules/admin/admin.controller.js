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
exports.AdminController = void 0;
const common_1 = require("@nestjs/common");
const swagger_1 = require("@nestjs/swagger");
const admin_service_1 = require("./admin.service");
const auth_guard_1 = require("../../common/guards/auth.guard");
const auth_guard_2 = require("../../common/guards/auth.guard");
let AdminController = class AdminController {
    constructor(adminService) {
        this.adminService = adminService;
    }
    async getDashboard() { return this.adminService.getDashboard(); }
    async getUsers(page = 1, limit = 20) { return this.adminService.getUsers(page, limit); }
    async banUser(adminId, userId, reason) {
        return this.adminService.banUser(adminId, userId, reason);
    }
    async getReports() { return this.adminService.getReports(); }
    async resolveReport(id) { return this.adminService.resolveReport(id); }
};
exports.AdminController = AdminController;
__decorate([
    (0, common_1.Get)('dashboard'),
    (0, swagger_1.ApiOperation)({ summary: '仪表盘' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], AdminController.prototype, "getDashboard", null);
__decorate([
    (0, common_1.Get)('users'),
    (0, swagger_1.ApiOperation)({ summary: '用户列表' }),
    __param(0, (0, common_1.Query)('page')),
    __param(1, (0, common_1.Query)('limit')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, Object]),
    __metadata("design:returntype", Promise)
], AdminController.prototype, "getUsers", null);
__decorate([
    (0, common_1.Put)('users/:id/ban'),
    (0, swagger_1.ApiOperation)({ summary: '封禁用户' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __param(1, (0, common_1.Param)('id')),
    __param(2, (0, common_1.Body)('reason')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, String]),
    __metadata("design:returntype", Promise)
], AdminController.prototype, "banUser", null);
__decorate([
    (0, common_1.Get)('reports'),
    (0, swagger_1.ApiOperation)({ summary: '举报列表' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], AdminController.prototype, "getReports", null);
__decorate([
    (0, common_1.Put)('reports/:id/resolve'),
    (0, swagger_1.ApiOperation)({ summary: '处理举报' }),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], AdminController.prototype, "resolveReport", null);
exports.AdminController = AdminController = __decorate([
    (0, swagger_1.ApiTags)('管理后台'),
    (0, common_1.Controller)('admin'),
    (0, common_1.UseGuards)(auth_guard_1.JwtAuthGuard, auth_guard_2.RolesGuard),
    (0, auth_guard_1.Roles)('super_admin', 'admin'),
    (0, swagger_1.ApiBearerAuth)(),
    __metadata("design:paramtypes", [admin_service_1.AdminService])
], AdminController);
//# sourceMappingURL=admin.controller.js.map