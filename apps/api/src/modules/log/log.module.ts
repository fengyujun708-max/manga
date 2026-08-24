import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { LogController } from './log.controller';
import { LogService } from './log.service';
import { ClientLog } from './entities/client-log.entity';

@Module({
  imports: [TypeOrmModule.forFeature([ClientLog])],
  controllers: [LogController],
  providers: [LogService],
})
export class LogModule {}