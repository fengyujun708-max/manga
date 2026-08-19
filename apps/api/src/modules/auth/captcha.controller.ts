import { Controller, Get, Post, Body, HttpCode, HttpStatus } from '@nestjs/common';
import { ApiTags, ApiOperation } from '@nestjs/swagger';
import { CaptchaService } from './captcha.service';
import { Public } from '../../common/guards/auth.guard';
import { IsString, Length, Matches, MinLength, MaxLength } from 'class-validator';

export class RegisterDto {
  @IsString()
  @Matches(/^1[3-9]\d{9}$/, { message: '手机号格式不正确（需要11位手机号）' })
  phone: string;

  @IsString()
  @MinLength(8, { message: '密码至少8位' })
  @MaxLength(32, { message: '密码最多32位' })
  password: string;

  @IsString()
  confirmPassword: string;

  @IsString()
  @MinLength(1)
  @MaxLength(50)
  nickname: string;

  @IsString()
  captchaId: string;

  @IsString()
  @Length(4, 4, { message: '验证码为4位数字' })
  captchaAnswer: string;
}

@ApiTags('验证码')
@Controller('auth')
export class CaptchaController {
  constructor(private captchaService: CaptchaService) {}

  @Public()
  @Get('captcha')
  @ApiOperation({ summary: '获取图片验证码' })
  async getCaptcha() {
    const captcha = await this.captchaService.generate();
    // 不返回 answer（仅在服务端校验）
    return { id: captcha.id, svg: captcha.svg };
  }
}