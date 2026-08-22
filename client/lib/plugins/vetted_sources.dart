/// 实测可用的源白名单（沙箱 Node 回归 2026-08-22，runtime 71861f7+）。
/// 市场只展示这些源；其余（需登录/被墙/停服）一律隐藏。
class VettedSources {
  static const Set<String> ids = {
    'copy_manga',   // 拷贝漫画（域名自动刷新）
    'jm',           // 禁漫天堂（动态域名发现，多线路）
    'komiic',
    'comick',
    'manga_dex',
    'baozi',        // 包子漫畫
    'ccc',          // CCC追漫台
    'zaimanhua',    // 再漫画
    'manhuagui',    // 漫画柜
    'manhuaren',    // 漫画人
    'manwaba',
    'hot_manga',
    'jcomic',       // JC漫畫
    'goda',
    'mh18',
    'mxs',
    'nhentai',
    'wnacg',
    'lanraragi',    // 需自建
    'hcomic',
  };
}
