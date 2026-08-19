import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { HttpModule } from '@nestjs/axios';
import { ScheduleModule } from '@nestjs/schedule';

import { SourceSyncModule } from './source-sync/source-sync.module';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: '.env',
    }),
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
        entities: ['dist/**/*.entity{.ts,.js}'],
        synchronize: false,
        logging: false,
      }),
    }),
    HttpModule,
    ScheduleModule.forRoot(),
    SourceSyncModule,
  ],
})
export class WorkerModule {}