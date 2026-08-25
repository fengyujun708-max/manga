/**
 * Webtoon HTTP 客户端：抓取公开网页（免费内容）
 * - 浏览器 UA + Referer + cookie jar（规避边缘网关）
 * - 代理可配置（WEBTOON_HTTP_PROXY），沙箱/服务器网络差异可解
 * - 重试 + 超时 + 限速（礼貌抓取）
 */
import { Injectable, Logger } from '@nestjs/common';
import axios, { AxiosInstance } from 'axios';
import { HttpsProxyAgent } from 'https-proxy-agent';

export interface FetchOptions {
  genre?: string; // 榜单分类，如 'romance' / '' = 首页排行
  page?: number;
}

@Injectable()
export class WebtoonClient {
  private readonly logger = new Logger(WebtoonClient.name);
  private readonly http: AxiosInstance;
  private readonly base = 'https://www.webtoons.com';
  private lastFetch = 0;
  private readonly minIntervalMs = Number(process.env.WEBTOON_FETCH_INTERVAL_MS || 1500);

  constructor() {
    const proxy = process.env.WEBTOON_HTTP_PROXY;
    this.http = axios.create({
      timeout: Number(process.env.WEBTOON_TIMEOUT_MS || 25000),
      headers: {
        'User-Agent':
          'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8',
        'Accept-Language': 'en-US,en;q=0.9',
        'Referer': this.base + '/en/',
        'sec-ch-ua': '"Not_A Brand";v="8", "Chromium";v="120"',
        'sec-ch-ua-mobile': '?0',
        'Sec-Fetch-Dest': 'document',
        'Sec-Fetch-Mode': 'navigate',
        'Sec-Fetch-Site': 'same-origin',
      },
      ...(proxy ? { httpsAgent: new HttpsProxyAgent(proxy) } : {}),
    });
  }

  private async throttle() {
    const wait = this.minIntervalMs - (Date.now() - this.lastFetch);
    if (wait > 0) await new Promise((r) => setTimeout(r, wait));
    this.lastFetch = Date.now();
  }

  /** 排行页（CDN 直连已验证可用）*/
  async fetchRanking(options: FetchOptions = {}): Promise<string | null> {
    const url =
      options.genre && options.genre !== 'all'
        ? `${this.base}/en/genres/${options.genre}`
        : `${this.base}/en/ranking`;
    return this.get(url);
  }

  /** 详情页（部分网络环境下会被网关 500，调用方需容错）*/
  async fetchDetail(titleNo: number): Promise<string | null> {
    return this.get(`${this.base}/en/detail/${titleNo}?title_no=${titleNo}`);
  }

  /** 章节阅读页（含图片 URL 列表）*/
  async fetchEpisode(titleNo: number, episodeNo: number): Promise<string | null> {
    return this.get(`${this.base}/en/episode/${titleNo}/${episodeNo}`);
  }

  /** 图片原图（phinf CDN，带 Referer）*/
  async fetchImage(url: string): Promise<Buffer | null> {
    try {
      await this.throttle();
      const res = await this.http.get(url, {
        responseType: 'arraybuffer',
        headers: { Referer: `${this.base}/`, Accept: 'image/avif,image/webp,image/jpeg,*/*' },
        timeout: 30000,
      });
      if (res.status === 200 && Buffer.isBuffer(res.data)) return res.data;
      return null;
    } catch (e) {
      this.logger.warn(`fetchImage 失败: ${url} ${(e as Error).message}`);
      return null;
    }
  }

  private async get(url: string, retries = 2): Promise<string | null> {
    for (let i = 0; i <= retries; i++) {
      try {
        await this.throttle();
        const res = await this.http.get(url);
        if (res.status === 200 && typeof res.data === 'string') return res.data;
      } catch (e) {
        const msg = (e as Error).message;
        if (i < retries) {
          this.logger.warn(`重试(${i + 1}/${retries}) ${url} -> ${msg}`);
          await new Promise((r) => setTimeout(r, 2000 * (i + 1)));
        } else {
          this.logger.warn(`抓取失败 ${url} -> ${msg}`);
        }
      }
    }
    return null;
  }
}