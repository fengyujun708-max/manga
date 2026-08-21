// 漫界客户端 Venera 运行时 —— 纯浏览器环境（QuickJS/flutter_js）
// 网络通过宿主 fetch（Dart 实现），HTML 解析用简易 DOM 引擎
'use strict';

// ===== Convert（加密通过宿主桥接，结果存全局变量）=====
function __cryptoJs(op) {
  sendMessage('crypto', JSON.stringify(op));
  return globalThis.__cryptoResult || '';
}
const Convert = {
  decodeBase64(s) { return __cryptoJs({op:'b64decode', data:String(s??'')}); },
  encodeBase64(s) { return __cryptoJs({op:'b64encode', data:String(s??'')}); },
  encodeUtf8(s) { return unescape(encodeURIComponent(String(s ?? ''))); },
  decodeUtf8(s) { return decodeURIComponent(escape(String(s ?? ''))); },
  md5(s) { return __cryptoJs({op:'md5', data:String(s??'')}); },
  sha1(s) { return __cryptoJs({op:'sha1', data:String(s??'')}); },
  sha256(s) { return __cryptoJs({op:'sha256', data:String(s??'')}); },
  sha512(s) { return __cryptoJs({op:'sha512', data:String(s??'')}); },
  hmacString(keyBinary, msgBinary, algo = 'sha256') {
    return __cryptoJs({op:'hmac', key:keyBinary, msg:String(msgBinary??''), algo:algo});
  },
  hmacSha256(key, msg) {
    return __cryptoJs({op:'hmac', key:key, msg:String(msg??''), algo:'sha256'});
  },
  hexEncode(s) {
    return Array.from(String(s ?? ''), (c) => c.charCodeAt(0).toString(16).padStart(2, '0')).join('');
  },
  hexDecode(s) {
    let out = '';
    for (let i = 0; i < String(s).length; i += 2) out += String.fromCharCode(parseInt(String(s).substr(i, 2), 16));
    return out;
  },
  decryptAesCbc(data, key, iv) { return data; },
  decryptAesEcb(data, key) { return data; },
  _toBuf(data) { return data; },
};

// ===== 简易 CSS 解析器 =====
function htmlToText(html) {
  return String(html || '')
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/<style[\s\S]*?<\/style>/gi, '')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/g, ' ').replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'")
    .replace(/\s+/g, ' ')
    .trim();
}

function extractAttr(html, attr) {
  const re = new RegExp(`${attr}=['"]([^'"]*)['"]`, 'i');
  const m = String(html).match(re);
  return m ? m[1] : '';
}

function matchSelectorPart(html, part) {
  if (!html) return [];
  const out = [];
  if (part.startsWith('#')) {
    const id = part.slice(1);
    const re = new RegExp(`<[a-zA-Z0-9]+[^>]*\\bid=["']${id}["'][^>]*>[\\s\\S]*?</[a-zA-Z0-9]+>|<[a-zA-Z0-9]+[^>]*\\bid=["']${id}["'][^>]*/?>`, 'gi');
    let m; while ((m = re.exec(html)) !== null) out.push(m[0]);
    return out;
  }
  if (part.startsWith('.')) {
    const cls = part.slice(1).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const re = new RegExp(`<[a-zA-Z0-9]+[^>]*class=["'][^"']*\\b${cls}\\b[^"']*["'][^>]*>(?:[\\s\\S]*?</[a-zA-Z0-9]+>)?`, 'gi');
    let m; while ((m = re.exec(html)) !== null) out.push(m[0]);
    return out;
  }
  const attrMatch = part.match(/^\[([a-zA-Z-]+)(?:=["']([^"']*)["'])?\]$/);
  if (attrMatch) {
    const attr = attrMatch[1];
    const val = attrMatch[2];
    let re;
    if (val !== undefined) {
      re = new RegExp(`<[a-zA-Z0-9]+[^>]*\\b${attr}=["']${val.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}["'][^>]*>(?:[\\s\\S]*?<\\/[a-zA-Z0-9]+>)?`, 'gi');
    } else {
      re = new RegExp(`<[a-zA-Z0-9]+[^>]*\\b${attr}[^>]*>(?:[\\s\\S]*?<\\/[a-zA-Z0-9]+>)?`, 'gi');
    }
    let m; while ((m = re.exec(html)) !== null) out.push(m[0]);
    return out;
  }
  const tag = part === '*' ? '[a-zA-Z0-9]+' : part.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const re = new RegExp(`<${tag}[^>]*>(?:[\\s\\S]*?<\\/${tag}>)?`, 'gi');
  let m; while ((m = re.exec(html)) !== null) out.push(m[0]);
  return out;
}

