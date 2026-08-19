import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, UpdateDateColumn,
  ManyToOne, JoinColumn, Index,
} from 'typeorm';

// ====== 社区系统 ======

@Entity('posts')
export class Post {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  userId: string;

  @Column({ type: 'varchar', length: 200 })
  title: string;

  @Column({ type: 'text' })
  content: string;

  @Column({ type: 'simple-array', nullable: true })
  tags: string[];

  @Column({ type: 'int', default: 0 })
  likesCount: number;

  @Column({ type: 'int', default: 0 })
  commentsCount: number;

  @Column({ type: 'int', default: 0 })
  favoritesCount: number;

  @Column({ type: 'int', default: 0 })
  views: number;

  @Column({ type: 'varchar', length: 50, default: 'normal' })
  type: string; // 'normal' | 'question' | 'request' | 'review' | 'share'

  @Column({ type: 'varchar', length: 20, default: 'published' })
  status: string; // 'published' | 'draft' | 'hidden' | 'deleted'

  @Column({ type: 'boolean', default: false })
  isPinned: boolean;

  @Column({ type: 'boolean', default: false })
  isLocked: boolean;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}

@Entity('post_comics_refs')
export class PostComicRef {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  postId: string;

  @Column({ type: 'varchar', length: 100 })
  sourceId: string;

  @Column({ type: 'varchar', length: 255 })
  comicId: string;

  @Column({ type: 'varchar', length: 255, nullable: true })
  comicTitle: string;

  @Column({ type: 'varchar', length: 500, nullable: true })
  comicCover: string;

  @Column({ type: 'varchar', length: 255, nullable: true })
  chapterId: string;

  @Column({ type: 'varchar', length: 255, nullable: true })
  chapterTitle: string;

  @Column({ type: 'int', default: 0 })
  sortOrder: number;

  @CreateDateColumn()
  createdAt: Date;
}

@Entity('post_comments')
export class PostComment {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  postId: string;

  @Column({ type: 'uuid' })
  userId: string;

  @Column({ type: 'uuid', nullable: true })
  parentId: string;

  @Column({ type: 'uuid', nullable: true })
  replyToUserId: string;

  @Column({ type: 'text' })
  content: string;

  @Column({ type: 'int', default: 0 })
  likesCount: number;

  @Column({ type: 'int', default: 0 })
  repliesCount: number;

  @Column({ type: 'varchar', length: 20, default: 'published' })
  status: string;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}

@Entity('post_likes')
@Index(['userId', 'postId'], { unique: true })
export class PostLike {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  userId: string;

  @Column({ type: 'uuid' })
  postId: string;

  @Column({ type: 'uuid', nullable: true })
  commentId: string;

  @CreateDateColumn()
  createdAt: Date;
}

// ====== 求漫/求源系统 ======

@Entity('manga_requests')
export class MangaRequest {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  userId: string;

  @Column({ type: 'varchar', length: 200 })
  mangaName: string;

  @Column({ type: 'varchar', length: 200, nullable: true })
  altName: string;

  @Column({ type: 'varchar', length: 200, nullable: true })
  author: string;

  @Column({ type: 'text', nullable: true })
  description: string;

  @Column({ type: 'varchar', length: 500, nullable: true })
  imageUrl: string;

  @Column({ type: 'text', nullable: true })
  notes: string;

  @Column({ type: 'varchar', length: 20, default: 'pending' })
  status: string; // 'pending' | 'searching' | 'found' | 'not_found' | 'closed'

  @Column({ type: 'uuid', nullable: true })
  resolvedComicId: string;

  @Column({ type: 'int', default: 0 })
  supportCount: number;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}

@Entity('source_requests')
export class SourceRequest {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  userId: string;

  @Column({ type: 'varchar', length: 200 })
  sourceName: string;

  @Column({ type: 'varchar', length: 500, nullable: true })
  sourceUrl: string;

  @Column({ type: 'text', nullable: true })
  description: string;

  @Column({ type: 'text', nullable: true })
  notes: string;

  @Column({ type: 'varchar', length: 20, default: 'pending' })
  status: string; // 'pending' | 'testing' | 'approved' | 'rejected' | 'online'

  @Column({ type: 'text', nullable: true })
  testResult: string;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}

