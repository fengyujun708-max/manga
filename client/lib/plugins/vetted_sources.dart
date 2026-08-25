/// 实测可用的源白名单（沙箱 Node 回归 2026-08-22，runtime 71861f7+）。
/// 市场只展示这些源；其余（需登录/被墙/停服）一律隐藏。
class VettedSources {
  static const Set<String> ids = {
    'copy_manga', 'jm', 'komiic', 'comick', 'manga_dex', 'baozi', 'ccc', 'zaimanhua',
    'manhuagui', 'manhuaren', 'manwaba', 'hot_manga', 'jcomic', 'goda', 'mh18', 'mxs',
    'nhentai', 'wnacg', 'lanraragi', 'hcomic', 'picacg', 'comic_walker',
    'shonen_jump_plus', 'hitomi', 'ykmh', 'ikmmh', 'ehentai', 'mh1234', 'kavita', 'happy',
    'komga', 'mycomic',
  };
}
