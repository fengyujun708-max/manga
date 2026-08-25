import { IsString, IsBoolean, IsArray, IsOptional, IsUrl, IsNumber, IsPositive, Min, Max } from 'class-validator';

export class CreateSourceRegistryDto {
  @IsString()
  id: string;

  @IsString()
  displayName: string;

  @IsString()
  @IsOptional()
  description?: string;

  @IsString()
  @IsOptional()
  author?: string;

  @IsUrl()
  @IsOptional()
  repositoryUrl?: string;

  @IsString()
  version: string;

  @IsBoolean()
  @IsOptional()
  enabled?: boolean;

  @IsBoolean()
  @IsOptional()
  vetted?: boolean;

  @IsString()
  @IsOptional()
  minAppVersion?: string;

  @IsUrl()
  @IsOptional()
  downloadUrl?: string;

  @IsUrl()
  @IsOptional()
  iconUrl?: string;

  @IsOptional()
  metadata?: Record<string, any>;

  @IsArray()
  @IsString({ each: true })
  @IsOptional()
  tags?: string[];

  @IsArray()
  @IsString({ each: true })
  @IsOptional()
  capabilities?: string[];

  @IsString()
  @IsOptional()
  networkType?: string;
}
