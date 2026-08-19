import { Controller, Post, Get, Body, HttpCode, HttpStatus, Ip, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { AuthService } from './auth.service';
import { CaptchaService } from './captcha.service';
import { Public, CurrentUser } from '../../common/guards/auth.guard';
import { JwtAuthGuard } from '../../common/guards/auth.guard';
import { IsString, MinLength, MaxLength, Length, Matches } from 'class-validator';

class RegisterDto {
  @IsString()
  @Matches(/^1[3-9]\d{9}$/, { message: '手机号格式不正确' })
  phone: string;

  @IsString() @MinLength(8) @MaxLength(32) password: string;
  @IsString() confirmPassword: string;
  @IsString() @MinLength(1) @MaxLength(50) nickname: string;

  @IsString() captchaId: string;
  @IsString() @Length(4, 4) captchaAnswer: string;
}

class LoginDto {
  @IsString() phone: string;
  @IsString() password: string;
}

class RefreshTokenDto {
  @IsString() refreshToken: string;
}

class ChangePasswordDto {
  @IsString() oldPassword: string;
  @IsString() @MinLength(8) @MaxLength(32) newPassword: string;
}

@ApiTags('认证')
@Controller('auth')
export class AuthController {
  constructor(
    private authService: AuthService,
    private captchaService: CaptchaService,
  ) {}

  @Public()
  @Get('captcha')
  @ApiOperation({ summary: '获取图片验证码' })
  async getCaptcha() {
    const captcha = await this.captchaService.generate();
    return { id: captcha.id, svg: captcha.svg };
  }

  @Public()
  @Post('register')
  @ApiOperation({ summary: '注册（需图片验证码）' })
  @HttpCode(HttpStatus.CREATED)
  async register(@Body() dto: RegisterDto, @Ip() ip: string) {
    return this.authService.register(dto.phone, dto.password, dto.confirmPassword, dto.nickname, dto.captchaId, dto.captchaAnswer);
  }

  @Public()
  @Post('login')
  @ApiOperation({ summary: '密码登录' })
  @HttpCode(HttpStatus.OK)
  async login(@Body() dto: LoginDto, @Ip() ip: string) {
    return this.authService.login(dto.phone, dto.password, ip);
  }

  @Public()
  @Post('refresh')
  @ApiOperation({ summary: '刷新 Token' })
  @HttpCode(HttpStatus.OK)
  async refresh(@Body() dto: RefreshTokenDto) {
    return this.authService.refreshTokens(dto.refreshToken);
  }

  @UseGuards(JwtAuthGuard)
  @Post('logout')
  @ApiBearerAuth()
  @ApiOperation({ summary: '退出登录' })
  @HttpCode(HttpStatus.OK)
  async logout(@CurrentUser('id') userId: string) {
    await this.authService.logout(userId);
    return { message: '已退出登录' };
  }

  @UseGuards(JwtAuthGuard)
  @Post('logout-all')
  @ApiBearerAuth()
  @ApiOperation({ summary: '退出所有设备' })
  @HttpCode(HttpStatus.OK)
  async logoutAll(@CurrentUser('id') userId: string) {
    await this.authService.logoutAllDevices(userId);
    return { message: '已退出所有设备' };
  }

  @UseGuards(JwtAuthGuard)
  @Post('change-password')
  @ApiBearerAuth()
  @ApiOperation({ summary: '修改密码' })
  @HttpCode(HttpStatus.OK)
  async changePassword(@CurrentUser('id') userId: string, @Body() dto: ChangePasswordDto) {
    await this.authService.changePassword(userId, dto.oldPassword, dto.newPassword);
    return { message: '密码已修改' };
  }
}