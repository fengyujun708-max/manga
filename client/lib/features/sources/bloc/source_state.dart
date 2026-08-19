import 'package:equatable/equatable.dart';
import '../../plugins/manga_source.dart';

abstract class SourceState extends Equatable {
  const SourceState();
  @override List<Object?> get props => [];
}

class SourceInitial extends SourceState {}
class SourceLoading extends SourceState {}
class SourceLoaded extends SourceState {
  final List<MangaSource> sources;
  const SourceLoaded({required this.sources});
  @override List<Object?> get props => [sources];
}
class SourceError extends SourceState {
  final String message;
  const SourceError({required this.message});
  @override List<Object?> get props => [message];
}

class SourceUpdating extends SourceState {
  final String sourceId;
  const SourceUpdating({required this.sourceId});
  @override List<Object?> get props => [sourceId];
}
class SourceUpdatingSuccess extends SourceState {
  final String sourceId;
  const SourceUpdatingSuccess(this.sourceId);
  @override List<Object?> get props => [sourceId];
}
class SourceTestRequested extends SourceState {
  final String sourceId;
  const SourceTestRequested(this.sourceId);
  @override List<Object?> get props => [sourceId];
}
class SourceTesting extends SourceState {
  final String sourceId;
  const SourceTesting(this.sourceId);
  @override List<Object?> get props => [sourceId];
}
class SourceTestResult extends SourceState {
  final String sourceId;
  final SourceTestResultData result;
  const SourceTestResult({required this.sourceId, required this.result});
  @override List<Object?> get props => [sourceId, result];
}
class SourceTestError extends SourceState {
  final String sourceId;
  final String message;
  const SourceTestError({required this.sourceId, required this.message});
  @override List<Object?> get props => [sourceId, message];
}

class SourceDeleting extends SourceState {
  final String sourceId;
  const SourceDeleting(this.sourceId);
  @override List<Object?> get props => [sourceId];
}

class SourceInstalling extends SourceState {
  final String sourceId;
  const SourceInstalling(this.sourceId);
  @override List<Object?> get props => [sourceId];
}

class SourceMarketLoading extends SourceState {}
class SourceMarketLoaded extends SourceState {
  final List<SourceManifest> sources;
  const SourceMarketLoaded({required this.sources});
  @override List<Object?> get props => [sources];
}
class SourceMarketError extends SourceState {
  final String message;
  const SourceMarketError(this.message);
  @override List<Object?> get props => [message];
}
class SourceInstalling extends SourceState {
  final String sourceId;
  const SourceInstalling(this.sourceId);
  @override List<Object?> get props => [sourceId];
}
class SourceTestData extends Equatable {
  final String sourceId;
  final bool passed;
  final List<TestCaseResult> results;
  const SourceTestData({required this.sourceId, required this.passed, required this.results});
  @override List<Object?> get props => [sourceId, passed, results];
}
class TestCaseResult extends Equatable {
  final String name;
  final bool passed;
  final String? error;
  const TestCaseResult({required this.name, required this.passed, this.error});
  @override List<Object?> get props => [name, passed, error];
}