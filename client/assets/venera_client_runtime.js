// 漫界客户端 Venera 运行时 —— 纯浏览器环境（QuickJS/flutter_js）
// 网络通过宿主 fetch（Dart 实现），HTML 解析用简易 DOM 引擎
'use strict';

// ===== Venera 宿主全局（源 JS 常引用）=====
globalThis.APP = {
  locale: 'zh_CN',
  platform: 'android',
  packageName: 'com.manjie.app',
  appVersion: '1.2.0',
  version: '1.2.0',
  channelId: 'dev',
};

// ===== fetch 兜底：flutter_js 未注入全局 fetch 时用 XMLHttpRequest 实现 =====
if (typeof globalThis.fetch !== 'function') {
  globalThis.fetch = function (url, options) {
    options = options || {};
    return new Promise(function (resolve, reject) {
      try {
        var xhr = new XMLHttpRequest();
        xhr.open(options.method || 'GET', url, true);
        var hs = options.headers || {};
        Object.keys(hs).forEach(function (k) { try { xhr.setRequestHeader(k, String(hs[k])); } catch (_) {} });
        xhr.onload = function () {
          var headers = { get: function (name) {
            try { return xhr.getResponseHeader(name) || ''; } catch (_) { return ''; }
          }};
          resolve({ status: xhr.status || 0, ok: (xhr.status || 0) >= 200 && (xhr.status || 0) < 300,
            text: function () { return Promise.resolve(String(xhr.responseText || '')); },
            json: function () { return Promise.resolve(JSON.parse(xhr.responseText || 'null')); },
            headers: headers });
        };
        xhr.onerror = function () { reject(new Error('XHR network error')); };
        xhr.ontimeout = function () { reject(new Error('XHR timeout')); };
        if (options.body !== undefined && options.body !== null && String(options.method || 'GET').toUpperCase() !== 'GET') {
          xhr.send(typeof options.body === 'string' ? options.body : JSON.stringify(options.body));
        } else { xhr.send(); }
      } catch (e) { reject(e); }
    });
  };
}

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
  const VOID_TAGS = /^(img|br|hr|input|meta|link|source|area|base|col|embed|track|wbr)$/i;

  // 扫描匹配谓词的开标签并做深度配对提取完整元素
  function scanOpen(tagPattern, pred) {
    const res = [];
    const re = new RegExp('<(' + tagPattern + ')((?:"[^"]*"|\'[^\']*\'|[^>])*)>', 'gi');
    let m;
    while ((m = re.exec(html)) !== null) {
      const attrs = m[2] || '';
      if (pred && !pred(attrs)) continue;
      const tag = m[1];
      const start = m.index;
      let end;
      if (/\/\s*>\s*$/.test(m[0]) || VOID_TAGS.test(tag)) {
        end = start + m[0].length;
      } else {
        const pair = new RegExp('<(/?)' + tag + '(?:(?:"[^"]*"|\'[^\']*\'|[^>])*)>', 'gi');
        pair.lastIndex = start + m[0].length;
        let depth = 1, p, found = false;
        while ((p = pair.exec(html)) !== null) {
          if (p[1] === '/') { depth--; if (depth <= 0) { end = p.index + p[0].length; found = true; break; } }
          else if (!/\/\s*>\s*$/.test(p[0])) depth++;
        }
        if (!found) end = html.length;
      }
      res.push(html.slice(start, end));
      re.lastIndex = end;
    }
    return res;
  }

  function getAttrVal(attrs, name) {
    const mm = attrs.match(new RegExp('\\b' + name + '\\s*=\\s*(?:"([^"]*)"|\'([^\']*)\')', 'i'));
    return mm ? (mm[1] !== undefined ? mm[1] : mm[2]) : null;
  }
  function classOk(attrs, needCls) {
    if (!needCls.length) return true;
    const cv = getAttrVal(attrs, 'class');
    if (cv === null) return false;
    const have = ' ' + cv + ' ';
    return needCls.every(function (c) { return have.indexOf(' ' + c + ' ') !== -1; });
  }
  function attrCheck(attrs, aName, aOp, aVal) {
    if (!aName) return true;
    const rawAttr = new RegExp('\\b' + aName + '(?=[\\s=>/]|$)', 'i').test(attrs);
    if (!aOp) return rawAttr;
    const v = getAttrVal(attrs, aName);
    if (v === null) return false;
    switch (aOp) {
      case '=': return v === aVal;
      case '^=': return v.slice(0, aVal.length) === aVal;
      case '$=': return v.slice(-aVal.length) === aVal;
      case '*=': return v.indexOf(aVal) !== -1;
      case '~=': return (' ' + v + ' ').indexOf(' ' + aVal + ' ') !== -1;
      default: return false;
    }
  }

  if (part.charAt(0) === '#') {
    const id = part.slice(1);
    return scanOpen('[a-zA-Z0-9]+', function (a) { return getAttrVal(a, 'id') === id; });
  }
  if (part.charAt(0) === '.') {
    const needCls = [part.slice(1)];
    return scanOpen('[a-zA-Z0-9]+', function (a) { return classOk(a, needCls); });
  }
  const cm = part.match(/^([a-zA-Z][a-zA-Z0-9]*|\*)?(?:\.([a-zA-Z0-9_\-]+(?:\.[a-zA-Z0-9_\-]+)*))?(?:\[([a-zA-Z-]+)\s*(?:([\*\^\$~]?=)\s*(?:"([^"]*)"|'([^']*)'|([^\]\s"']+)))?\])?$/);
  if (cm && (cm[1] || cm[2] || cm[3])) {
    let tp = (!cm[1] || cm[1] === '*') ? '[a-zA-Z0-9]+' : cm[1].replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const needCls = cm[2] ? cm[2].split('.') : [];
    const aName = cm[3], aOp = cm[4] || '';
    const aVal = cm[5] !== undefined ? cm[5] : (cm[6] !== undefined ? cm[6] : cm[7]);
    return scanOpen(tp, function (a) { return classOk(a, needCls) && attrCheck(a, aName, aOp, aVal); });
  }
  const tag = (part === '*') ? '[a-zA-Z0-9]+' : part.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  return scanOpen(tag, null);
}

function parseSelector(html, selector) {
  if (html !== undefined && typeof html === 'object' && typeof html._html === 'string') html = html._html;
  if (typeof html !== 'string' || !html) return [];
  const results = [];
  const ors = selector.split(',').map(s => s.trim()).filter(Boolean);
  for (const or of ors) {
    const parts = or.split(/\s+/).filter(p => p && p !== '>');
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
    const rawTag = this._html.match(/^\s*<([a-zA-Z0-9]+)[^>]*>([\s\S]*?)<\/\1>\s*$/i);
    this.textContent = (rawTag && /^(script|style|pre|textarea)$/i.test(rawTag[1])) ? rawTag[2] : htmlToText(this._html);
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
    try {
      const ov = globalThis.__settingsOverride__;
      if (ov && ov[key] !== undefined && ov[key] !== null) return ov[key];
    } catch (_) {}
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
    console, setTimeout, clearTimeout, setInterval, clearInterval,
    TextEncoder, TextDecoder, URLSearchParams, URL,
    createUuid: () => (globalThis.crypto && globalThis.crypto.randomUUID ? globalThis.crypto.randomUUID() : 'id-' + randomInt(10000, 99999)),
    btoa: (s) => {
      const S = String(s); const CH = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
      let out = '';
      for (let i = 0; i < S.length; i += 3) {
        const b = [S.charCodeAt(i), S.charCodeAt(i + 1), S.charCodeAt(i + 2)];
        out += CH[b[0] >> 2] + CH[((b[0] & 3) << 4) | ((isNaN(b[1]) ? 0 : b[1]) >> 4)] + (isNaN(b[1]) ? '=' : CH[((b[1] & 15) << 2) | ((isNaN(b[2]) ? 0 : b[2]) >> 6)]) + (isNaN(b[2]) ? '=' : CH[b[2] & 63]);
      }
      return out;
    },
    atob: (s) => {
      const S = String(s).replace(/[^A-Za-z0-9+\/=]/g, ''); const CH = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
      let out = '';
      for (let i = 0; i < S.length; i += 4) {
        const e = [CH.indexOf(S[i]), CH.indexOf(S[i + 1]), CH.indexOf(S[i + 2]), CH.indexOf(S[i + 3])];
        out += String.fromCharCode((e[0] << 2) | ((e[1] === -1 ? 0 : e[1]) >> 4));
        if (e[2] !== -1 && S[i + 2] !== '=') out += String.fromCharCode(((e[1] & 15) << 4) | (e[2] >> 2));
        if (e[3] !== -1 && S[i + 3] !== '=') out += String.fromCharCode(((e[2] & 3) << 6) | e[3]);
      }
      return out;
    },
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

// ===== 显式挂载到全局（保证跨 script 可见）=====
try {
  const __g = globalThis;
  ['SimpleElement','SimpleDocument','HtmlDocument','Comic','ComicDetails','ComicList','Cookie','PageJumpTarget','Comment','ComicSource','parseSelector','matchSelectorPart','htmlToText','extractAttr','randomInt','Network','Convert'].forEach(n => { if (__g[n] === undefined) { try { __g[n] = eval(n); } catch (_) {} } });
} catch (_) {}