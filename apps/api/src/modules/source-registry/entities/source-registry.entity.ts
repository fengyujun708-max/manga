import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, UpdateDateColumn, Index } from 'typeorm';

@Entity('source_registry')
@Index(['enabled', 'id'])
export class SourceRegistry {
  @PrimaryGeneratedColumn('uuid')
  uuid: string;

  @Column({ unique: true })
  id: string;

  @Column({ name: 'display_name' })
  displayName: string;

  @Column({ name: 'description', type: 'text', nullable: true })
  description: string;

  @Column({ nullable: true })
  author: string;

  @Column({ name: 'repository_url', type: 'text', nullable: true })
  repositoryUrl: string;

  @Column({ name: 'version', length: '64' })
  version: string;

  @Column({ default: true })
  enabled: boolean;

  @Column({ default: false })
  vetted: boolean;

  @Column({ name: 'min_app_version', length: '64', nullable: true })
  minAppVersion: string;

  @Column({ name: 'download_url', type: 'text', nullable: true })
  downloadUrl: string;

  @Column({ name: 'icon_url', type: 'text', nullable: true })
  iconUrl: string;

  @Column({ name: 'metadata', type: 'jsonb', nullable: true })
  metadata: Record<string, any>;

  @Column({ name: 'tags', type: 'text', nullable: true })
  tags: string;

  @Column({ name: 'capabilities', type: 'text', nullable: true })
  capabilities: string;

  @Column({ name: 'network_type', length: '32', default: 'HTTP' })
  networkType: string;

  @Column({ name: 'health_status', length: '32', default: 'UNKNOWN' })
  healthStatus: string;

  @Column({ name: 'health_checked_at', type: 'timestamp', nullable: true })
  healthCheckedAt: Date;

  @Column({ name: 'health_error', type: 'text', nullable: true })
  healthError: string;

  @Column({ name: 'downloads', default: 0 })
  downloads: number;

  @Column({ name: 'rating', type: 'decimal', precision: 3, scale: 2, default: 0 })
  rating: number;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}
