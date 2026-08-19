"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.UpdateModule = void 0;
const common_1 = require("@nestjs/common");
const typeorm_1 = require("@nestjs/typeorm");
const update_controller_1 = require("./update.controller");
const update_service_1 = require("./update.service");
const community_entity_1 = require("../community/entities/community.entity");
const user_entity_1 = require("../user/entities/user.entity");
let UpdateModule = class UpdateModule {
};
exports.UpdateModule = UpdateModule;
exports.UpdateModule = UpdateModule = __decorate([
    (0, common_1.Module)({
        imports: [typeorm_1.TypeOrmModule.forFeature([community_entity_1.AppVersion, community_entity_1.RemoteConfig, community_entity_1.Announcement, user_entity_1.UserDevice])],
        controllers: [update_controller_1.UpdateController],
        providers: [update_service_1.UpdateService],
        exports: [update_service_1.UpdateService],
    })
], UpdateModule);
//# sourceMappingURL=update.module.js.map