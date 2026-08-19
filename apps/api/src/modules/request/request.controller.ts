import { Controller, Get, Post, Body, Param, Query, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { RequestService } from './request.service';
import { JwtAuthGuard, Public, CurrentUser } from '../../common/guards/auth.guard';
import { IsString, IsOptional } from 'class-validator';

class CreateMangaRequestDto {
  @IsString() mangaName: string;
  @IsOptional() @IsString() altName?: string;
  @IsOptional() @IsString() author?: string;
  @IsOptional() @IsString() description?: string;
  @IsOptional() @IsString() notes?: string;
}

class CreateSourceRequestDto {
  @IsString() sourceName: string;
  @IsOptional() @IsString() sourceUrl?: string;
  @IsOptional() @IsString() description?: string;
  @IsOptional() @IsString() notes?: string;
}

@ApiTags('求漫/求源')
@Controller('request')
export class RequestController {
  constructor(private requestService: RequestService) {}

  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @Post('manga')
  @ApiOperation({ summary: '发布求漫' })
  async createMangaRequest(@CurrentUser('id') userId: string, @Body() dto: CreateMangaRequestDto) {
    return this.requestService.createMangaRequest(userId, dto);
  }

  @Public()
  @Get('manga')
  @ApiOperation({ summary: '求漫列表' })
  async getMangaRequests(@Query('page') page = 1, @Query('limit') limit = 20, @Query('status') status?: string) {
    return this.requestService.getMangaRequests(page, limit, status);
  }

  @Public()
  @Get('manga/:id')
  @ApiOperation({ summary: '求漫详情' })
  async getMangaRequest(@Param('id') id: string) {
    return this.requestService.getMangaRequest(id);
  }

  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @Post('source')
  @ApiOperation({ summary: '发布求源' })
  async createSourceRequest(@CurrentUser('id') userId: string, @Body() dto: CreateSourceRequestDto) {
    return this.requestService.createSourceRequest(userId, dto);
  }

  @Public()
  @Get('source')
  @ApiOperation({ summary: '求源列表' })
  async getSourceRequests(@Query('page') page = 1, @Query('limit') limit = 20) {
    return this.requestService.getSourceRequests(page, limit);
  }
}