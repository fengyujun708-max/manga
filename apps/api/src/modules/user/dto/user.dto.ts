import { IsString, MaxLength, MinLength, IsOptional, IsUrl } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class UpdateProfileDto {
  @ApiProperty({ description: '昵称' })
  @IsString()
  @MinLength(1)
  @MaxLength(50)
  nickname: string;
}

export class UpdatePhoneDto {
  @ApiProperty({ description: '新手机号' })
  @IsString()
  phone: string;

  @ApiProperty({ description: '验证码' })
  @IsString()
  code: string;
}

export class UpdateAvatarDto {
  @ApiProperty({ description: '头像 URL' })
  @IsString()
  url: string;
}