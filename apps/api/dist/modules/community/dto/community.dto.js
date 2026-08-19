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
exports.CreateCommentDto = exports.CreatePostDto = void 0;
const class_validator_1 = require("class-validator");
const swagger_1 = require("@nestjs/swagger");
class CreatePostDto {
}
exports.CreatePostDto = CreatePostDto;
__decorate([
    (0, swagger_1.ApiProperty)({ description: '标题' }),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], CreatePostDto.prototype, "title", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ description: '内容' }),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], CreatePostDto.prototype, "content", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ description: '标签', required: false }),
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsArray)(),
    (0, class_validator_1.IsString)({ each: true }),
    __metadata("design:type", Array)
], CreatePostDto.prototype, "tags", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ description: '类型', required: false }),
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], CreatePostDto.prototype, "type", void 0);
class CreateCommentDto {
}
exports.CreateCommentDto = CreateCommentDto;
__decorate([
    (0, swagger_1.ApiProperty)({ description: '评论内容' }),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], CreateCommentDto.prototype, "content", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ description: '父评论 ID', required: false }),
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], CreateCommentDto.prototype, "parentId", void 0);
__decorate([
    (0, swagger_1.ApiProperty)({ description: '回复用户 ID', required: false }),
    (0, class_validator_1.IsOptional)(),
    (0, class_validator_1.IsString)(),
    __metadata("design:type", String)
], CreateCommentDto.prototype, "replyToUserId", void 0);
//# sourceMappingURL=community.dto.js.map