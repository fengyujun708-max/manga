import { IsString, IsPhoneNumber, MinLength, MaxLength, IsOptional } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class SendCodeDto {
  @ApiProperty({ description: '手机号', example: '13800138000' })
  @IsString()
  phone: string;
}

export class VerifyCodeDto {
  @ApiProperty({ description: '手机号', example: '13800138000' })
  @IsString()
  phone: string;

  @ApiProperty({ description: '验证码', example: '123456' })
  @IsString()
  @MinLength(6)
  @MaxLength(6)
  code: string;
}

export class RegisterDto {
  @ApiProperty({ description: '手机号', example: '13800138000' })
  @IsString()
  phone: string;

  @ApiProperty({ description: '验证码', example: '123456' })
  @IsString()
  @MinLength(6)
  @MaxLength(6)
  code: string;

  @ApiProperty({ description: '密码', example: 'Password123' })
  @IsString()
  @MinLength(8)
  @MaxLength(32)
  password: string;

  @ApiProperty({ description: '昵称', example: '漫界用户' })
  @IsString()
  @MinLength(1)
  @MaxLength(50)
  nickname: string;
}

export class LoginDto {
  @ApiProperty({ description: '手机号', example: '13800138000' })
  @IsString()
  phone: string;

  @ApiProperty({ description: '密码', example: 'Password123' })
  @IsString()
  password: string;
}

export class SmsLoginDto {
  @ApiProperty({ description: '手机号', example: '13800138000' })
  @IsString()
  phone: string;

  @ApiProperty({ description: '验证码', example: '123456' })
  @IsString()
  @MinLength(6)
  @MaxLength(6)
  code: string;
}

export class RefreshTokenDto {
  @ApiProperty({ description: '刷新令牌' })
  @IsString()
  refreshToken: string;
}

export class ResetPasswordDto {
  @ApiProperty({ description: '手机号', example: '13800138000' })
  @IsString()
  phone: string;

  @ApiProperty({ description: '验证码', example: '123456' })
  @IsString()
  @MinLength(6)
  @MaxLength(6)
  code: string;

  @ApiProperty({ description: '新密码', example: 'NewPass123' })
  @IsString()
  @MinLength(8)
  @MaxLength(32)
  newPassword: string;
}

export class ChangePasswordDto {
  @ApiProperty({ description: '旧密码' })
  @IsString()
  oldPassword: string;

  @ApiProperty({ description: '新密码' })
  @IsString()
  @MinLength(8)
  @MaxLength(32)
  newPassword: string;
}

export class TokenResponse {
  @ApiProperty()
  accessToken: string;

  @ApiProperty()
  refreshToken: string;

  @ApiProperty()
  expiresIn: number;
}