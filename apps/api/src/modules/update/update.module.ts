import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { UpdateController } from './update.controller';
import { UpdateService } from './update.service';
import { AppVersion, RemoteConfig, Announcement } from '../community/entities/community.entity';
import { UserDevice } from '../user/entities/user.entity';

@Module({
  imports: [TypeOrmModule.forFeature([AppVersion, RemoteConfig, Announcement, UserDevice])],
  controllers: [UpdateController],
  providers: [UpdateService],
  exports: [UpdateService],
})
export class UpdateModule {}