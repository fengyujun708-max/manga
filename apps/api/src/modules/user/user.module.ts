import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { UserController } from './user.controller';
import { UserService } from './user.service';
import { AccountController } from './account.controller';
import { AccountService } from './account.service';
import { User, UserDevice, UserSession, VerificationCode, LoginSession } from './entities/user.entity';
import { ReadingHistory, Favorite } from '../comic/entities/comic.entity';

@Module({
  imports: [TypeOrmModule.forFeature([User, UserDevice, UserSession, VerificationCode, LoginSession, ReadingHistory, Favorite])],
  controllers: [UserController, AccountController],
  providers: [UserService, AccountService],
  exports: [TypeOrmModule],
})
export class UserModule {}
