import 'package:equatable/equatable.dart';
import '../../plugins/manga_source.dart';

abstract class SourceEvent extends Equatable {
  const SourceEvent();
  @override List<Object?> get props => [];
}

class SourceLoadRequested extends SourceEvent {}
class SourceRefreshRequested extends SourceEvent {}
class SourceToggleEnabled extends SourceEvent {
  final String sourceId;
  final bool enabled;
  const SourceToggleEnabled(this.sourceId, this.enabled);
  @override List<Object?> get props => [sourceId, enabled];
}
class SourceUpdateRequested extends SourceEvent {
  final String sourceId;
  const SourceUpdateRequested(this.sourceId);
  @override List<Object?> get props => [sourceId];
}
class SourceTestRequested extends SourceEvent {
  final String sourceId;
  const SourceTestRequested(this.sourceId);
  @override List<Object?> get props => [sourceId];
}
class SourceDeleteRequested extends SourceEvent {
  final String sourceId;
  const SourceDeleteRequested(this.sourceId);
  @override List<Object?> get props => [sourceId];
}
class SourceMarketLoadRequested extends SourceEvent {}
class SourceInstallRequested extends SourceEvent {
  final String sourceId;
  final String sourceUrl;
  const SourceInstallRequested({required this.sourceId, required this.sourceUrl});
  @override List<Object?> get props => [sourceId, sourceUrl];
}