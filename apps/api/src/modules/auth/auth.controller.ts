import { Controller, Post, Body, HttpCode, HttpStatus, Ip, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { AuthService } from './auth.service';
import { Public, CurrentUser } from '../../common/guards/auth.guard';
import { JwtAuthGuard } from '../../common/guards/auth.guard';
import {
  SendCodeDto, VerifyCodeDto, RegisterDto, LoginDto,
  SmsLoginDto, RefreshTokenDto, ResetPasswordDto, ChangePasswordDto,
} from './dto/auth.dto';

@ApiTags('认证')
@Controller('auth')
export class AuthController {
  constructor(private authService: AuthService) {}

  @Public()
  @Post('send-code')
  @ApiOperation({ summary: '发送验证码' })
  @HttpCode(HttpStatus.OK)
  async sendCode(@Body() dto: SendCodeDto) {
    await this.authService.sendCode(dto.phone);
    return { message: '验证码已发送' };
  }

  @Public()
  @Post('register')
  @ApiOperation({ summary: '注册' })
  @HttpCode(HttpStatus.CREATED)
  async register(@Body() dto: RegisterDto, @Ip() ip: string) {
    return this.authService.register(dto.phone, dto.code, dto.password, dto.nickname);
  }

  @Public()
  @Post('login')
  @ApiOperation({ summary: '密码登录' })
  @HttpCode(HttpStatus.OK)
  async login(@Body() dto: LoginDto, @Ip() ip: string) {
    return this.authService.login(dto.phone, dto.password, ip);
  }

  @Public()
  @Post('sms-login')
  @ApiOperation({ summary: '验证码登录' })
  @HttpCode(HttpStatus.OK)
  async smsLogin(@Body() dto: SmsLoginDto, @Ip() ip: string) {
    return this.authService.smsLogin(dto.phone, dto.code, ip);
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

  @Public()
  @Post('reset-password')
  @ApiOperation({ summary: '重置密码' })
  @HttpCode(HttpStatus.OK)
  async resetPassword(@Body() dto: ResetPasswordDto) {
    await this.authService.resetPassword(dto.phone, dto.code, dto.newPassword);
    return { message: '密码已重置' };
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