import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, UpdateDateColumn, Index } from 'typeorm';

@Entity('source_registry')
@Index(['sourceId'])
@Index(['status'])
@Index(['downloadCount'])
export class SourceRegistry {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ unique: true, name: 'sourceId' })
  sourceId: string;

  @Column({ name: 'name' })
  name: string;

  @Column({ name: 'version' })
  version: string;

  @Column({ nullable: true })
  author: string;

  @Column({ nullable: true })
  description: string;

  @Column({ name: 'icon', nullable: true })
  icon: string;

  @Column({ name: 'downloadUrl' })
  downloadUrl: string;

  @Column({ nullable: true })
  sha256: string;

  @Column({ nullable: true })
  minAppVersion: string;

  @Column({ type: 'text', nullable: true })
  capabilities: string;

  @Column({ name: 'downloadCount', default: 0 })
  downloadCount: number;

  @Column({ type: 'numeric', precision: 3, scale: 2, default: 0 })
  rating: number;

  @Column({ type: 'json', nullable: true })
  metadata: Record<string, any>;

  @Column({ default: 'active' })
  status: string;

  @CreateDateColumn({ name: 'createdAt' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updatedAt' })
  updatedAt: Date;
}
