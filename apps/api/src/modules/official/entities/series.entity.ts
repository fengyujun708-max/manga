import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, UpdateDateColumn, ManyToOne, JoinColumn, Index, OneToMany } from 'typeorm';
import { OfficialChannel } from './channel.entity';
import { OfficialEpisode } from './episode.entity';

@Entity('official_series')
@Index(['channelId', 'providerId'], { unique: true })
export class OfficialSeries {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ name: 'channel_id' })
  channelId: string;

  @ManyToOne(() => OfficialChannel, channel => channel.series)
  @JoinColumn({ name: 'channel_id' })
  channel: OfficialChannel;

  @Column({ name: 'provider_id' })
  providerId: string;

  @Column({ name: 'title' })
  title: string;

  @Column({ name: 'alt_title', nullable: true })
  altTitle: string;

  @Column({ name: 'author', nullable: true })
  author: string;

  @Column({ name: 'artist', nullable: true })
  artist: string;

  @Column({ name: 'description', nullable: true })
  description: string;

  @Column({ name: 'cover_url', nullable: true })
  coverUrl: string;

  @Column({ name: 'genres', type: 'simple-array', nullable: true })
  genres: string[];

  @Column({ name: 'status', default: 'ONGOING' })
  status: string;

  @Column({ name: 'provider_url', nullable: true })
  providerUrl: string;

  @Column({ name: 'language', default: 'zh' })
  language: string;

  @Column({ name: 'mature', default: false })
  mature: boolean;

  @OneToMany(() => OfficialEpisode, episode => episode.series)
  episodes: OfficialEpisode[];

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}