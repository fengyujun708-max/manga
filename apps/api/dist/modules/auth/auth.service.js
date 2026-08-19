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
var AuthService_1;
var _a, _b, _c, _d, _e, _f;
Object.defineProperty(exports, "__esModule", { value: true });
exports.AuthService = void 0;
const common_1 = require("@nestjs/common");
const jwt_1 = require("@nestjs/jwt");
const config_1 = require("@nestjs/config");
const typeorm_1 = require("@nestjs/typeorm");
const typeorm_2 = require("typeorm");
const schedule_1 = require("@nestjs/schedule");
const argon2 = require("argon2");
const uuid_1 = require("uuid");
const user_entity_1 = require("../../user/entities/user.entity");
const user_entity_2 = require("../../user/entities/user.entity");
let AuthService = AuthService_1 = class AuthService {
    constructor(userRepo, sessionRepo, verifyCodeRepo, loginSessionRepo, jwtService, config) {
        this.userRepo = userRepo;
        this.sessionRepo = sessionRepo;
        this.verifyCodeRepo = verifyCodeRepo;
        this.loginSessionRepo = loginSessionRepo;
        this.jwtService = jwtService;
        this.config = config;
        this.logger = new common_1.Logger(AuthService_1.name);
    }
    async sendCode(phone) {
        const recent = await this.verifyCodeRepo.findOne({
            where: { phone, purpose: 'register' },
            order: { createdAt: 'DESC' },
        });
        if (recent) {
            const elapsed = (Date.now() - recent.createdAt.getTime()) / 1000;
            if (elapsed < 60) {
                throw new common_1.BadRequestException('请 60 秒后再试');
            }
        }
        const code = Math.floor(100000 + Math.random() * 900000).toString();
        const expiresAt = new Date(Date.now() + 5 * 60 * 1000);
        await this.verifyCodeRepo.save({
            phone,
            code,
            purpose: 'register',
            expiresAt,
        });
        this.logger.log(`[DEV] 验证码 ${code} 发送到 ${phone}`);
    }
    async verifyCode(phone, code, purpose) {
        const record = await this.verifyCodeRepo.findOne({
            where: { phone, code, purpose, isUsed: false },
            order: { createdAt: 'DESC' },
        });
        if (!record)
            return false;
        if (record.expiresAt < new Date()) {
            throw new common_1.BadRequestException('验证码已过期');
        }
        if (record.attemptCount >= 5) {
            throw new common_1.BadRequestException('验证码错误次数过多');
        }
        record.attemptCount += 1;
        record.isUsed = true;
        await this.verifyCodeRepo.save(record);
        return true;
    }
    async register(phone, code, password, nickname) {
        const valid = await this.verifyCode(phone, code, 'register');
        if (!valid)
            throw new common_1.BadRequestException('验证码错误');
        const existing = await this.userRepo.findOneBy({ phone });
        if (existing)
            throw new common_1.ConflictException('手机号已注册');
        const passwordHash = await argon2.hash(password, {
            type: argon2.argon2id,
            memoryCost: 19456,
            timeCost: 2,
            parallelism: 1,
        });
        const user = await this.userRepo.save({
            phone,
            passwordHash,
            nickname,
            phoneVerified: true,
            role: user_entity_1.UserRole.USER,
            status: user_entity_1.UserStatus.ACTIVE,
        });
        return this.generateTokens(user);
    }
    async login(phone, password, ip) {
        const user = await this.userRepo.findOne({
            where: { phone },
            select: ['id', 'phone', 'passwordHash', 'nickname', 'avatar', 'role', 'status'],
        });
        if (!user) {
            await this.logLogin(phone, null, ip, false, '用户不存在');
            throw new common_1.UnauthorizedException('手机号或密码错误');
        }
        if (user.status !== user_entity_1.UserStatus.ACTIVE) {
            throw new common_1.UnauthorizedException('账号已被禁用');
        }
        const valid = await argon2.verify(user.passwordHash, password);
        if (!valid) {
            await this.logLogin(phone, user.id, ip, false, '密码错误');
            throw new common_1.UnauthorizedException('手机号或密码错误');
        }
        await this.userRepo.update(user.id, { lastLoginAt: new Date(), lastLoginIp: ip });
        await this.logLogin(phone, user.id, ip, true);
        return this.generateTokens(user);
    }
    async smsLogin(phone, code, ip) {
        const valid = await this.verifyCode(phone, code, 'login');
        if (!valid)
            throw new common_1.BadRequestException('验证码错误');
        let user = await this.userRepo.findOneBy({ phone });
        if (!user) {
            user = await this.userRepo.save({
                phone,
                passwordHash: '',
                nickname: `用户${phone.slice(-4)}`,
                phoneVerified: true,
                role: user_entity_1.UserRole.USER,
                status: user_entity_1.UserStatus.ACTIVE,
            });
        }
        await this.userRepo.update(user.id, { lastLoginAt: new Date(), lastLoginIp: ip });
        return this.generateTokens(user);
    }
    async generateTokens(user) {
        const payload = { sub: user.id, phone: user.phone, role: user.role };
        const accessToken = this.jwtService.sign(payload, {
            secret: this.config.get('JWT_SECRET', 'manjie-secret-dev'),
            expiresIn: '15m',
        });
        const refreshToken = (0, uuid_1.v4)();
        const expiresAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);
        await this.sessionRepo.save({
            userId: user.id,
            refreshToken: await argon2.hash(refreshToken),
            deviceId: 'default',
            expiresAt,
        });
        return {
            accessToken,
            refreshToken,
            expiresIn: 900,
            user: {
                id: user.id,
                phone: user.phone,
                nickname: user.nickname,
                avatar: user.avatar,
                role: user.role,
            },
        };
    }
    async refreshTokens(refreshToken) {
        const sessions = await this.sessionRepo.find({
            where: { isRevoked: false },
            select: ['id', 'userId', 'refreshToken', 'expiresAt'],
        });
        for (const session of sessions) {
            const valid = await argon2.verify(session.refreshToken, refreshToken);
            if (valid) {
                if (session.expiresAt < new Date()) {
                    throw new common_1.UnauthorizedException('Refresh token 已过期');
                }
                const user = await this.userRepo.findOneBy({ id: session.userId });
                if (!user)
                    throw new common_1.UnauthorizedException('用户不存在');
                await this.sessionRepo.update(session.id, { isRevoked: true });
                return this.generateTokens(user);
            }
        }
        throw new common_1.UnauthorizedException('Invalid refresh token');
    }
    async logout(userId, deviceId) {
        const query = { userId };
        if (deviceId)
            query.deviceId = deviceId;
        await this.sessionRepo.update(query, { isRevoked: true });
    }
    async logoutAllDevices(userId) {
        await this.sessionRepo.update({ userId }, { isRevoked: true });
    }
    async resetPassword(phone, code, newPassword) {
        const valid = await this.verifyCode(phone, code, 'reset_password');
        if (!valid)
            throw new common_1.BadRequestException('验证码错误');
        const passwordHash = await argon2.hash(newPassword, { type: argon2.argon2id });
        await this.userRepo.update({ phone }, { passwordHash });
        const user = await this.userRepo.findOneBy({ phone });
        if (user)
            await this.sessionRepo.update({ userId: user.id }, { isRevoked: true });
    }
    async changePassword(userId, oldPassword, newPassword) {
        const user = await this.userRepo.findOne({
            where: { id: userId },
            select: ['id', 'passwordHash'],
        });
        if (!user)
            throw new common_1.UnauthorizedException('用户不存在');
        const valid = await argon2.verify(user.passwordHash, oldPassword);
        if (!valid)
            throw new common_1.BadRequestException('旧密码错误');
        const passwordHash = await argon2.hash(newPassword, { type: argon2.argon2id });
        await this.userRepo.update(userId, { passwordHash });
    }
    async logLogin(phone, userId, ip, success, failReason) {
        await this.loginSessionRepo.save({ phone, userId, ip, success, failReason });
    }
    async cleanExpiredCodes() {
        await this.verifyCodeRepo.delete({ expiresAt: new Date(Date.now() - 24 * 60 * 60 * 1000) });
    }
};
exports.AuthService = AuthService;
__decorate([
    (0, schedule_1.Cron)(schedule_1.CronExpression.EVERY_DAY_AT_3AM),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], AuthService.prototype, "cleanExpiredCodes", null);
exports.AuthService = AuthService = AuthService_1 = __decorate([
    (0, common_1.Injectable)(),
    __param(0, (0, typeorm_1.InjectRepository)(user_entity_1.User)),
    __param(1, (0, typeorm_1.InjectRepository)(user_entity_2.UserSession)),
    __param(2, (0, typeorm_1.InjectRepository)(user_entity_2.VerificationCode)),
    __param(3, (0, typeorm_1.InjectRepository)(user_entity_2.LoginSession)),
    __metadata("design:paramtypes", [typeof (_a = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _a : Object, typeof (_b = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _b : Object, typeof (_c = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _c : Object, typeof (_d = typeof typeorm_2.Repository !== "undefined" && typeorm_2.Repository) === "function" ? _d : Object, typeof (_e = typeof jwt_1.JwtService !== "undefined" && jwt_1.JwtService) === "function" ? _e : Object, typeof (_f = typeof config_1.ConfigService !== "undefined" && config_1.ConfigService) === "function" ? _f : Object])
], AuthService);
//# sourceMappingURL=auth.service.js.map