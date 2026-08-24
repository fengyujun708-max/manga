import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  Index,
} from 'typeorm';

@Entity('client_logs')
@Index(['createdAt'])
export class ClientLog {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'varchar', length: 36, nullable: true })
  userId: string;

  @Column({ type: 'varchar', length: 30 })
  level: string; // error | warning | info

  @Column({ type: 'varchar', length: 200 })
  title: string;

  @Column({ type: 'text', nullable: true })
  content: string;

  @Column({ type: 'varchar', length: 50, nullable: true })
  deviceModel: string;

  @Column({ type: 'varchar', length: 30, nullable: true })
  appVersion: string;

  @Column({ type: 'varchar', length: 30, nullable: true })
  platform: string;

  @CreateDateColumn()
  createdAt: Date;
}