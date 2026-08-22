// Node 回归 harness：模拟 flutter_js/QuickJS 宿主环境跑真实源 JS
// 用法: node run.js <sourceFile> [explore|search|comic|pages|categories|routes] [args...]
'use strict';
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

// --- sendMessage shim（对齐 Dart 桥接行为：结果写入 globalThis.__cryptoResult）---
globalThis.sendMessage = function (channel, payload) {
  if (channel !== 'crypto') { globalThis.__cryptoResult = ''; return; }
  let op = {};
  try { op = JSON.parse(payload); } catch (_) {}
  const bin = (s) => Buffer.from(String(s ?? ''), 'latin1');
  const out = (buf) => buf.toString('latin1');
  const d = String(op.data ?? '');
  switch (op.op) {
    case 'md5': globalThis.__cryptoResult = out(crypto.createHash('md5').update(bin(d)).digest()); break;
    case 'sha1': globalThis.__cryptoResult = out(crypto.createHash('sha1').update(bin(d)).digest()); break;
    case 'sha256': globalThis.__cryptoResult = out(crypto.createHash('sha256').update(bin(d)).digest()); break;
    case 'sha512': globalThis.__cryptoResult = out(crypto.createHash('sha512').update(bin(d)).digest()); break;
    case 'b64encode': globalThis.__cryptoResult = bin(d).toString('base64'); break;
    case 'b64decode': try { globalThis.__cryptoResult = Buffer.from(d, 'base64').toString('latin1'); } catch (_) { globalThis.__cryptoResult = ''; } break;
    case 'hex': globalThis.__cryptoResult = Buffer.from(d, 'latin1').toString('hex'); break;
    case 'hmac': {
      const algos = { sha1: 'sha1', sha256: 'sha256', sha512: 'sha512', md5: 'md5' };
      const a = algos[String(op.algo || 'sha256').toLowerCase()] || 'sha256';
      globalThis.__cryptoResult = out(crypto.createHmac(a, bin(op.key ?? '')).update(bin(op.msg ?? '')).digest());
      break;
    }
    default: globalThis.__cryptoResult = '';
  }
};

// --- 简化 console / 定时器由 Node 原生提供 ---
// --- APP / btoa / atob 由 runtime 注入 ---

async function main() {
  const [, , file, action = 'explore', ...args] = process.argv;
  const runtimeSrc = fs.readFileSync(path.resolve(__dirname, '../../client/assets/venera_client_runtime.js'), 'utf8');
  const srcCode = fs.readFileSync(file, 'utf8');
  const sourceId = path.basename(file, '.js');

  // 1) 跑 runtime
  (0, eval)(runtimeSrc);
  // 2) 注册源
  await globalThis.__executeSource__(srcCode, sourceId);
  if (!globalThis.__sources__?.[sourceId]) {
    console.log(JSON.stringify({ fatal: 'source not registered' }));
    process.exit(2);
  }

  let out;
  try {
    switch (action) {
      case 'explore': out = await globalThis.__exploreAll__(sourceId); break;
      case 'search': out = await globalThis.__search__(sourceId, args[0] || '海贼王', parseInt(args[1] || '1')); break;
      case 'comic': out = await globalThis.__comic__(sourceId, args[0]); break;
      case 'pages': out = await globalThis.__pages__(sourceId, args[0], args[1]); break;
      case 'categories': out = await globalThis.__categories__(sourceId); break;
      case 'routes': {
        const s = globalThis.__sources__[sourceId];
        out = { exploreKeys: Object.keys(s), settings: Object.keys(s.settings || {}) };
        break;
      }
      default: throw new Error('unknown action ' + action);
    }
  } catch (e) {
    out = { error: String((e && e.message) || e) };
  }

  // 单行摘要（不破坏 JSON）
  const count = (o) => {
    if (!Array.isArray(o)) return o && o.error ? `ERR:${String(o.error).slice(0,80)}` : (o && o.fatal ? 'FATAL:'+o.fatal : 'EMPTY');
    let tot = 0; const errs = [];
    for (const s of o) { if (s.error) errs.push(String(s.error).slice(0,80)); if (Array.isArray(s.items)) tot += s.items.length; }
    return `${tot}items${errs.length ? ' ERR:' + errs[0] : ''}`;
  };
  let line;
  try { line = count(JSON.parse(JSON.stringify(out))); } catch(_) { line = 'CYCLIC'; }
  console.log(line);
}

main().catch(e => { console.error('HARNESS FATAL', e); process.exit(1); });
