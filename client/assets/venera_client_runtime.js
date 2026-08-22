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

// ===== AES-ECB 解密（纯 JS，AES-128/192/256 + PKCS7）=====
const __aes = (function () {
  const sbox = new Uint8Array(256), rsbox = new Uint8Array(256);
  const S = '637c777bf26b6fc53001672bfed7ab76ca82c97dfa5947f0add4a2af9ca472c0b7fd9326363ff7cc34a5e5f171d8311504c723c31896059a071280e2eb27b27509832c1a1b6e5aa0523bd6b329e32f8453d100ed20fcb15b6acbbe394a4c58cfd0efaafb434d338545f9027f503c9fa851a3408f929d38f5bcb6da2110fff3d2cd0c13ec5f974417c4a77e3d645d197360814fdc222a908846eeb814de5e0bdbe0323a0a4906245cc2d3ac629195e479e7c8376d8dd54ea96c56f4ea657aae08ba78252e1ca6b4c6e8dd741f4bbd8b8a703eb5664803f60e613557b986c11d9ee1f8981169d98e949b1e87e9ce5528df8ca1890dbfe6426841992d0fb054bb16';
  for (let i = 0; i < 256; i++) {
    sbox[i] = parseInt(S.substr(i * 2, 2), 16);
    rsbox[sbox[i]] = i;
  }
  // GF(2^8) 乘法
  function xtime(a) { return ((a << 1) ^ ((a & 0x80) ? 0x1b : 0)) & 0xff; }
  function mul(a, b) {
    let r = 0;
    while (b) { if (b & 1) r ^= a; a = xtime(a); b >>= 1; }
    return r;
  }
  const RCON = [0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36, 0x6c, 0xd8];
  // 密钥扩展 → 返回轮密钥数组（每轮16字节）
  function expandKey(keyBytes) {
    const nk = keyBytes.length / 4, nr = nk + 6;
    const w = new Uint8Array(16 * (nr + 1));
    w.set(keyBytes.subarray(0, 16 * ((nk * 4) / 16)));
    let t = new Uint8Array(4);
    for (let i = nk; i < 4 * (nr + 1); i++) {
      const wi = i * 4;
      if (i % nk === 0) {
        t[0] = w[wi - 4]; t[1] = w[wi - 3]; t[2] = w[wi - 2]; t[3] = w[wi - 1];
        const tmp = t[0]; t[0] = sbox[t[1]] ^ RCON[i / nk - 1]; t[1] = sbox[t[2]]; t[2] = sbox[t[3]]; t[3] = sbox[tmp];
      } else if (nk > 6 && i % nk === 4) {
        t[0] = sbox[w[wi - 4]]; t[1] = sbox[w[wi - 3]]; t[2] = sbox[w[wi - 2]]; t[3] = sbox[w[wi - 1]];
      } else {
        t[0] = w[wi - 4]; t[1] = w[wi - 3]; t[2] = w[wi - 2]; t[3] = w[wi - 1];
      }
      // W[i] = W[i-Nk] ^ g(W[i-1])
      const p = wi - nk * 4;
      w[wi]     = w[p]     ^ t[0];
      w[wi + 1] = w[p + 1] ^ t[1];
      w[wi + 2] = w[p + 2] ^ t[2];
      w[wi + 3] = w[p + 3] ^ t[3];
    }
    return { rk: w, nr };
  }
  function decryptBlock(w, nr, inp, outOff, out) {
    const st = new Uint8Array(16);
    for (let i = 0; i < 16; i++) st[i] = inp[outOff + i];
    function addRK(round) { const b = round * 16; for (let i = 0; i < 16; i++) st[i] ^= w[b + i]; }
    function invShift() {
      const t = new Uint8Array(16);
      // 行 r（索引 r, r+4, r+8, r+12）右移 r
      for (let c = 0; c < 4; c++) {
        for (let r = 0; r < 4; r++) {
          t[r + 4 * ((c + r) % 4)] = st[r + 4 * c];
        }
      }
      st.set(t);
    }
    function invSub() { for (let i = 0; i < 16; i++) st[i] = rsbox[st[i]]; }
    function invMix() {
      for (let c = 0; c < 4; c++) {
        const o = c * 4;
        const a0 = st[o], a1 = st[o + 1], a2 = st[o + 2], a3 = st[o + 3];
        st[o] = mul(a0, 14) ^ mul(a1, 11) ^ mul(a2, 13) ^ mul(a3, 9);
        st[o + 1] = mul(a0, 9) ^ mul(a1, 14) ^ mul(a2, 11) ^ mul(a3, 13);
        st[o + 2] = mul(a0, 13) ^ mul(a1, 9) ^ mul(a2, 14) ^ mul(a3, 11);
        st[o + 3] = mul(a0, 11) ^ mul(a1, 13) ^ mul(a2, 9) ^ mul(a3, 14);
      }
    }
    addRK(nr);
    for (let round = nr - 1; round >= 1; round--) {
      invShift(); invSub(); addRK(round); invMix();
    }
    invShift(); invSub(); addRK(0);
    out.set(st, outOff);
  }
  return {
    ecbDecrypt(bytes, keyUtf8) {
      const keyBytes = [];
      for (let i = 0; i < keyUtf8.length; i++) {
        const c = keyUtf8.charCodeAt(i);
        if (c > 255) throw new Error('aes key must be latin1');
        keyBytes.push(c & 0xff);
      }
      if (![16, 24, 32].includes(keyBytes.length)) throw new Error('invalid aes key length ' + keyBytes.length);
      const { rk, nr } = expandKey(new Uint8Array(keyBytes));
      const nBlocks = Math.floor(bytes.length / 16);
      if (nBlocks === 0) throw new Error('aes data too short');
      const out = new Uint8Array(nBlocks * 16);
      for (let b = 0; b < nBlocks; b++) decryptBlock(rk, nr, bytes, b * 16, out);
      // PKCS7 unpad
      const pad = out[out.length - 1];
      if (pad >= 1 && pad <= 16 && nBlocks * 16 >= pad) return out.subarray(0, out.length - pad);
      return out;
    },
  };
})();

