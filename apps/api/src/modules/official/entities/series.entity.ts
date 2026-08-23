import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, UpdateDateColumn, ManyToOne, JoinColumn, Index, OneToMany } from 'typeorm';
import { OfficialChannel } from './channel.entity';
import { OfficialEpisode } from './episode.entity';

@Entity('official_series')
@Index(['channelId', 'providerId'], { unique: true })
export class OfficialSeries {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  channelId: string;

  @ManyToOne(() => OfficialChannel, channel => channel.series)
  @JoinColumn({ name: 'channelId' })
  channel: OfficialChannel;

  @Column({ type: 'varchar', length: 128 })
  providerId: string;

  @Column({ type: 'varchar', length: 512 })
  title: string;

  @Column({ type: 'varchar', length: 512, nullable: true })
  altTitle: string;

  @Column({ type: 'varchar', length: 256, nullable: true })
  author: string;

  @Column({ type: 'varchar', length: 256, nullable: true })
  artist: string;

  @Column({ type: 'text', nullable: true })
  description: string;

  @Column({ type: 'text', nullable: true })
  coverUrl: string;

  @Column({ type: 'simple-array', nullable: true })
  genres: string[];

  @Column({ type: 'varchar', length: 16, default: 'ONGOING' })
  status: string;

  @Column({ type: 'text', nullable: true })
  providerUrl: string;

  @Column({ type: 'varchar', length: 8, default: 'zh' })
  language: string;

  @Column({ type: 'boolean', default: false })
  mature: boolean;

  @OneToMany(() => OfficialEpisode, episode => episode.series)
  episodes: OfficialEpisode[];

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}