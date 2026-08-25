import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { SourceRegistry } from './entities/source-registry.entity';
import { SourceRegistryService } from './source-registry.service';
import { SourceRegistryController } from './source-registry.controller';

@Module({
  imports: [TypeOrmModule.forFeature([SourceRegistry])],
  controllers: [SourceRegistryController],
  providers: [SourceRegistryService],
  exports: [SourceRegistryService],
})
export class SourceRegistryModule {}
