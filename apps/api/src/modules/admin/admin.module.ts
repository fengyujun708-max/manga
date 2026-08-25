import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AdminController } from './admin.controller';
import { AdminService } from './admin.service';
import { Report, Ban, AuditLog, AppVersion, RemoteConfig } from '../community/entities/community.entity';
import { User } from '../user/entities/user.entity';
import { OfficialModule } from '../official/official.module';

@Module({
  imports: [TypeOrmModule.forFeature([Report, Ban, AuditLog, AppVersion, RemoteConfig, User]), OfficialModule],
  controllers: [AdminController],
  providers: [AdminService],
  exports: [TypeOrmModule],
})
export class AdminModule {}