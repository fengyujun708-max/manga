/**
 * Webtoon HTML 解析器（纯函数，无副作用）
 * 数据来源：www.webtoons.com 公开网页（免费内容）
 * - 排行页（CDN，已验证）：标题卡片 → titleNo/标题/封面/分类
 * - 详情页（结构多级容错，部分结构待真机验证）：系列信息 + 章节列表
 */
export interface WtCard {
  titleNo: number;
  rank?: number;
  title: string;
  cover: string;
  genre?: string;
  author?: string;
}

export interface WtEpisodeMeta {
  episodeNo: number;
  title: string;
  publishedAt?: string;
  status: 'FREE' | 'PAID' | 'PASS' | 'UNKNOWN';
}

export interface WtDetail {
  titleNo: number;
  title: string;
  author?: string;
  description?: string;
  genre?: string;
  cover: string;
  status?: string;
  episodes: WtEpisodeMeta[];
}

/** 排行/首页卡片解析：data-title-no + img.alt + .title / .genre */
export function parseRankingCards(html: string): WtCard[] {
  const cards: WtCard[] = [];
  // 按卡片块切分
  const blockRe = /data-title-no="(\d+)"([\s\S]*?)<\/li>/g;
  let m: RegExpExecArray | null;
  while ((m = blockRe.exec(html)) !== null) {
    const titleNo = Number(m[1]);
    const block = m[2];
    const img = block.match(/<img[^>]*src="([^"]+)"[^>]*alt="([^"]*)"/);
    if (!img) continue;
    const titleM = block.match(/<strong class="title">([^<]*)<\/strong>/);
    const genreM = block.match(/<div class="genre">([^<]*)<\/div>/);
    const rankM = block.match(/data-rank="(\d+)"/);
    cards.push({
      titleNo,
      title: (titleM?.[1] || img[2]).trim(),
      cover: img[1],
      genre: genreM?.[1]?.trim(),
      rank: rankM ? Number(rankM[1]) : undefined,
    });
  }
  return cards;
}

/** 从任意 HTML 提取 __NEXT_DATA__ JSON */
export function extractNextData(html: string): any | null {
  const m = html.match(/<script id="__NEXT_DATA__" type="application\/json">([\s\S]*?)<\/script>/);
  if (!m) return null;
  try {
    return JSON.parse(m[1]);
  } catch {
    return null;
  }
}

/**
 * 详情页解析（多级容错）：
 * 1) __NEXT_DATA__（新版 React）
 * 2) 旧版 .detail_header + #_listUl
 */
export function parseDetail(html: string, titleNo: number): WtDetail | null {
  // 通道一：__NEXT_DATA__
  const nd = extractNextData(html);
  if (nd?.props?.pageProps) {
    const pp = nd.props.pageProps;
    const series =
      pp.series || pp.seriesData || (pp.initialData?.series) || null;
    if (series) {
      const eps: WtEpisodeMeta[] = (Array.isArray(series.episodeList)
        ? series.episodeList
        : (pp.episodeList as any[]) || []
      ).map((e: any) => ({
        episodeNo: Number(e.episodeNo || e.no || 0),
        title: e.title || e.subject || '',
        publishedAt: e.publishedAt || e.publishedDate || e.regDate,
        status: (e.status || e.lockType || 'UNKNOWN').toString().toUpperCase(),
      })).filter((e: WtEpisodeMeta) => e.episodeNo > 0);
      return {
        titleNo,
        title: series.title || series.titleName || '',
        author: series.author || series.artist,
        description: series.synopsis || series.description,
        genre: Array.isArray(series.genreList) ? series.genreList.join(', ') : series.genre,
        cover: series.thumbnail || series.thumbnailUrl || '',
        status: series.status || 'ONGOING',
        episodes: eps,
      };
    }
  }

  // 通道二：旧版 HTML 结构
  const titleM = html.match(/<p class="title">([\s\S]*?)<\/p>/);
  const authorM = html.match(/<span class="author">([\s\S]*?)<\/span>/);
  const summaryM = html.match(/<p class="summary">([\s\S]*?)<\/p>/);
  const genreM = html.match(/<span class="genre">([\s\S]*?)<\/span>/);
  const coverM = html.match(/<meta property="og:image" content="([^"]+)"/)
    || html.match(/<img[^>]*class="[^"]*detail_logo[^"]*"[^>]*src="([^"]+)"/);

  const eps: WtEpisodeMeta[] = [];
  const liRe = /<li[^>]*data-episode-no="(\d+)"([\s\S]*?)<\/li>/g;
  let em: RegExpExecArray | null;
  while ((em = liRe.exec(html)) !== null) {
    const block = em[2];
    const subjM = block.match(/<span class="subj">[\s\S]*?<span>([\s\S]*?)<\/span>/);
    const dateM = block.match(/<span class="date">([\s\S]*?)<\/span>/);
    const isFree = !block.includes('ico_') || block.includes('ico_open');
    eps.push({
      episodeNo: Number(em[1]),
      title: subjM?.[1]?.trim() || '',
      publishedAt: dateM?.[1]?.trim(),
      status: isFree ? 'FREE' : 'UNKNOWN',
    });
  }

  const title = titleM?.[1]?.replace(/<[^>]+>/g, '').trim() || '';
  if (!title) return null;
  return {
    titleNo,
    title,
    author: authorM?.[1]?.replace(/<[^>]+>/g, '').trim(),
    description: summaryM?.[1]?.replace(/<[^>]+>/g, '').trim(),
    genre: genreM?.[1]?.replace(/<[^>]+>/g, '').trim(),
    cover: coverM?.[1] || '',
    episodes: eps,
  };
}

/**
 * 章节阅读页图片解析（多级容错）：
 * 1) __NEXT_DATA__ (imageList)
 * 2) #_imageList 内 img data-src / src
 */
export function parseEpisodeImages(html: string): string[] {
  const urls = new Set<string>();

  const nd = extractNextData(html);
  if (nd?.props?.pageProps) {
    const pp = nd.props.pageProps;
    const list =
      pp.imageList ||
      pp.images ||
      (Array.isArray(pp.episode) ? pp.episode.map((x: any) => x.imageUrl || x) : null);
    if (Array.isArray(list)) {
      for (const u of list) {
        const s = typeof u === 'string' ? u : u?.imageUrl || u?.url;
        if (typeof s === 'string' && s.startsWith('http')) urls.add(s);
      }
    }
  }

  if (urls.size === 0) {
    const imgRe = /<img[^>]+(?:data-src|src)="(https:\/\/[^"]+)"[^>]*>/g;
    let m: RegExpExecArray | null;
    while ((m = imgRe.exec(html)) !== null) {
      if (m[1].includes('webtoon-phinf')) urls.add(m[1]);
    }
  }

  return [...urls];
}