function parseSelector(html, selector) {
  if (html !== undefined && typeof html === 'object' && typeof html._html === 'string') html = html._html;
  if (typeof html !== 'string' || !html) return [];
  const results = [];
  const ors = selector.split(',').map(s => s.trim()).filter(Boolean);
  for (const or of ors) {
    const parts = or.split(/\s+/);
    let current = [html];
    for (const part of parts) {
      const next = [];
      for (const chunk of current) {
        const matched = matchSelectorPart(chunk, part);
        next.push(...matched);
      }
      current = next;
      if (!current.length) break;
    }
    for (const chunk of current) results.push(new SimpleElement(chunk));
  }
  return results;
}

class SimpleElement {
  constructor(html) {
    this._html = String(html || '');
    this.attributes = {};
    const attrRe = /([a-zA-Z-]+)=["']([^"']*)["']/g;
    let m; while ((m = attrRe.exec(this._html)) !== null) this.attributes[m[1]] = m[2];
    this.textContent = htmlToText(this._html);
    this.text = this.textContent;
    this.innerHTML = this._html;
  }
  getAttribute(name) { return this.attributes[name] ?? null; }
  querySelector(sel) { return parseSelector(this._html, sel)[0] ?? new SimpleElement(''); }
  querySelectorAll(sel) { return parseSelector(this._html, sel); }
  get children() { return parseSelector(this._html, '*'); }
  get tagName() { return extractAttr(this._html, 'tag') || 'div'; }
}

class SimpleDocument {
  constructor(html) { this._html = String(html || ''); }
  querySelector(sel) { return parseSelector(this._html, sel)[0] ?? new SimpleElement(''); }
  querySelectorAll(sel) { return parseSelector(this._html, sel); }
  getElementById(id) {
    const re = new RegExp(`<[a-zA-Z0-9]+[^>]*\\bid=["']${String(id).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}["'][^>]*>[\\s\\S]*?</[a-zA-Z0-9]+>`, 'gi');
    const m = this._html.match(re);
    return m ? new SimpleElement(m[0]) : new SimpleElement('');
  }
  get textContent() { return htmlToText(this._html); }
  get text() { return htmlToText(this._html); }
  get title() {
    const m = this._html.match(/<title[^>]*>([^<]*)<\/title>/i);
    return m ? m[1].trim() : '';
  }
  dispose() {}
}

class HtmlDocument {
  constructor(html) { this._html = String(html || ''); }
  querySelector(sel) { return new SimpleDocument(this._html).querySelector(sel); }
  querySelectorAll(sel) { return new SimpleDocument(this._html).querySelectorAll(sel); }
  getElementById(id) { return new SimpleDocument(this._html).getElementById(id); }
  get textContent() { return htmlToText(this._html); }
  get text() { return this.textContent; }
  get body() { return this; }
  get head() { return this; }
  get documentElement() { return this; }
  dispose() {}
}

// ===== 数据类 =====
class Comic { constructor(opts = {}) { Object.assign(this, opts); } }
class ComicDetails { constructor(opts = {}) { Object.assign(this, opts); } }
class ComicList { constructor(opts = {}) { Object.assign(this, opts); } }
class Cookie {
  constructor(opts = {}) {
    this.name = opts.name || ''; this.value = opts.value || ''; this.domain = opts.domain || '';
    this.path = opts.path || '/'; this.expires = opts.expires; this.httpOnly = opts.httpOnly || false; this.secure = opts.secure || false;
  }
  toString() { return `${this.name}=${this.value}`; }
}
class PageJumpTarget { constructor(opts = {}) { Object.assign(this, opts); } }
class Comment { constructor(opts = {}) { Object.assign(this, opts); } }

