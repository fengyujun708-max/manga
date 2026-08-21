import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:get_it/get_it.dart';
import '../features/auth/bloc/auth_bloc.dart';
import '../features/auth/view/login_page.dart';
import '../features/home/view/home_page.dart';
import '../features/library/view/library_page.dart';
import '../features/discover/view/source_detail_page.dart';
import '../features/discover/view/source_comic_page.dart';
import '../features/discover/view/source_reader_page.dart';
import '../features/discover/view/source_category_page.dart';
import '../features/discover/view/discover_page.dart';
import '../features/community/view/community_page.dart';
import '../features/profile/view/profile_page.dart';
import '../features/sources/view/source_market_page.dart';
import '../core/network/api_client.dart';
import '../features/search/view/search_page.dart';
import '../features/comic/view/comic_detail_page.dart';
import '../features/settings/view/settings_page.dart';
import '../features/request/view/request_page.dart';
import 'router/app_shell.dart';
import 'theme/theme.dart';

class ManjieApp extends StatelessWidget {
  const ManjieApp({super.key});

  @override
  Widget build(BuildContext context) {
    final router = GoRouter(
      initialLocation: '/home',
      redirect: (context, state) {
        final authState = context.read<AuthBloc>().state;
        final isLoggedIn = authState is AuthAuthenticated;
        final isAuthPage = state.matchedLocation == '/login';

        if (!isLoggedIn && !isAuthPage) return '/login';
        if (isLoggedIn && isAuthPage) return '/home';
        return null;
      },
      routes: [
        GoRoute(path: '/login', builder: (_, __) => const LoginPage()),
        GoRoute(path: '/source-manager', builder: (_, __) => const SourceMarketPage()),

        GoRoute(
          path: '/discover/source/:id',
          builder: (_, state) => SourceDetailPage(
            sourceId: state.pathParameters['id']!,
            sourceName: state.uri.queryParameters['name'] ?? '',
          ),
        ),
        GoRoute(
          path: '/source/:sourceId/comic/:comicId',
          builder: (_, state) => SourceComicPage(
            sourceId: state.pathParameters['sourceId']!,
            comicId: state.pathParameters['comicId']!,
          ),
        ),
        GoRoute(
          path: '/source/:sourceId/reader/:comicId/:epId',
          builder: (_, state) => SourceReaderPage(
            sourceId: state.pathParameters['sourceId']!,
            comicId: state.pathParameters['comicId']!,
            epId: state.pathParameters['epId']!,
          ),
        ),
        GoRoute(
          path: '/source/:sourceId/category',
          builder: (_, state) => SourceCategoryPage(
            sourceId: state.pathParameters['sourceId']!,
            initialCategory: state.uri.queryParameters['initial'],
          ),
        ),
            GoRoute(path: '/search', builder: (_, __) => const SearchPage()),
        GoRoute(path: '/comic/:id', builder: (_, state) => ComicDetailPage(comicId: state.pathParameters['id']!)),
        GoRoute(path: '/settings', builder: (_, __) => const SettingsPage()),
        GoRoute(path: '/community/create', builder: (_, __) => const CreatePostPage()),
        GoRoute(path: '/request/:type', builder: (_, state) => RequestPage(type: state.pathParameters['type'] ?? 'manga')),
        ShellRoute(
          builder: (_, __, child) => AppShell(child: child),
          routes: [
            GoRoute(path: '/home', builder: (_, __) => const HomePage()),
            GoRoute(path: '/discover', builder: (_, __) => const DiscoverPage()),
            GoRoute(path: '/library', builder: (_, __) => const LibraryPage()),
            GoRoute(path: '/community', builder: (_, __) => const CommunityPage()),
            GoRoute(path: '/profile', builder: (_, __) => const ProfilePage()),
          ],
        ),
      ],
    );

    return MaterialApp.router(
      title: '漫界',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      darkTheme: AppTheme.dark,
      themeMode: ThemeMode.dark,
      routerConfig: router,
    );
  }
}