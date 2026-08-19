# 漫界 — Venera 源适配器设计

> 目标：兼容 Venera 生态的 JavaScript 漫画源，让漫界可以直接使用 Venera 源
> 许可证：Venera 源本身为 GPL-3.0，适配器层为漫界自有代码（MPL-2.0）

---

## 1. 架构设计

```
漫界 App
    │
    ├── MangaSource SDK (统一接口)
    │       │
    │       ├── BreezeSourceAdapter  ← Breeze 原生源
    │       ├── VeneraSourceAdapter  ← Venera JS 源
    │       └── NativeSource         ← 漫界自研源
    │
    └── JS Runtime (QuickJS 沙箱)
            │
            └── Venera JS Source
```

## 2. MangaSource SDK 统一接口

```typescript
interface MangaSource {
  // 元数据
  readonly id: string;
  readonly name: string;
  readonly version: string;
  readonly icon?: string;
  readonly lang?: string[];

  // 核心 API
  search(keyword: string, page?: number): Promise<MangaResult[]>;
  getDetail(mangaId: string): Promise<MangaDetail>;
  getChapters(mangaId: string): Promise<Chapter[]>;
  getPages(mangaId: string, chapterId: string): Promise<string[]>;

  // 可选 API
  getCategories?(): Promise<Category[]>;
  getCategoryList?(categoryId: string, page?: number): Promise<MangaResult[]>;
  getPopular?(page?: number): Promise<MangaResult[]>;
  getLatest?(page?: number): Promise<MangaResult[]>;

  // 登录（可选）
  isLoggedIn?(): Promise<boolean>;
  login?(credentials: any): Promise<void>;
  getFavorites?(page?: number): Promise<MangaResult[]>;
  addFavorite?(mangaId: string): Promise<void>;
  removeFavorite?(mangaId: string): Promise<void>;
}

interface MangaResult {
  id: string;
  title: string;
  cover?: string;
  author?: string;
  status?: string;
  rating?: number;
  description?: string;
  lastChapter?: string;
  lastUpdate?: string;
}

interface MangaDetail extends MangaResult {
  chapters: Chapter[];
  tags?: string[];
  artist?: string;
  description: string;
}

interface Chapter {
  id: string;
  title: string;
  number?: number;
  pageCount?: number;
  updateTime?: string;
}

interface Category {
  id: string;
  title: string;
}
```

## 3. Venera 源适配器

### 3.1 适配器类

```typescript
class VeneraSourceAdapter implements MangaSource {
  private jsEngine: QuickJSBridge;
  private sourceId: string;
  private manifest: SourceManifest;

  constructor(jsEngine: QuickJSBridge, manifest: SourceManifest) {
    this.jsEngine = jsEngine;
    this.sourceId = manifest.id;
    this.manifest = manifest;
  }

  async search(keyword: string, page: number = 1): Promise<MangaResult[]> {
    const result = await this.jsEngine.evaluate(`source.search(${JSON.stringify(keyword)}, ${page})`);
    return this.parseMangaResults(result);
  }

  async getDetail(mangaId: string): Promise<MangaDetail> {
    const result = await this.jsEngine.evaluate(`source.getDetail(${JSON.stringify(mangaId)})`);
    return this.parseMangaDetail(result);
  }

  async getChapters(mangaId: string): Promise<Chapter[]> {
    const result = await this.jsEngine.evaluate(`source.getChapters(${JSON.stringify(mangaId)})`);
    return this.parseChapters(result);
  }

  async getPages(mangaId: string, chapterId: string): Promise<string[]> {
    const result = await this.jsEngine.evaluate(`source.getPages(${JSON.stringify(mangaId)}, ${JSON.stringify(chapterId)})`);
    return result;
  }

  async getCategories(): Promise<Category[]> {
    if (this.manifest.capabilities?.includes('categories')) {
      const result = await this.jsEngine.evaluate('source.getCategories()');
      return this.parseCategories(result);
    }
    return [];
  }

  async getCategoryList(categoryId: string, page: number = 1): Promise<MangaResult[]> {
    const result = await this.jsEngine.evaluate(`source.getCategoryList(${JSON.stringify(categoryId)}, ${page})`);
    return this.parseMangaResults(result);
  }
}
```

