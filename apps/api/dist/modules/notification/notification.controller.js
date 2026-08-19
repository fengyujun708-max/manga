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
exports.NotificationController = void 0;
const common_1 = require("@nestjs/common");
const swagger_1 = require("@nestjs/swagger");
const notification_service_1 = require("./notification.service");
const auth_guard_1 = require("../../common/guards/auth.guard");
let NotificationController = class NotificationController {
    constructor(notificationService) {
        this.notificationService = notificationService;
    }
    async getNotifications(userId) { return this.notificationService.getNotifications(userId); }
    async markRead(userId, id) { return this.notificationService.markRead(userId, id); }
    async getUnreadCount(userId) { return this.notificationService.getUnreadCount(userId); }
};
exports.NotificationController = NotificationController;
__decorate([
    (0, common_1.Get)(),
    (0, swagger_1.ApiOperation)({ summary: '通知列表' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], NotificationController.prototype, "getNotifications", null);
__decorate([
    (0, common_1.Put)(':id/read'),
    (0, swagger_1.ApiOperation)({ summary: '标记已读' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __param(1, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String]),
    __metadata("design:returntype", Promise)
], NotificationController.prototype, "markRead", null);
__decorate([
    (0, common_1.Get)('unread-count'),
    (0, swagger_1.ApiOperation)({ summary: '未读通知数' }),
    __param(0, (0, auth_guard_1.CurrentUser)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], NotificationController.prototype, "getUnreadCount", null);
exports.NotificationController = NotificationController = __decorate([
    (0, swagger_1.ApiTags)('通知'),
    (0, common_1.Controller)('notifications'),
    (0, common_1.UseGuards)(auth_guard_1.JwtAuthGuard),
    (0, swagger_1.ApiBearerAuth)(),
    __metadata("design:paramtypes", [notification_service_1.NotificationService])
], NotificationController);
//# sourceMappingURL=notification.controller.js.map