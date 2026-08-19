"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.ComicModule = void 0;
const common_1 = require("@nestjs/common");
const typeorm_1 = require("@nestjs/typeorm");
const comic_entity_1 = require("./entities/comic.entity");
let ComicModule = class ComicModule {
};
exports.ComicModule = ComicModule;
exports.ComicModule = ComicModule = __decorate([
    (0, common_1.Module)({
        imports: [typeorm_1.TypeOrmModule.forFeature([comic_entity_1.Comic, comic_entity_1.ComicSource, comic_entity_1.Chapter, comic_entity_1.ReadingHistory, comic_entity_1.Favorite, comic_entity_1.FavoriteFolder])],
        exports: [typeorm_1.TypeOrmModule],
    })
], ComicModule);
//# sourceMappingURL=comic.module.js.map