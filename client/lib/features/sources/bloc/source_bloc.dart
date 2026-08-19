import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import '../../../plugins/manga_source.dart';
import '../../../plugins/runtime/js_engine.dart';
import '../../../core/network/api_client.dart';

part 'source_event.dart';
part 'source_state.dart';

class SourceBloc extends Bloc<SourceEvent, SourceState> {
  final ApiClient apiClient;
  final SourceManager _sourceManager;

  SourceBloc({required this.apiClient})
      : _sourceManager = SourceManager(apiClient: apiClient, jsRuntime: QuickJSEngine()),
        super(SourceInitial()) {
    on<SourceLoadRequested>(_onLoad);
    on<SourceRefreshRequested>(_onRefresh);
    on<SourceToggleEnabled>(_onToggleEnabled);
    on<SourceUpdateRequested>(_onUpdate);
    on<SourceTestRequested>(_onTest);
    on<SourceDeleteRequested>(_onDelete);
    on<SourceMarketLoadRequested>(_onMarketLoad);
    on<SourceInstallRequested>(_onInstall);
  }

  Future<void> _onLoad(SourceLoadRequested event, Emitter<SourceState> emit) async {
    emit(SourceLoading());
    try {
      await _sourceManager.initialize();
      final sources = _sourceManager.getInstalledSources();
      emit(SourceLoaded(sources: sources));
    } catch (e) {
      emit(SourceError(message: '加载失败: $e'));
    }
  }

  Future<void> _onRefresh(SourceRefreshRequested event, Emitter<SourceState> emit) async {
    emit(SourceLoading());
    try {
      await _sourceManager.checkUpdates();
      final sources = _sourceManager.getInstalledSources();
      emit(SourceLoaded(sources: sources));
    } catch (e) {
      emit(SourceError(message: '刷新失败: $e'));
    }
  }

  Future<void> _onToggleEnabled(SourceToggleEnabled event, Emitter<SourceState> emit) async {
    try {
      await _sourceManager.toggleSource(event.sourceId, event.enabled);
      final sources = _sourceManager.getInstalledSources();
      emit(SourceLoaded(sources: sources));
    } catch (e) {
      emit(SourceError(message: '操作失败: $e'));
    }
  }

  Future<void> _onUpdate(SourceUpdateRequested event, Emitter<SourceState> emit) async {
    emit(SourceUpdating(sourceId: event.sourceId));
    try {
      await _sourceManager.updateSource(event.sourceId);
      final sources = _sourceManager.getInstalledSources();
      emit(SourceLoaded(sources: sources));
    } catch (e) {
      emit(SourceError(message: '更新失败: $e'));
    }
  }

  Future<void> _onTest(SourceTestRequested event, Emitter<SourceState> emit) async {
    emit(SourceTesting(sourceId: event.sourceId));
    try {
      final result = await _sourceManager.testSource(event.sourceId);
      emit(SourceTestResultState(sourceId: event.sourceId, result: result));
    } catch (e) {
      emit(SourceTestError(sourceId: event.sourceId, message: '测试失败: $e'));
    }
  }

  Future<void> _onDelete(SourceDeleteRequested event, Emitter<SourceState> emit) async {
    try {
      await _sourceManager.uninstallSource(event.sourceId);
      final sources = _sourceManager.getInstalledSources();
      emit(SourceLoaded(sources: sources));
    } catch (e) {
      emit(SourceError(message: '删除失败: $e'));
    }
  }

  Future<void> _onMarketLoad(SourceMarketLoadRequested event, Emitter<SourceState> emit) async {
    emit(SourceMarketLoading());
    try {
      final sources = await apiClient.get('/sources');
      final marketSources = (sources['sources'] as List).map((e) => SourceManifest.fromJson(e)).toList();
      emit(SourceMarketLoaded(sources: marketSources));
    } catch (e) {
      emit(SourceMarketError(message: '加载市场失败: $e'));
    }
  }

  Future<void> _onInstall(SourceInstallRequested event, Emitter<SourceState> emit) async {
    emit(SourceInstalling(sourceId: event.sourceId));
    try {
      await _sourceManager.installSource(event.sourceId, event.sourceUrl);
      final sources = _sourceManager.getInstalledSources();
      emit(SourceLoaded(sources: sources));
    } catch (e) {
      emit(SourceError(message: '安装失败: $e'));
    }
  }
}