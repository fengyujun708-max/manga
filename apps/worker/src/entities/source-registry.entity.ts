import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, UpdateDateColumn, Index } from 'typeorm';

@Entity('source_registry')
@Index(['sourceId'], { unique: true })
@Index(['status'])
@Index(['downloadCount'])
export class SourceRegistry {
  @PrimaryGeneratedColumn('uuid') id: string;
  @Column({ type: 'varchar', length: 100, unique: true }) sourceId: string;
  @Column({ type: 'varchar', length: 200 }) name: string;
  @Column({ type: 'varchar', length: 50 }) version: string;
  @Column({ type: 'varchar', length: 100, nullable: true }) author: string;
  @Column({ type: 'text', nullable: true }) description: string;
  @Column({ type: 'varchar', length: 100, nullable: true }) icon: string;
  @Column({ type: 'varchar', length: 500 }) downloadUrl: string;
  @Column({ type: 'varchar', length: 64 }) sha256: string;
  @Column({ type: 'varchar', length: 50, nullable: true }) minAppVersion: string;
  @Column({ type: 'simple-array', nullable: true }) capabilities: string[];
  @Column({ type: 'int', default: 0 }) downloadCount: number;
  @Column({ type: 'decimal', precision: 3, scale: 2, default: 0 }) rating: number;
  @Column({ type: 'json', nullable: true }) metadata: Record<string, any>;
  @Column({ type: 'varchar', length: 20, default: 'active' }) status: string;
  @CreateDateColumn() createdAt: Date;
  @UpdateDateColumn() updatedAt: Date;
}
