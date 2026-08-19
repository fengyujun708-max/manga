import { Controller, Get, Put, Body, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { UserService } from './user.service';
import { JwtAuthGuard, CurrentUser } from '../../common/guards/auth.guard';
import { UpdateProfileDto, UpdatePhoneDto, UpdateAvatarDto } from './dto/user.dto';

@ApiTags('用户')
@Controller('user')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class UserController {
  constructor(private userService: UserService) {}

  @Get('profile')
  @ApiOperation({ summary: '获取用户信息' })
  async getProfile(@CurrentUser('id') userId: string) {
    return this.userService.getProfile(userId);
  }

  @Put('profile')
  @ApiOperation({ summary: '修改资料' })
  async updateProfile(@CurrentUser('id') userId: string, @Body() dto: UpdateProfileDto) {
    return this.userService.updateProfile(userId, dto);
  }

  @Put('phone')
  @ApiOperation({ summary: '修改手机号' })
  async updatePhone(@CurrentUser('id') userId: string, @Body() dto: UpdatePhoneDto) {
    return this.userService.updatePhone(userId, dto.phone, dto.code);
  }

  @Get('devices')
  @ApiOperation({ summary: '设备列表' })
  async getDevices(@CurrentUser('id') userId: string) {
    return this.userService.getDevices(userId);
  }

  @Get('stats')
  @ApiOperation({ summary: '阅读统计' })
  async getStats(@CurrentUser('id') userId: string) {
    return this.userService.getStats(userId);
  }

  @Put('avatar')
  @ApiOperation({ summary: '修改头像' })
  async updateAvatar(@CurrentUser('id') userId: string, @Body() dto: UpdateAvatarDto) {
    return this.userService.updateAvatar(userId, dto.url);
  }
}