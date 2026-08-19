import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, Index } from 'typeorm';

@Entity('source_sync_log')
@Index(['sourceId', 'createdAt'])
export class SourceSyncLog {
  @PrimaryGeneratedColumn('uuid') id: string;
  @Column({ type: 'varchar', length: 100 }) sourceId: string;
  @Column({ type: 'varchar', length: 50 }) action: string;
  @Column({ type: 'varchar', length: 20 }) status: string;
  @Column({ type: 'text', nullable: true }) message: string;
  @Column({ type: 'json', nullable: true }) details: Record<string, any>;
  @Column({ type: 'int', nullable: true }) duration: number;
  @CreateDateColumn() createdAt: Date;
}