### 3.2 Venera JS 源格式

Venera 源是一个 JavaScript 文件，导出 `source` 对象：

```javascript
// 示例：Venera 源格式
const source = {
  id: 'example_source',
  name: '示例源',
  version: '1.0.0',
  icon: 'data:image/png;base64,...',

  // 搜索
  async search(keyword, page = 1) {
    const html = await fetch(`https://example.com/search?q=${keyword}&page=${page}`);
    const doc = new DOMParser().parseFromString(html, 'text/html');
    return doc.querySelectorAll('.comic-item').map(el => ({
      id: el.dataset.id,
      title: el.querySelector('.title').textContent,
      cover: el.querySelector('img').src,
      author: el.querySelector('.author').textContent,
    }));
  },

  // 漫画详情
  async getDetail(mangaId) {
    // ...
  },

  // 章节列表
  async getChapters(mangaId) {
    // ...
  },

  // 图片列表
  async getPages(mangaId, chapterId) {
    // ...
  },
};
```

### 3.3 Venera 源注册表

```json
{
  "sources": [
    {
      "id": "example_source",
      "name": "示例源",
      "version": "1.0.0",
      "url": "https://source.manjie.xxx/sources/example.js",
      "sha256": "abc123...",
      "minAppVersion": "1.0.0",
      "capabilities": ["search", "detail", "chapters", "pages", "categories"],
      "lang": ["zh"],
      "updateTime": "2025-01-15T10:00:00Z"
    }
  ]
}
```

## 4. JS 运行时沙箱

### 4.1 安全限制

```
允许:
├── HTTP 请求 (fetch)
├── JSON 解析
├── 字符串处理
├── 正则表达式
├── HTML 解析 (DOMParser)
└── Base64 编解码

禁止:
├── 文件系统操作
├── 进程/Shell 执行
├── 数据库访问
├── 用户 Token 读取
├── 任意网络请求 (限制域名)
├── 原生 API 调用
└── 内存/CPU 占用过高
```

### 4.2 API 注入

```typescript
// 注入到 JS 沙箱的 API
const sandboxApis = {
  fetch: async (url: string, options?: any) => {
    // 限制只允许 HTTP/HTTPS
    // 限制域名白名单（可选）
    // 超时控制（10秒）
    // 返回格式：{ status, headers, body }
  },
  DOMParser: class DOMParser {
    parseFromString(html: string, mime: string) {
      // 轻量 HTML 解析
    }
  },
  atob: (str: string) => Buffer.from(str, 'base64').toString(),
  btoa: (str: string) => Buffer.from(str).toString('base64'),
  setTimeout: (fn: Function, ms: number) => {
    // 限制最大超时时间
  },
  crypto: {
    // 有限制的 crypto 子集
  },
};
```

### 4.3 超时与资源限制

```typescript
// 每个源调用超时 30 秒
// 源间隔离（不同源不同 QuickJS 上下文）
// 内存限制：每个源最大 64MB
// 请求频率限制：每分钟 60 次
```

## 5. 源同步服务

### 5.1 同步架构

```
上游源仓库 (GitHub/Venera-Configs)
    │
    ▼
Sync Worker (每 6 小时)
    │
    ├── 拉取最新源 JS
    ├── SHA256 校验
    ├── JS 语法检查 (AST)
    ├── 沙箱测试 (连接/搜索/详情/图片)
    └── 发布到 Source Registry
         │
         ▼
    CDN (source.manjie.xxx)
         │
         ▼
    漫界 App 自动更新
```

### 5.2 源测试

```typescript
async function testSource(source: MangaSource): Promise<TestResult> {
  const results = [];

  // 1. 连接测试
  try {
    const categories = await source.getCategories?.();
    results.push({ name: '连接测试', passed: true });
  } catch (e) {
    results.push({ name: '连接测试', passed: false, error: e.message });
  }

  // 2. 搜索测试
  try {
    const searchResults = await source.search('测试', 1);
    results.push({ name: '搜索测试', passed: searchResults.length > 0 });
  } catch (e) {
    results.push({ name: '搜索测试', passed: false });
  }

  // 3. 详情测试
  // 4. 章节测试
  // 5. 图片测试

  return {
    sourceId: source.id,
    version: source.version,
    passed: results.every(r => r.passed),
    results,
    testedAt: new Date().toISOString(),
  };
}
```

## 6. 漫界源注册表 API

### 6.1 接口

```typescript
// GET /v1/sources
// 返回可用源列表
Response: {
  items: SourceManifest[];
  updateTime: string;
}

