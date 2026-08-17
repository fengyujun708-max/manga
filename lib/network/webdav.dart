import 'package:mangaverse/main.dart';
import 'package:mangaverse/network/sync/sync_service.dart';
import 'package:mangaverse/network/sync/webdav_sync_service.dart';

Future<void> testWebDavServer() async {
  final settings = objectbox.userSettingBox.get(1)!.globalSetting;
  final service = WebDavSyncService(settings);
  await service.testConnection();
}

Future<void> syncWithWebDav() async {
  final settings = objectbox.userSettingBox.get(1)!.globalSetting;
  await autoSync(settings);
}