// ===== Network（基于全局 fetch —— flutter_js XHR→Dart http 异步桥接）=====
const Network = {
  _cookies: new Map(),
  async request(method, url, headers = {}, body) {
    // 清理 undefined headers
    const h = {};
    for (const [k, v] of Object.entries(headers || {})) {
      if (v !== undefined && v !== null) h[k] = String(v);
    }
    let res;
    try {
      res = await fetch(url, {
        method: method.toUpperCase(),
        headers: h,
        body: (body !== undefined && body !== null && method.toUpperCase() !== 'GET') ? body : undefined,
        redirect: 'follow',
      });
    } catch (e) {
      throw new Error('网络错误: ' + String(e && e.message ? e.message : e));
    }
    const text = await res.text();
    let ct = '';
    try { ct = res.headers && res.headers.get ? (res.headers.get('content-type') || '') : ''; } catch (_) {}
    return { status: res.status, body: text, headers: { 'content-type': ct } };
  },
  async get(url, headers = {}, query) {
    let finalUrl = url;
    if (query) {
      const params = new URLSearchParams();
      for (const [k, v] of Object.entries(query)) params.append(k, v);
      finalUrl = url + (url.includes('?') ? '&' : '?') + params.toString();
    }
    return this.request('GET', finalUrl, headers, null);
  },
  async post(url, headers = {}, body) {
    return this.request('POST', url, headers, body);
  },
  async put(url, headers = {}, body) { return this.request('PUT', url, headers, body); },
  async patch(url, headers = {}, body) { return this.request('PATCH', url, headers, body); },
  async delete(url, headers = {}) { return this.request('DELETE', url, headers, null); },
  async sendRequest(options) {
    if (typeof options === 'string') {
      const method = arguments[0].toUpperCase();
      const url = arguments[1];
      const headers = arguments[2] || {};
      const body = arguments[3];
      if (typeof url === 'string' && url.startsWith('http')) {
        return this.request(method, url, headers, body);
      }
    }
    const method = (options.method || 'GET').toUpperCase();
    let url = options.url;
    if (options.query) {
      const params = new URLSearchParams();
      for (const [k, v] of Object.entries(options.query)) params.append(k, v);
      url += (url.includes('?') ? '&' : '?') + params.toString();
    }
    return this.request(method, url, options.headers || {}, options.body);
  },
  async fetchBytes(url, headers = {}) {
    let method = 'GET', realUrl = url, hdrs = headers;
    if (typeof url === 'string' && /^(GET|POST|PUT|PATCH|DELETE|HEAD)$/i.test(url) && typeof arguments[1] === 'string') {
      method = url.toUpperCase(); realUrl = arguments[1]; hdrs = arguments[2] || {};
    }
    const res = await this.request(method, realUrl, hdrs, null);
    const bin = typeof res.body === 'string'
      ? Uint8Array.from(atob(btoa(unescape(encodeURIComponent(res.body)))).split(''), (c) => c.charCodeAt(0))
      : (res.body || new Uint8Array(0));
    return { status: res.status, bytes: bin, body: bin, headers: res.headers || {} };
  },
  getCookies(url) {
    try { const u = new URL(url); return Network._cookies.get(u.hostname) || []; } catch { return []; }
  },
  setCookies(url, cookies) {
    try {
      const u = new URL(url);
      const key = u.hostname;
      const existing = Network._cookies.get(key) || [];
      for (const c of cookies) {
        const idx = existing.findIndex(e => e.name === c.name);
        if (idx >= 0) existing[idx] = c; else existing.push(c);
      }
      Network._cookies.set(key, existing);
    } catch {}
  },
  deleteCookies(url) {
    try { const u = new URL(url); Network._cookies.delete(u.hostname); } catch {}
  },
  clearCookies() { Network._cookies.clear(); },
};

function randomInt(min, max) {
  return min + Math.floor(Math.random() * (max - min + 1));
}

