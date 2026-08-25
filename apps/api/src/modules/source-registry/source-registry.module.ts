import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { SourceRegistryController } from './source-registry.controller';
import { SourceRegistryService } from './source-registry.service';
import { SourceRegistry } from './entities/source-registry.entity';

@Module({
  imports: [TypeOrmModule.forFeature([SourceRegistry])],
  controllers: [SourceRegistryController],
  providers: [SourceRegistryService],
  exports: [SourceRegistryService],
})
export class SourceRegistryModule {}