// GET /v1/sources/:id
// 返回源详情 + 下载地址
Response: {
  id: string;
  name: string;
  version: string;
  downloadUrl: string;
  sha256: string;
  changelog?: string;
}

// GET /v1/sources/:id.js
// 直接返回源 JS 文件（CDN）
```

### 6.2 源注册表缓存策略

```typescript
// 客户端缓存
// 启动时检查源列表更新
// 每次启动只拉取源列表（不下载源 JS）
// 首次使用源时下载 JS 并缓存
// 源 JS 缓存 24 小时
// 后台异步检查源更新
```

## 7. 源管理（客户端）

### 7.1 源管理页面

```
源管理
├── 已安装源
│   ├── [源名称] v1.0.0 ✓
│   │   ├── 启用/禁用
│   │   ├── 检查更新
│   │   └── 卸载
│   └── ...
│
├── 源市场（从注册表获取）
│   ├── [新源名称] v1.0.0
│   │   └── 安装
│   └── ...
│
└── 手动添加
    └── 输入源 URL / 选择本地文件
```

### 7.2 源设置

```typescript
interface SourceConfig {
  id: string;
  enabled: boolean;
  autoUpdate: boolean;
  customCookies?: Record<string, string>;
  proxy?: string;
  userAgent?: string;
}
```

## 8. 兼容性说明

### 8.1 Venera 源 API 映射

| Venera API | 漫界 SDK | 状态 |
|-----------|----------|------|
| `source.id` | `id` | ✅ 直接映射 |
| `source.name` | `name` | ✅ 直接映射 |
| `source.search(keyword, page)` | `search(keyword, page)` | ✅ 直接映射 |
| `source.getDetail(mangaId)` | `getDetail(mangaId)` | ✅ 直接映射 |
| `source.getChapters(mangaId)` | `getChapters(mangaId)` | ✅ 直接映射 |
| `source.getPages(mangaId, chapterId)` | `getPages(mangaId, chapterId)` | ✅ 直接映射 |
| `source.getCategories()` | `getCategories()` | ✅ 可选 |
| `source.getCategoryList(catId, page)` | `getCategoryList(catId, page)` | ✅ 可选 |
| `source.login(credentials)` | `login(credentials)` | 🟡 需适配 |
| `source.getFavorites(page)` | `getFavorites(page)` | 🟡 需适配 |
| `source.addFavorite(mangaId)` | `addFavorite(mangaId)` | 🟡 需适配 |
| `source.removeFavorite(mangaId)` | `removeFavorite(mangaId)` | 🟡 需适配 |
| `source.getComments(mangaId)` | — | 🔴 暂不支持 |

### 8.2 已知差异

1. **`fetch` 实现**：Venera 源使用 Venera 的 `fetch`，漫界需要注入兼容的 `fetch` 实现
2. **`DOMParser`**：Venera 源依赖 HTML 解析，漫界需注入轻量 HTML 解析器
3. **`Base64`**：部分源使用 `atob`/`btoa`，需要注入
4. **`Storage`**：部分源使用 `localStorage`，需要注入内存存储

## 9. 实现计划

### Phase 1: 基础适配
- [ ] 定义 MangaSource SDK 接口（TypeScript 类型）
- [ ] 实现 QuickJS 桥接层
- [ ] 实现 VeneraSourceAdapter
- [ ] 注入沙箱 API（fetch, DOMParser, atob/btoa）

### Phase 2: 源管理
- [ ] 源注册表客户端（拉取/缓存/更新）
- [ ] 源安装/卸载/更新
- [ ] 源管理 UI

### Phase 3: 源同步
- [ ] 源同步 Worker（拉取上游/测试/发布）
- [ ] 源自动测试
- [ ] 源 CDN 部署

### Phase 4: 高级功能
- [ ] Venera 登录/收藏兼容
- [ ] 自定义源配置
- [ ] 源性能监控
- [ ] 源错误报告