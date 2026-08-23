import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, UpdateDateColumn, ManyToOne, JoinColumn, Index } from 'typeorm';
import { OfficialSeries } from './series.entity';

@Entity('official_episodes')
@Index(['seriesId', 'epNumber'], { unique: true })
export class OfficialEpisode {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ name: 'series_id' })
  seriesId: string;

  @ManyToOne(() => OfficialSeries, series => series.episodes)
  @JoinColumn({ name: 'series_id' })
  series: OfficialSeries;

  @Column({ name: 'ep_number' })
  epNumber: number;

  @Column({ name: 'title', nullable: true })
  title: string;

  @Column({ name: 'sub_title', nullable: true })
  subTitle: string;

  @Column({ name: 'published_at', type: 'timestamptz', nullable: true })
  publishedAt: Date;

  @Column({ name: 'availability', default: 'AVAILABLE' })
  availability: string;

  @Column({ name: 'external_url', nullable: true })
  externalUrl: string;

  @Column({ name: 'asset_url', nullable: true })
  assetUrl: string;

  @Column({ name: 'asset_type', default: 'image' })
  assetType: string;

  @Column({ name: 'metadata', type: 'jsonb', default: '{}' })
  metadata: Record<string, unknown>;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}