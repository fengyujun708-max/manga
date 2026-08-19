import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ThrottlerModule, ThrottlerGuard } from '@nestjs/throttler';
import { APP_GUARD } from '@nestjs/core';
import { ScheduleModule } from '@nestjs/schedule';

import { AuthModule } from './modules/auth/auth.module';
import { UserModule } from './modules/user/user.module';
import { SmsModule } from './modules/sms/sms.module';
import { ComicModule } from './modules/comic/comic.module';
import { CommunityModule } from './modules/community/community.module';
import { RequestModule } from './modules/request/request.module';
import { SourceModule } from './modules/source/source.module';
import { NotificationModule } from './modules/notification/notification.module';
import { AnnouncementModule } from './modules/announcement/announcement.module';
import { UpdateModule } from './modules/update/update.module';
import { AdminModule } from './modules/admin/admin.module';

@Module({
  imports: [
    // Config
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: '.env',
    }),

    // Database
    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        type: 'postgres',
        host: config.get<string>('DB_HOST', 'localhost'),
        port: config.get<number>('DB_PORT', 5432),
        username: config.get<string>('DB_USERNAME', 'manjie'),
        password: config.get<string>('DB_PASSWORD', 'manjie'),
        database: config.get<string>('DB_DATABASE', 'manjie'),
        entities: [__dirname + '/**/*.entity{.ts,.js}'],
        migrations: [__dirname + '/database/migrations/*{.ts,.js}'],
        synchronize: config.get<string>('NODE_ENV') === 'development',
        logging: config.get<string>('NODE_ENV') === 'development',
      }),
    }),

    // Rate Limiting
    ThrottlerModule.forRootAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        throttlers: [
          {
            ttl: 60000,
            limit: config.get<number>('RATE_LIMIT_GLOBAL', 60),
          },
        ],
      }),
    }),

    // Scheduling
    ScheduleModule.forRoot(),

    // Feature Modules
    AuthModule,
    UserModule,
    SmsModule,
    ComicModule,
    CommunityModule,
    RequestModule,
    SourceModule,
    NotificationModule,
    AnnouncementModule,
    UpdateModule,
    AdminModule,
  ],
  providers: [
    {
      provide: APP_GUARD,
      useClass: ThrottlerGuard,
    },
  ],
})
export class AppModule {}