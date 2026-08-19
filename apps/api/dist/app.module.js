"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.AppModule = void 0;
const common_1 = require("@nestjs/common");
const config_1 = require("@nestjs/config");
const typeorm_1 = require("@nestjs/typeorm");
const throttler_1 = require("@nestjs/throttler");
const core_1 = require("@nestjs/core");
const schedule_1 = require("@nestjs/schedule");
const auth_module_1 = require("./modules/auth/auth.module");
const user_module_1 = require("./modules/user/user.module");
const sms_module_1 = require("./modules/sms/sms.module");
const comic_module_1 = require("./modules/comic/comic.module");
const community_module_1 = require("./modules/community/community.module");
const request_module_1 = require("./modules/request/request.module");
const source_module_1 = require("./modules/source/source.module");
const notification_module_1 = require("./modules/notification/notification.module");
const announcement_module_1 = require("./modules/announcement/announcement.module");
const update_module_1 = require("./modules/update/update.module");
const admin_module_1 = require("./modules/admin/admin.module");
let AppModule = class AppModule {
};
exports.AppModule = AppModule;
exports.AppModule = AppModule = __decorate([
    (0, common_1.Module)({
        imports: [
            config_1.ConfigModule.forRoot({
                isGlobal: true,
                envFilePath: '.env',
            }),
            typeorm_1.TypeOrmModule.forRootAsync({
                imports: [config_1.ConfigModule],
                inject: [config_1.ConfigService],
                useFactory: (config) => ({
                    type: 'postgres',
                    host: config.get('DB_HOST', 'localhost'),
                    port: config.get('DB_PORT', 5432),
                    username: config.get('DB_USERNAME', 'manjie'),
                    password: config.get('DB_PASSWORD', 'manjie'),
                    database: config.get('DB_DATABASE', 'manjie'),
                    entities: [__dirname + '/**/*.entity{.ts,.js}'],
                    migrations: [__dirname + '/database/migrations/*{.ts,.js}'],
                    synchronize: config.get('NODE_ENV') === 'development',
                    logging: config.get('NODE_ENV') === 'development',
                }),
            }),
            throttler_1.ThrottlerModule.forRootAsync({
                imports: [config_1.ConfigModule],
                inject: [config_1.ConfigService],
                useFactory: (config) => ({
                    throttlers: [
                        {
                            ttl: 60000,
                            limit: config.get('RATE_LIMIT_GLOBAL', 60),
                        },
                    ],
                }),
            }),
            schedule_1.ScheduleModule.forRoot(),
            auth_module_1.AuthModule,
            user_module_1.UserModule,
            sms_module_1.SmsModule,
            comic_module_1.ComicModule,
            community_module_1.CommunityModule,
            request_module_1.RequestModule,
            source_module_1.SourceModule,
            notification_module_1.NotificationModule,
            announcement_module_1.AnnouncementModule,
            update_module_1.UpdateModule,
            admin_module_1.AdminModule,
        ],
        providers: [
            {
                provide: core_1.APP_GUARD,
                useClass: throttler_1.ThrottlerGuard,
            },
        ],
    })
], AppModule);
//# sourceMappingURL=app.module.js.map