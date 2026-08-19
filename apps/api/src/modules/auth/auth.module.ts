import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { PassportModule } from '@nestjs/passport';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ConfigModule, ConfigService } from '@nestjs/config';

import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';
import { CaptchaService } from './captcha.service';
import { JwtStrategy, JwtRefreshStrategy } from './strategies/jwt.strategy';
import { User } from '../user/entities/user.entity';
import { UserSession, VerificationCode, LoginSession } from '../user/entities/user.entity';

@Module({
  imports: [
    TypeOrmModule.forFeature([User, UserSession, VerificationCode, LoginSession]),
    PassportModule.register({ defaultStrategy: 'jwt' }),
    JwtModule.registerAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        secret: config.get<string>('JWT_SECRET', 'manjie-secret-dev'),
        signOptions: { expiresIn: '15m' },
      }),
    }),
  ],
  controllers: [AuthController],
  providers: [AuthService, CaptchaService, JwtStrategy, JwtRefreshStrategy],
  exports: [AuthService, CaptchaService, JwtModule],
})
export class AuthModule {}