// ====== 源注册表 ======

@Entity('source_registry')
export class Notification {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  userId: string;

  @Column({ type: 'varchar', length: 50 })
  type: string; // 'system' | 'comic_update' | 'comment_reply' | 'like' | 'mention' | 'request_reply'

  @Column({ type: 'varchar', length: 200 })
  title: string;

  @Column({ type: 'text', nullable: true })
  body: string;

  @Column({ type: 'json', nullable: true })
  data: Record<string, any>;

  @Column({ type: 'boolean', default: false })
  isRead: boolean;

  @CreateDateColumn()
  createdAt: Date;
}

// ====== 公告 ======

@Entity('announcements')
export class Announcement {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'varchar', length: 200 })
  title: string;

  @Column({ type: 'text' })
  content: string;

  @Column({ type: 'varchar', length: 50, default: 'normal' })
  priority: string; // 'normal' | 'important' | 'maintenance' | 'version'

  @Column({ type: 'varchar', length: 500, nullable: true })
  coverUrl: string;

  @Column({ type: 'boolean', default: false })
  isForceRead: boolean;

  @Column({ type: 'timestamp', nullable: true })
  startAt: Date;

  @Column({ type: 'timestamp', nullable: true })
  endAt: Date;

  @Column({ type: 'boolean', default: true })
  isActive: boolean;

  @Column({ type: 'varchar', length: 50, nullable: true })
  targetVersion: string;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}

// ====== App 版本管理 ======

@Entity('app_versions')
export class AppVersion {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'varchar', length: 20 })
  version: string;

  @Column({ type: 'int' })
  buildNumber: number;

  @Column({ type: 'varchar', length: 50 })
  platform: string; // 'android' | 'ios' | 'windows' | 'macos' | 'linux'

  @Column({ type: 'varchar', length: 500 })
  downloadUrl: string;

  @Column({ type: 'text', nullable: true })
  changelog: string;

  @Column({ type: 'boolean', default: false })
  isForceUpdate: boolean;

  @Column({ type: 'varchar', length: 20, nullable: true })
  minVersion: string;

  @Column({ type: 'boolean', default: true })
  isActive: boolean;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}

// ====== 管理/审计 ======

@Entity('reports')
export class Report {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  reporterId: string;

  @Column({ type: 'varchar', length: 50 })
  targetType: string; // 'post' | 'comment' | 'user' | 'source'

  @Column({ type: 'uuid' })
  targetId: string;

  @Column({ type: 'varchar', length: 50 })
  reason: string;

  @Column({ type: 'text', nullable: true })
  description: string;

  @Column({ type: 'varchar', length: 20, default: 'pending' })
  status: string; // 'pending' | 'resolved' | 'dismissed'

  @Column({ type: 'uuid', nullable: true })
  handledBy: string;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}

@Entity('bans')
export class Ban {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  userId: string;

  @Column({ type: 'uuid' })
  handledBy: string;

  @Column({ type: 'varchar', length: 50 })
  reason: string;

  @Column({ type: 'text', nullable: true })
  description: string;

  @Column({ type: 'timestamp' })
  startAt: Date;

  @Column({ type: 'timestamp', nullable: true })
  endAt: Date;

  @Column({ type: 'boolean', default: false })
  isPermanent: boolean;

  @Column({ type: 'boolean', default: false })
  isActive: boolean;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}

@Entity('audit_logs')
export class AuditLog {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid', nullable: true })
  userId: string;

  @Column({ type: 'varchar', length: 100 })
  action: string;

  @Column({ type: 'varchar', length: 50 })
  resourceType: string;

  @Column({ type: 'uuid', nullable: true })
  resourceId: string;

  @Column({ type: 'json', nullable: true })
  details: Record<string, any>;

  @Column({ type: 'varchar', length: 45, nullable: true })
  ip: string;

  @CreateDateColumn()
  createdAt: Date;
}

// ====== Remote Config ======

@Entity('remote_configs')
export class RemoteConfig {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'varchar', length: 100, unique: true })
  key: string;

  @Column({ type: 'json' })
  value: any;

  @Column({ type: 'text', nullable: true })
  description: string;

  @Column({ type: 'boolean', default: true })
  isActive: boolean;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}