function __aesEcbDecrypt(dataStr, keyStr) {
  // dataStr 为 latin1 二进制串（decodeBase64 输出），还原为字节数组
  const bytes = new Uint8Array(dataStr.length);
  for (let i = 0; i < dataStr.length; i++) bytes[i] = dataStr.charCodeAt(i) & 0xff;
  const dec = __aes.ecbDecrypt(bytes, keyStr);
  // 还原为 latin1 字符串（供 decodeUtf8 转文本）
  let s = '';
  for (let i = 0; i < dec.length; i += 4096) s += String.fromCharCode.apply(null, dec.subarray(i, Math.min(i + 4096, dec.length)));
  return s;
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
    // Venera 官方语义：hmacString 返回 hex 字符串
    return Convert.hexEncode(__cryptoJs({op:'hmac', key:keyBinary, msg:String(msgBinary??''), algo:algo}));
  },
  hmacSha256(key, msg) {
    return Convert.hexEncode(__cryptoJs({op:'hmac', key:key, msg:String(msg??''), algo:'sha256'}));
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
  decryptAesEcb(data, key) { return __aesEcbDecrypt(String(data ?? ''), String(key ?? '')); },
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
        const pair = new RegExp('<(/?)' + tag + '(?=[\\s/>])((?:"[^"]*"|\'[^\']*\'|[^>])*)>', 'gi');
        pair.lastIndex = start + m[0].length;
        let depth = 1, p, found = false;
        while ((p = pair.exec(html)) !== null) {
          if (p[1] === '/') { depth--; if (depth <= 0) { end = p.index + p[0].length; found = true; break; } }
          else if (!/\/\s*>\s*$/.test(p[0])) depth++;
        }
        if (!found) {
          // 源站存在未闭合标签：以下一个同类开标签为边界兜底，避免吞掉整个文档
          const nxt = new RegExp('<' + tag + '(?=[\\s/>])', 'gi');
          nxt.lastIndex = start + m[0].length;
          const nm = nxt.exec(html);
          end = nm ? nm.index : html.length;
        }
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
      // Venera 官方语义：非字符串 body 自动 JSON 序列化
      let outBody = body;
      if (body !== undefined && body !== null && typeof body === 'object') outBody = JSON.stringify(body);
      res = await fetch(url, {
        method: method.toUpperCase(),
        headers: h,
        body: (outBody !== undefined && outBody !== null && method.toUpperCase() !== 'GET') ? outBody : undefined,
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
    // 二进制路径：直接读 arrayBuffer（DataView/图片解析需要真字节）
    try {
      const h = {};
      for (const [k, v] of Object.entries(hdrs || {})) {
        if (v !== undefined && v !== null) h[k] = String(v);
      }
      const res2 = await fetch(realUrl, { method, headers: h, redirect: 'follow' });
      const ab = await res2.arrayBuffer();
      const bin = new Uint8Array(ab);
      return { status: res2.status, bytes: bin, body: bin, headers: {} };
    } catch (e) {
      return { status: 0, bytes: new Uint8Array(0), body: new Uint8Array(0), headers: {}, error: String(e) };
    }
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