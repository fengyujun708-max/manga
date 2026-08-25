import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, UpdateDateColumn, Index } from 'typeorm';

@Entity('source_registry')
@Index(['sourceId'])
@Index(['status'])
@Index(['downloadCount'])
export class SourceRegistry {
  @PrimaryGeneratedColumn('uuid')
  uuid: string;

  @Column({ unique: true, name: 'sourceId' })
  sourceId: string;

  @Column({ name: 'name' })
  name: string;

  @Column({ name: 'version' })
  version: string;

  @Column({ name: 'author', nullable: true })
  author: string;

  @Column({ name: 'description', nullable: true })
  description: string;

  @Column({ name: 'icon', nullable: true })
  icon: string;

  @Column({ name: 'downloadUrl' })
  downloadUrl: string;

  @Column({ name: 'sha256', nullable: true })
  sha256: string;

  @Column({ name: 'minAppVersion', nullable: true })
  minAppVersion: string;

  @Column({ name: 'capabilities', type: 'text', nullable: true })
  capabilities: string;

  @Column({ name: 'downloadCount', default: 0 })
  downloadCount: number;

  @Column({ name: 'rating', type: 'numeric', precision: 3, scale: 2, default: 0 })
  rating: number;

  @Column({ name: 'metadata', type: 'json', nullable: true })
  metadata: Record<string, any>;

  @Column({ name: 'status', default: 'active' })
  status: string;

  @CreateDateColumn({ name: 'createdAt' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updatedAt' })
  updatedAt: Date;
}
