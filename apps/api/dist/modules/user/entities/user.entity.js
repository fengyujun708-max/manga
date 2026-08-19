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
Object.defineProperty(exports, "__esModule", { value: true });
exports.LoginSession = exports.VerificationCode = exports.UserSession = exports.UserDevice = exports.User = exports.UserStatus = exports.UserRole = void 0;
const typeorm_1 = require("typeorm");
var UserRole;
(function (UserRole) {
    UserRole["SUPER_ADMIN"] = "super_admin";
    UserRole["ADMIN"] = "admin";
    UserRole["MODERATOR"] = "moderator";
    UserRole["SOURCE_MANAGER"] = "source_manager";
    UserRole["CONTENT_MANAGER"] = "content_manager";
    UserRole["USER"] = "user";
    UserRole["BANNED"] = "banned";
})(UserRole || (exports.UserRole = UserRole = {}));
var UserStatus;
(function (UserStatus) {
    UserStatus["ACTIVE"] = "active";
    UserStatus["INACTIVE"] = "inactive";
    UserStatus["SUSPENDED"] = "suspended";
    UserStatus["DELETED"] = "deleted";
})(UserStatus || (exports.UserStatus = UserStatus = {}));
let User = class User {
};
exports.User = User;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], User.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Index)({ unique: true }),
    (0, typeorm_1.Column)({ type: 'varchar', length: 20, unique: true }),
    __metadata("design:type", String)
], User.prototype, "phone", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'boolean', default: false }),
    __metadata("design:type", Boolean)
], User.prototype, "phoneVerified", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 255, select: false }),
    __metadata("design:type", String)
], User.prototype, "passwordHash", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 50 }),
    __metadata("design:type", String)
], User.prototype, "nickname", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 500, nullable: true }),
    __metadata("design:type", String)
], User.prototype, "avatar", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'enum', enum: UserStatus, default: UserStatus.ACTIVE }),
    __metadata("design:type", String)
], User.prototype, "status", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'enum', enum: UserRole, default: UserRole.USER }),
    __metadata("design:type", String)
], User.prototype, "role", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'timestamp', nullable: true }),
    __metadata("design:type", Date)
], User.prototype, "lastLoginAt", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 45, nullable: true }),
    __metadata("design:type", String)
], User.prototype, "lastLoginIp", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], User.prototype, "createdAt", void 0);
__decorate([
    (0, typeorm_1.UpdateDateColumn)(),
    __metadata("design:type", Date)
], User.prototype, "updatedAt", void 0);
exports.User = User = __decorate([
    (0, typeorm_1.Entity)('users')
], User);
let UserDevice = class UserDevice {
};
exports.UserDevice = UserDevice;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], UserDevice.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], UserDevice.prototype, "userId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 255 }),
    __metadata("design:type", String)
], UserDevice.prototype, "deviceName", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 100 }),
    __metadata("design:type", String)
], UserDevice.prototype, "deviceId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 45 }),
    __metadata("design:type", String)
], UserDevice.prototype, "ip", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'timestamp', default: () => 'CURRENT_TIMESTAMP' }),
    __metadata("design:type", Date)
], UserDevice.prototype, "lastActiveAt", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], UserDevice.prototype, "createdAt", void 0);
exports.UserDevice = UserDevice = __decorate([
    (0, typeorm_1.Entity)('user_devices')
], UserDevice);
let UserSession = class UserSession {
};
exports.UserSession = UserSession;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], UserSession.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid' }),
    __metadata("design:type", String)
], UserSession.prototype, "userId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 512, select: false }),
    __metadata("design:type", String)
], UserSession.prototype, "refreshToken", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 100 }),
    __metadata("design:type", String)
], UserSession.prototype, "deviceId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'timestamp' }),
    __metadata("design:type", Date)
], UserSession.prototype, "expiresAt", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'boolean', default: false }),
    __metadata("design:type", Boolean)
], UserSession.prototype, "isRevoked", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], UserSession.prototype, "createdAt", void 0);
__decorate([
    (0, typeorm_1.UpdateDateColumn)(),
    __metadata("design:type", Date)
], UserSession.prototype, "updatedAt", void 0);
exports.UserSession = UserSession = __decorate([
    (0, typeorm_1.Entity)('user_sessions')
], UserSession);
let VerificationCode = class VerificationCode {
};
exports.VerificationCode = VerificationCode;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], VerificationCode.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 20 }),
    __metadata("design:type", String)
], VerificationCode.prototype, "phone", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 6 }),
    __metadata("design:type", String)
], VerificationCode.prototype, "code", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 50 }),
    __metadata("design:type", String)
], VerificationCode.prototype, "purpose", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'timestamp' }),
    __metadata("design:type", Date)
], VerificationCode.prototype, "expiresAt", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'boolean', default: false }),
    __metadata("design:type", Boolean)
], VerificationCode.prototype, "isUsed", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'int', default: 0 }),
    __metadata("design:type", Number)
], VerificationCode.prototype, "attemptCount", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], VerificationCode.prototype, "createdAt", void 0);
exports.VerificationCode = VerificationCode = __decorate([
    (0, typeorm_1.Entity)('verification_codes')
], VerificationCode);
let LoginSession = class LoginSession {
};
exports.LoginSession = LoginSession;
__decorate([
    (0, typeorm_1.PrimaryGeneratedColumn)('uuid'),
    __metadata("design:type", String)
], LoginSession.prototype, "id", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'uuid', nullable: true }),
    __metadata("design:type", String)
], LoginSession.prototype, "userId", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 20 }),
    __metadata("design:type", String)
], LoginSession.prototype, "phone", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 45 }),
    __metadata("design:type", String)
], LoginSession.prototype, "ip", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 500, nullable: true }),
    __metadata("design:type", String)
], LoginSession.prototype, "userAgent", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'boolean', default: true }),
    __metadata("design:type", Boolean)
], LoginSession.prototype, "success", void 0);
__decorate([
    (0, typeorm_1.Column)({ type: 'varchar', length: 255, nullable: true }),
    __metadata("design:type", String)
], LoginSession.prototype, "failReason", void 0);
__decorate([
    (0, typeorm_1.CreateDateColumn)(),
    __metadata("design:type", Date)
], LoginSession.prototype, "createdAt", void 0);
exports.LoginSession = LoginSession = __decorate([
    (0, typeorm_1.Entity)('login_sessions')
], LoginSession);
//# sourceMappingURL=user.entity.js.map