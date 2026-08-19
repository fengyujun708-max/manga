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
exports.TokenResponse = exports.ChangePasswordDto = exports.ResetPasswordDto = exports.RefreshTokenDto = exports.SmsLoginDto = exports.LoginDto = exports.RegisterDto = exports.VerifyCodeDto = exports.SendCodeDto = void 0;
const class_validator_1 = require("class-validator");
const swagger_1 = require("@nestjs/swagger");
class SendCodeDto {
}
exports.SendCodeDto = SendCodeDto;
__decorate([
    (0, swagger_1.ApiProperty)({ description: '手机号', example: '13800138000' }),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], SendCodeDto.prototype, "phone", void 0);
class VerifyCodeDto {
}
exports.VerifyCodeDto = VerifyCodeDto;
__decorate([
    (0, swagger_1.ApiProperty)({ description: '手机号', example: '13800138000' }),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], VerifyCodeDto.prototype, "phone", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ description: '验证码', example: '123456' }),
    (0, class_validator_1.IsString)(),
    (0, class_validator_1.MinLength)(6),
    (0, class_validator_1.MaxLength)(6),
    __metadata("design:type", String)
], VerifyCodeDto.prototype, "code", void 0);
class RegisterDto {
}
exports.RegisterDto = RegisterDto;
__decorate([
    (0, swagger_1.ApiProperty)({ description: '手机号', example: '13800138000' }),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], RegisterDto.prototype, "phone", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ description: '验证码', example: '123456' }),
    (0, class_validator_1.IsString)(),
    (0, class_validator_1.MinLength)(6),
    (0, class_validator_1.MaxLength)(6),
    __metadata("design:type", String)
], RegisterDto.prototype, "code", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ description: '密码', example: 'Password123' }),
    (0, class_validator_1.IsString)(),
    (0, class_validator_1.MinLength)(8),
    (0, class_validator_1.MaxLength)(32),
    __metadata("design:type", String)
], RegisterDto.prototype, "password", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ description: '昵称', example: '漫界用户' }),
    (0, class_validator_1.IsString)(),
    (0, class_validator_1.MinLength)(1),
    (0, class_validator_1.MaxLength)(50),
    __metadata("design:type", String)
], RegisterDto.prototype, "nickname", void 0);
class LoginDto {
}
exports.LoginDto = LoginDto;
__decorate([
    (0, swagger_1.ApiProperty)({ description: '手机号', example: '13800138000' }),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], LoginDto.prototype, "phone", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ description: '密码', example: 'Password123' }),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], LoginDto.prototype, "password", void 0);
class SmsLoginDto {
}
exports.SmsLoginDto = SmsLoginDto;
__decorate([
    (0, swagger_1.ApiProperty)({ description: '手机号', example: '13800138000' }),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], SmsLoginDto.prototype, "phone", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ description: '验证码', example: '123456' }),
    (0, class_validator_1.IsString)(),
    (0, class_validator_1.MinLength)(6),
    (0, class_validator_1.MaxLength)(6),
    __metadata("design:type", String)
], SmsLoginDto.prototype, "code", void 0);
class RefreshTokenDto {
}
exports.RefreshTokenDto = RefreshTokenDto;
__decorate([
    (0, swagger_1.ApiProperty)({ description: '刷新令牌' }),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], RefreshTokenDto.prototype, "refreshToken", void 0);
class ResetPasswordDto {
}
exports.ResetPasswordDto = ResetPasswordDto;
__decorate([
    (0, swagger_1.ApiProperty)({ description: '手机号', example: '13800138000' }),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], ResetPasswordDto.prototype, "phone", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ description: '验证码', example: '123456' }),
    (0, class_validator_1.IsString)(),
    (0, class_validator_1.MinLength)(6),
    (0, class_validator_1.MaxLength)(6),
    __metadata("design:type", String)
], ResetPasswordDto.prototype, "code", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ description: '新密码', example: 'NewPass123' }),
    (0, class_validator_1.IsString)(),
    (0, class_validator_1.MinLength)(8),
    (0, class_validator_1.MaxLength)(32),
    __metadata("design:type", String)
], ResetPasswordDto.prototype, "newPassword", void 0);
class ChangePasswordDto {
}
exports.ChangePasswordDto = ChangePasswordDto;
__decorate([
    (0, swagger_1.ApiProperty)({ description: '旧密码' }),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], ChangePasswordDto.prototype, "oldPassword", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ description: '新密码' }),
    (0, class_validator_1.IsString)(),
    (0, class_validator_1.MinLength)(8),
    (0, class_validator_1.MaxLength)(32),
    __metadata("design:type", String)
], ChangePasswordDto.prototype, "newPassword", void 0);
class TokenResponse {
}
exports.TokenResponse = TokenResponse;
__decorate([
    (0, swagger_1.ApiProperty)(),
    __metadata("design:type", String)
], TokenResponse.prototype, "accessToken", void 0);
__decorate([
    (0, swagger_1.ApiProperty)(),
    __metadata("design:type", String)
], TokenResponse.prototype, "refreshToken", void 0);
__decorate([
    (0, swagger_1.ApiProperty)(),
    __metadata("design:type", Number)
], TokenResponse.prototype, "expiresIn", void 0);
//# sourceMappingURL=auth.dto.js.map