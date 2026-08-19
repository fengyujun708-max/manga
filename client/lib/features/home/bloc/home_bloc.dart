import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import '../../../core/network/api_client.dart';

part 'home_event.dart';
part 'home_state.dart';

class HomeBloc extends Bloc<HomeEvent, HomeState> {
  final ApiClient apiClient;

  HomeBloc({required this.apiClient}) : super(HomeInitial()) {
    on<HomeLoadRequested>(_onLoad);
    on<HomeRefreshRequested>(_onRefresh);
  }

  Future<void> _onLoad(HomeLoadRequested event, Emitter<HomeState> emit) async {
    emit(HomeLoading());
    try {
      final response = await apiClient.get('/comic/home');
      if (response.statusCode == 200) {
        final data = response.data;
        final HomeData homeData = HomeData.fromJson(data);
        emit(HomeLoaded(homeData));
      } else {
        emit(HomeError('加载失败: ${response.statusCode}'));
      }
    } catch (e) {
      emit(HomeError('加载失败: $e'));
    }
  }

  Future<void> _onRefresh(HomeRefreshRequested event, Emitter<HomeState> emit) async {
    emit(HomeLoading());
    try {
      final response = await apiClient.get('/comic/home');
      if (response.statusCode == 200) {
        final data = response.data;
        final HomeData homeData = HomeData.fromJson(data);
        emit(HomeLoaded(homeData));
      } else {
        emit(HomeError('刷新失败: ${response.statusCode}'));
      }
    } catch (e) {
      emit(HomeError('刷新失败: $e'));
    }
  }
}

abstract class HomeEvent extends Equatable {
  const HomeEvent();
  @override List<Object?> get props => [];
}

class HomeLoadRequested extends HomeEvent {}
class HomeRefreshRequested extends HomeEvent {}

abstract class HomeState extends Equatable {
  const HomeState();
  @override List<Object?> get props => [];
}

class HomeInitial extends HomeState {}
class HomeLoading extends HomeState {}
class HomeLoaded extends HomeState {
  final HomeData homeData;
  const HomeLoaded(this.homeData);
  @override List<Object?> get props => [homeData];
}
class HomeError extends HomeState {
  final String message;
  const HomeError(this.message);
  @override List<Object?> get props => [message];
}

class HomeData {
  final List<BannerItem> banner;
  final List<HomeSection> sections;

  HomeData({required this.banner, required this.sections});

  factory HomeData.fromJson(Map<String, dynamic> json) {
    return HomeData(
      banner: (json['banner'] as List? ?? []).map((e) => BannerItem.fromJson(e)).toList(),
      sections: (json['sections'] as List? ?? []).map((e) => HomeSection.fromJson(e)).toList(),
    );
  }
}

class BannerItem {
  final String id;
  final String title;
  final String description;
  final String coverUrl;
  final String badge;
  final List<String> tags;

  BannerItem({required this.id, required this.title, required this.description, required this.coverUrl, this.badge = '', this.tags = const []});

  factory BannerItem.fromJson(Map<String, dynamic> json) {
    return BannerItem(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      description: json['description']?.toString() ?? '',
      coverUrl: json['coverUrl']?.toString() ?? '',
      badge: json['badge']?.toString() ?? '',
      tags: (json['tags'] as List?)?.map((e) => e.toString()).toList() ?? [],
    );
  }
}

class HomeSection {
  final String id;
  final String title;
  final String type;
  final List<ComicItem> items;

  HomeSection({required this.id, required this.title, required this.type, required this.items});

  factory HomeSection.fromJson(Map<String, dynamic> json) {
    return HomeSection(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      type: json['type']?.toString() ?? 'horizontal',
      items: (json['items'] as List? ?? []).map((e) => ComicItem.fromJson(e)).toList(),
    );
  }
}

class ComicItem {
  final String id;
  final String title;
  final String author;
  final String coverUrl;
  final String chapter;
  final double rating;

  ComicItem({required this.id, required this.title, required this.author, this.coverUrl = '', this.chapter = '', this.rating = 0.0});

  factory ComicItem.fromJson(Map<String, dynamic> json) {
    return ComicItem(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      author: json['author']?.toString() ?? '',
      coverUrl: json['coverUrl']?.toString() ?? '',
      chapter: json['chapter']?.toString() ?? '',
      rating: (json['rating'] as num?)?.toDouble() ?? 0.0,
    );
  }
}