/**
 * 图片压缩存储（GitHub 主流方案：sharp/libvips，Next.js 同源）
 * - WebP 有损 q82 保持原始尺寸（漫画需要放大看细节，不降尺寸）
 * - 封面缩略图限定宽度（宽 > 480 缩到 480）
 * - 磁盘配额：总量超限后按 LRU 删除最久未访问的章节目录
 * - 目录结构：{IMAGE_DIR}/webtoon/{seriesKey}/{episodeNo}/{seq}.webp
 */
import { Injectable, Logger } from '@nestjs/common';
import { promises as fs } from 'fs';
import * as path from 'path';
import sharp from 'sharp';

export interface StoredPage {
  seq: number;
  url: string; // 对外静态 URL
  size: number; // 字节
}

@Injectable()
export class ImageStore {
  private readonly logger = new Logger(ImageStore.name);
  private readonly root = process.env.WEBTOON_IMAGE_DIR || '/data/webtoon-images';
  private readonly publicPrefix = process.env.WEBTOON_IMAGE_URL_PREFIX || '/static/webtoon';
  private readonly quotaBytes =
    Number(process.env.WEBTOON_IMAGE_QUOTA_GB || 10) * 1024 ** 3;
  private readonly webpQuality = Number(process.env.WEBTOON_WEBP_QUALITY || 82);
  private readonly indexFile = path.join(this.root, '.access-index.json');
  private accessIndex: Record<string, number> = {}; // relDir -> lastAccessMs
  private saving = false;

  constructor() {
    this.init().catch((e) => this.logger.error(`ImageStore 初始化失败: ${e}`));
  }

  private async init() {
    await fs.mkdir(this.root, { recursive: true });
    try {
      this.accessIndex = JSON.parse(await fs.readFile(this.indexFile, 'utf8'));
    } catch {
      this.accessIndex = {};
    }
    // 启动时执行一次配额清理
    this.enforceQuota().catch(() => {});
  }

  private async persistIndex() {
    if (this.saving) return;
    this.saving = true;
    try {
      await fs.writeFile(this.indexFile, JSON.stringify(this.accessIndex));
    } finally {
      this.saving = false;
    }
  }

  /** 章节目录（相对根） */
  private relDir(seriesKey: string, episodeNo: number): string {
    return path.join(seriesKey, String(episodeNo));
  }

  /**
   * 下载并压缩章节图片
   * @param fetcher 返回原图 Buffer（由调用方注入，便于测试）
   */
  async cacheEpisode(
    seriesKey: string,
    episodeNo: number,
    originalUrls: string[],
    fetcher: (url: string) => Promise<Buffer | null>,
  ): Promise<StoredPage[]> {
    const rel = this.relDir(seriesKey, episodeNo);
    const dir = path.join(this.root, rel);
    await fs.mkdir(dir, { recursive: true });

    const pages: StoredPage[] = [];
    let seq = 1;
    for (const url of originalUrls) {
      const file = path.join(dir, `${String(seq).padStart(4, '0')}.webp`);
      const buf = await fetcher(url);
      if (!buf || buf.length === 0) {
        this.logger.warn(`跳过空图片 ${url}`);
        seq++;
        continue;
      }
      try {
        const out = await sharp(buf)
          .rotate() // 尊重 EXIF
          .webp({ quality: this.webpQuality, effort: 4 })
          .toBuffer();
        await fs.writeFile(file, out);
        pages.push({
          seq,
          url: `${this.publicPrefix}/${rel}/${String(seq).padStart(4, '0')}.webp`,
          size: out.length,
        });
      } catch (e) {
        this.logger.warn(`压缩失败 ${url}: ${(e as Error).message}`);
      }
      seq++;
    }

    // 记录访问时间
    this.accessIndex[rel] = Date.now();
    await this.persistIndex();
    await this.enforceQuota();
    return pages;
  }

  /** 压缩封面并落盘，返回静态 URL */
  async cacheCover(seriesKey: string, originalUrl: string, fetcher: (url: string) => Promise<Buffer | null>): Promise<string | null> {
    const rel = path.join('covers', seriesKey);
    const dir = path.join(this.root, rel);
    await fs.mkdir(dir, { recursive: true });
    const file = path.join(dir, 'cover.webp');
    const buf = await fetcher(originalUrl);
    if (!buf || buf.length === 0) return null;
    try {
      const out = await sharp(buf)
        .rotate()
        .resize({ width: 480, withoutEnlargement: true }) // 封面缩略
        .webp({ quality: this.webpQuality })
        .toBuffer();
      await fs.writeFile(file, out);
      this.accessIndex[rel] = Date.now();
      await this.persistIndex();
      return `${this.publicPrefix}/${rel}/cover.webp`;
    } catch (e) {
      this.logger.warn(`封面压缩失败 ${originalUrl}: ${(e as Error).message}`);
      return null;
    }
  }

  /** 获取章节已缓存页面 */
  async listCachedPages(seriesKey: string, episodeNo: number): Promise<StoredPage[]> {
    const rel = this.relDir(seriesKey, episodeNo);
    const dir = path.join(this.root, rel);
    try {
      const files = (await fs.readdir(dir)).filter((f) => f.endsWith('.webp')).sort();
      this.accessIndex[rel] = Date.now();
      this.persistIndex();
      return files.map((f, i) => ({
        seq: i + 1,
        url: `${this.publicPrefix}/${rel}/${f}`,
        size: 0,
      }));
    } catch {
      return [];
    }
  }

  /** 配额控制：超限按 LRU 删目录 */
  private async enforceQuota() {
    try {
      const dirs: { rel: string; size: number; last: number }[] = [];
      const rels = Object.keys(this.accessIndex);
      const children = await fs.readdir(this.root, { withFileTypes: true });
      for (const ent of children) {
        if (!ent.isDirectory() || ent.name === 'covers') continue;
        const size = await this.dirSize(path.join(this.root, ent.name));
        dirs.push({ rel: ent.name, size, last: this.accessIndex[ent.name] || 0 });
      }
      let total = dirs.reduce((s, d) => s + d.size, 0);
      if (total <= this.quotaBytes) return;
      dirs.sort((a, b) => a.last - b.last); // 最久未访问在前
      for (const d of dirs) {
        if (total <= this.quotaBytes) break;
        await fs.rm(path.join(this.root, d.rel), { recursive: true, force: true });
        delete this.accessIndex[d.rel];
        total -= d.size;
        this.logger.warn(`配额清理: 删除 ${d.rel} (-${(d.size / 1024 / 1024).toFixed(1)}MB)`);
      }
      await this.persistIndex();
    } catch (e) {
      this.logger.error(`配额清理失败: ${(e as Error).message}`);
    }
  }

  private async dirSize(dir: string): Promise<number> {
    let total = 0;
    try {
      const ents = await fs.readdir(dir, { withFileTypes: true });
      for (const ent of ents) {
        const p = path.join(dir, ent.name);
        if (ent.isDirectory()) total += await this.dirSize(p);
        else total += (await fs.stat(p)).size;
      }
    } catch {}
    return total;
  }

  get rootDir(): string {
    return this.root;
  }
}