// ===== ComicSource 基类 =====
class ComicSource {
  constructor() {
    this._data = {};
    this._settings = {};
    this.appVersion = '9.9.9';
    const proto = this.constructor.prototype;
    if (proto) {
      const defaults = { name: '', key: '', version: '', url: '', apiUrl: '', explore: [], search: null, comic: null, categories: [], category: null, categoryComics: null };
      for (const [prop, def] of Object.entries(defaults)) {
        const desc = Object.getOwnPropertyDescriptor(proto, prop);
        if (!desc || !('get' in desc)) {
          if (!(prop in this)) this[prop] = def;
        }
      }
    }
  }
  loadData(key) { return this._data[key]; }
  saveData(key, val) { this._data[key] = val; }
  deleteData(key) { delete this._data[key]; }
  loadSetting(key) {
    if (this._settings[key] !== undefined && this._settings[key] !== null) return this._settings[key];
    if (this.settings && this.settings[key] && this.settings[key].default !== undefined) return this.settings[key].default;
    return undefined;
  }
  saveSetting(key, val) { this._settings[key] = val; }
  isAppVersionAfter() { return true; }
  get imageQuality() { return '1500'; }
  set imageQuality(v) {}
  log(msg) { console.log(`[source:${this.name}]`, msg); }
  error(msg) { throw msg; }
}

// ===== 执行源 =====
// 注意：QuickJS 中直接运行，无法用 vm。我们用全局注册表模式：
// executeSource 在 host 侧调用（Dart 桥接），这里定义工厂

globalThis.__executeSource__ = async function (jsCode, sourceId) {
  const sandboxProto = {
    ComicSource, Network, Convert, randomInt,
    Comic, ComicDetails, ComicList, Cookie, PageJumpTarget, Comment, HtmlDocument,
    APP: { version: '9.9.9' },
    console, setTimeout, clearTimeout, setInterval, clearInterval,
    TextEncoder, TextDecoder, URLSearchParams, URL,
    createUuid: () => (globalThis.crypto && globalThis.crypto.randomUUID ? globalThis.crypto.randomUUID() : 'id-' + randomInt(10000, 99999)),
    btoa: (s) => btoa(String(s)),
    atob: (s) => atob(String(s)),
    Date, Math, JSON, Promise, RegExp, String, Number, Boolean, Array, Object, Map, Set, Error,
    encodeURIComponent, decodeURIComponent, encodeURI, decodeURI,
  };
  // 全局挂载（QuickJS 中 globalThis 即全局）
  Object.assign(globalThis, sandboxProto);

  // 执行源代码
  const classMatch = jsCode.match(/class\s+([A-Za-z_$][\w$]*)\s+extends\s+ComicSource/);
  let execCode = jsCode;
  if (classMatch) execCode += `\n;globalThis.__sourceClass = ${classMatch[1]};`;
  eval(execCode);

  let SourceClass = globalThis.__sourceClass || null;
  if (!SourceClass) {
    for (const key of Object.keys(globalThis)) {
      const val = globalThis[key];
      if (typeof val === 'function' && val.prototype instanceof ComicSource && val !== ComicSource) {
        SourceClass = val; break;
      }
    }
  }
  if (!SourceClass) {
    if (globalThis.source && globalThis.source instanceof ComicSource) return globalThis.source;
    throw new Error(`源 ${sourceId} 未找到 ComicSource 子类`);
  }
  if (SourceClass.fallbackServers && !SourceClass.apiDomains) {
    SourceClass.apiDomains = SourceClass.fallbackServers.slice();
  }
  const instance = new SourceClass();
  try {
    if (instance.settings && typeof instance.settings === 'object') {
      for (const k of Object.keys(instance.settings)) {
        const v = instance.settings[k];
        if (v && typeof v === 'object' && v.default !== undefined && instance._settings[k] === undefined) {
          instance._settings[k] = v.default;
        }
      }
    }
  } catch (_) {}
  if (instance.init) {
    try {
      await Promise.race([
        Promise.resolve(instance.init()),
        new Promise((r) => setTimeout(r, 8000)),
      ]);
    } catch (_) {}
  }
  globalThis.__sources__ = globalThis.__sources__ || {};
  globalThis.__sources__[sourceId] = instance;
  return instance;
};

