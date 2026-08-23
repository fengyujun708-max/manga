import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, UpdateDateColumn, ManyToOne, JoinColumn, Index } from 'typeorm';
import { OfficialSeries } from './series.entity';

@Entity('official_episodes')
@Index(['seriesId', 'epNumber'], { unique: true })
export class OfficialEpisode {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  seriesId: string;

  @ManyToOne(() => OfficialSeries, series => series.episodes)
  @JoinColumn({ name: 'seriesId' })
  series: OfficialSeries;

  @Column({ type: 'int' })
  epNumber: number;

  @Column({ type: 'varchar', length: 512, nullable: true })
  title: string;

  @Column({ type: 'varchar', length: 512, nullable: true })
  subTitle: string;

  @Column({ type: 'timestamptz', nullable: true })
  publishedAt: Date;

  @Column({ type: 'varchar', length: 16, default: 'AVAILABLE' })
  availability: string;

  @Column({ type: 'text', nullable: true })
  externalUrl: string;

  @Column({ type: 'text', nullable: true })
  assetUrl: string;

  @Column({ type: 'varchar', length: 16, default: 'image' })
  assetType: string;

  @Column({ type: 'jsonb', default: {} })
  metadata: Record<string, unknown>;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}