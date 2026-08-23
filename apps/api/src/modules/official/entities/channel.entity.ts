import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, UpdateDateColumn, OneToMany } from 'typeorm';
import { OfficialSeries } from './series.entity';

@Entity('official_channels')
export class OfficialChannel {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ name: 'slug' })
  slug: string;

  @Column({ name: 'display_name' })
  displayName: string;

  @Column({ name: 'description', nullable: true })
  description: string;

  @Column({ name: 'logo_url', nullable: true })
  logoUrl: string;

  @Column({ name: 'provider' })
  provider: string;

  @Column({ name: 'status', default: 'ACTIVE' })
  status: string;

  @Column({ name: 'priority', default: 0 })
  priority: number;

  @OneToMany(() => OfficialSeries, series => series.channel)
  series: OfficialSeries[];

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}