import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, UpdateDateColumn,
  OneToMany, ManyToOne, JoinColumn, Index,
} from 'typeorm';

@Entity('comics')
export class Comic {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'varchar', length: 255 })
  title: string;

  @Column({ type: 'varchar', length: 255, nullable: true })
  altTitle: string;

  @Column({ type: 'text', nullable: true })
  description: string;

  @Column({ type: 'varchar', length: 500, nullable: true })
  coverUrl: string;

  @Column({ type: 'varchar', length: 255, nullable: true })
  author: string;

  @Column({ type: 'varchar', length: 255, nullable: true })
  artist: string;

  @Column({ type: 'simple-array', nullable: true })
  tags: string[];

  @Column({ type: 'varchar', length: 50, nullable: true })
  status: string; // 'ongoing' | 'completed' | 'hiatus' | 'cancelled'

  @Column({ type: 'int', default: 0 })
  chapterCount: number;

  @Column({ type: 'float', default: 0 })
  rating: number;

  @Column({ type: 'int', default: 0 })
  views: number;

  @Column({ type: 'int', default: 0 })
  favoritesCount: number;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}

@Entity('comic_sources')
@Index(['comicId', 'sourceId'], { unique: true })
export class ComicSource {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  comicId: string;

  @Column({ type: 'varchar', length: 100 })
  sourceId: string;

  @Column({ type: 'varchar', length: 500 })
  sourceUrl: string;

  @Column({ type: 'varchar', length: 255, nullable: true })
  sourceComicId: string;

  @Column({ type: 'boolean', default: true })
  isActive: boolean;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}

@Entity('chapters')
export class Chapter {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  comicId: string;

  @Column({ type: 'varchar', length: 100 })
  sourceId: string;

  @Column({ type: 'varchar', length: 255 })
  title: string;

  @Column({ type: 'float', nullable: true })
  chapterNumber: number;

  @Column({ type: 'int', nullable: true })
  pageCount: number;

  @Column({ type: 'varchar', length: 500, nullable: true })
  sourceUrl: string;

  @Column({ type: 'boolean', default: false })
  isDownloaded: boolean;

  @Column({ type: 'timestamp', nullable: true })
  sourceUpdatedAt: Date;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}

@Entity('reading_history')
@Index(['userId', 'comicId'], { unique: true })
export class ReadingHistory {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  userId: string;

  @Column({ type: 'uuid' })
  comicId: string;

  @Column({ type: 'uuid', nullable: true })
  chapterId: string;

  @Column({ type: 'int', default: 0 })
  page: number;

  @Column({ type: 'float', default: 0 })
  progress: number;

  @Column({ type: 'timestamp' })
  lastReadAt: Date;

  @Column({ type: 'int', default: 0 })
  totalReadTime: number; // seconds

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}

@Entity('favorites')
@Index(['userId', 'comicId', 'folderId'], { unique: true })
export class Favorite {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  userId: string;

  @Column({ type: 'uuid' })
  comicId: string;

  @Column({ type: 'uuid', nullable: true })
  folderId: string;

  @CreateDateColumn()
  createdAt: Date;
}

@Entity('favorite_folders')
export class FavoriteFolder {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  userId: string;

  @Column({ type: 'varchar', length: 100 })
  name: string;

  @Column({ type: 'int', default: 0 })
  sortOrder: number;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}