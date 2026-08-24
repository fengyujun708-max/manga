/// 极简应用设置桩 — 替代旧持久化模块，仅提供网络层所需的默认值。
class Appdata {
  static final Map<String, dynamic> settings = {
    'downloadThreads': 3,
    'proxy': 'system',
  };
}

final Appdata appdata = Appdata();