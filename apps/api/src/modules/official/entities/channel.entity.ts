import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, UpdateDateColumn, OneToMany } from 'typeorm';
import { OfficialSeries } from './series.entity';

@Entity('official_channels')
export class OfficialChannel {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'varchar', length: 64, unique: true })
  slug: string;

  @Column({ type: 'varchar', length: 128 })
  displayName: string;

  @Column({ type: 'text', nullable: true })
  description: string;

  @Column({ type: 'text', nullable: true })
  logoUrl: string;

  @Column({ type: 'varchar', length: 64 })
  provider: string;

  @Column({ type: 'varchar', length: 16, default: 'ACTIVE' })
  status: string;

  @Column({ type: 'int', default: 0 })
  priority: number;

  @OneToMany(() => OfficialSeries, series => series.channel)
  series: OfficialSeries[];

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}