// ===== 宿主辅助函数 =====
// 执行 explore 所有板块（返回标准 JSON）
globalThis.__exploreAll__ = async function (sourceId) {
  const src = globalThis.__sources__ && globalThis.__sources__[sourceId];
  if (!src) return { error: 'source not loaded' };
  const out = [];
  const explore = Array.isArray(src.explore) ? src.explore : (src.explore ? [src.explore] : []);
  for (const sec of explore) {
    try {
      const title = sec.title || sec.name || '首页';
      if (typeof sec.load === 'function') {
        const result = await sec.load(1);
        let items = [];
        if (Array.isArray(result)) {
          items = result;
        } else if (result && typeof result === 'object') {
          // {板块名: [comic,...]} → 拆成多个 section
          const keys = Object.keys(result);
          for (const k of keys) {
            if (Array.isArray(result[k]) && result[k].length > 0) {
              out.push({ title: k, type: sec.type || 'singlePageWithMultiPart', items: result[k] });
            }
          }
          continue;
        }
        out.push({ title, type: sec.type || 'singlePageWithMultiPart', items });
      }
    } catch (e) {
      out.push({ title: sec.title || '板块', type: sec.type || '', items: [], error: String(e && e.message ? e.message : e) });
    }
  }
  return out;
};

// 分类
globalThis.__categories__ = async function (sourceId) {
  const src = globalThis.__sources__ && globalThis.__sources__[sourceId];
  if (!src) return { error: 'source not loaded' };
  const cat = src.category;
  if (!cat) return [];
  const parts = (cat.parts && Array.isArray(cat.parts)) ? cat.parts : [];
  const out = [];
  for (const p of parts) {
    const name = p.name || '';
    const cats = Array.isArray(p.categories) ? p.categories : [];
    const params = Array.isArray(p.categoryParams) ? p.categoryParams : [];
    out.push({
      name,
      categories: cats.map((c, i) => ({
        name: typeof c === 'string' ? c : (c.name || c.title || String(c)),
        param: params[i] !== undefined ? String(params[i]) : '',
      })),
    });
  }
  return out;
};

// 分类漫画（分页）
globalThis.__categoryComics__ = async function (sourceId, category, param, options, page) {
  const src = globalThis.__sources__ && globalThis.__sources__[sourceId];
  if (!src) return { error: 'source not loaded' };
  if (typeof src.categoryComics !== 'object' || !src.categoryComics || typeof src.categoryComics.load !== 'function') {
    return { items: [] };
  }
  const res = await src.categoryComics.load(category, param, options || [], page || 1);
  if (Array.isArray(res)) return { items: res };
  if (res && typeof res === 'object') {
    const items = res.comics || res.items || res.list || [];
    return { items, hasMore: res.hasMore === true || res.hasNextPage === true };
  }
  return { items: [] };
};

// 搜索
globalThis.__search__ = async function (sourceId, keyword, page) {
  const src = globalThis.__sources__ && globalThis.__sources__[sourceId];
  if (!src || typeof src.search !== 'function') return { error: 'no search' };
  const res = await src.search(keyword, page || 1);
  if (Array.isArray(res)) return { items: res };
  if (res && typeof res === 'object') {
    const items = res.comics || res.items || res.list || [];
    return { items, hasMore: res.hasMore === true || res.hasNextPage === true };
  }
  return { items: [] };
};

// 详情 + 章节
globalThis.__comic__ = async function (sourceId, comicId) {
  const src = globalThis.__sources__ && globalThis.__sources__[sourceId];
  if (!src || typeof src.comic !== 'function') return { error: 'no comic method' };
  const d = await src.comic(comicId);
  return d || {};
};

// 图片页
globalThis.__pages__ = async function (sourceId, comicId, epId) {
  const src = globalThis.__sources__ && globalThis.__sources__[sourceId];
  if (!src || typeof src.pages !== 'function') return { error: 'no pages method' };
  const p = await src.pages(comicId, epId);
  if (Array.isArray(p)) return { pages: p };
  if (p && typeof p === 'object') {
    const pages = p.pages || p.urls || p.images || (typeof p.next === 'object' && Array.isArray(p.next.pages) ? p.next.pages : []);
    let next = '';
    if (p.next && typeof p.next === 'object' && p.next.pages) {
      const pagesList = p.next.pages || [];
      if (pagesList.length > 0) next = String(pagesList[0] || '');
    }
    return { pages: Array.isArray(pages) ? pages : [], next: next || (p.next !== undefined && typeof p.next === 'string' ? p.next : '') };
  }
  return { pages: [] };
};