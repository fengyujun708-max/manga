import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../app/theme/theme.dart';
import '../../app/components/manjie_comic_card.dart';
import '../../app/components/manjie_section_header.dart';
import '../bloc/home_bloc.dart';

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (context) => HomeBloc(apiClient: ApiClient())..add(HomeLoadRequested()),
      child: Scaffold(
        body: BlocBuilder<HomeBloc, HomeState>(
          builder: (context, state) {
            return CustomScrollView(
              slivers: [
                // App Bar
                SliverAppBar(
                  floating: true,
                  pinned: true,
                  title: Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                        decoration: BoxDecoration(
                          color: Theme.of(context).colorScheme.primary,
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: const Text('漫界', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
                      ),
                      const Spacer(),
                      IconButton(icon: const Icon(Icons.search), onPressed: () => context.push('/search')),
                      IconButton(icon: const Icon(Icons.notifications_outlined), onPressed: () {}),
                    ],
                  ),
                ),

                // Content
                if (state is HomeLoading)
                  const SliverFillRemaining(child: Center(child: CircularProgressIndicator()))
                else if (state is HomeError)
                  SliverFillRemaining(child: Center(child: Text('加载失败: ${(state as HomeError).message}')))
                else if (state is HomeLoaded)
                  _buildContent(context, state)
                else
                  const SliverToBoxAdapter(child: SizedBox()),
              ],
            ),
        );
      },
    );
  }

  Widget _buildContent(BuildContext context, HomeLoaded state) {
    final homeData = state.homeData;
    return SliverList(
      delegate: SliverChildListDelegate([
        // Hero Banner
        if (homeData.banner.isNotEmpty)
          _HeroBanner(banner: homeData.banner.first),

        // Sections
        ...homeData.sections.map((section) => _buildSection(context, section)).toList(),

        const SizedBox(height: 80),
      ]),
    );
  }

  Widget _buildSection(BuildContext context, HomeSection section) {
    if (section.items.isEmpty) return const SizedBox.shrink();
    return Column(
      children: [
        _SectionHeader(title: section.title, onSeeAll: () => context.push('/discover')),
        SizedBox(
          height: 220,
          child: ListView.builder(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16),
            itemCount: section.items.length,
            itemBuilder: (_, i) => ManjieComicCard(
              title: section.items[i].title,
              subtitle: section.items[i].author,
              imageUrl: section.items[i].coverUrl,
              badge: section.items[i].chapter,
              onTap: () => GoRouter.of(context).push('/comic/${section.items[i].id}'),
            ),
          ),
        ),
      ],
    );
  }
}

class _HeroBanner extends StatelessWidget {
  final BannerItem banner;
  const _HeroBanner({required this.banner});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 400,
      margin: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [Theme.of(context).colorScheme.primary, const Color(0xFF0F3460)],
        ),
      ),
      child: Stack(
        children: [
          Positioned(
            right: -40, top: -40,
            child: Container(width: 200, height: 200,
              decoration: BoxDecoration(shape: BoxShape.circle, color: Colors.white.withOpacity(0.05)),
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                if (banner.badge.isNotEmpty)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(color: Colors.white.withOpacity(0.2), borderRadius: BorderRadius.circular(20)),
                    child: Text(banner.badge, style: const TextStyle(fontSize: 12, color: Colors.white)),
                  ),
                const SizedBox(height: 12),
                Text(banner.title, style: Theme.of(context).textTheme.headlineLarge?.copyWith(
                  fontWeight: FontWeight.bold,
                  shadows: [Shadow(blurRadius: 10, color: Colors.black.withOpacity(0.5))],
                )),
                if (banner.description.isNotEmpty) ...[
                  const SizedBox(height: 8),
                  Text(banner.description, style: const TextStyle(color: Colors.white70, fontSize: 14), maxLines: 2, overflow: TextOverflow.ellipsis),
                ],
                if (banner.tags.isNotEmpty) ...[
                  const SizedBox(height: 8),
                  Row(children: banner.tags.map((t) => Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(color: Colors.black.withOpacity(0.3), borderRadius: BorderRadius.circular(12)),
                      child: Text(t, style: const TextStyle(fontSize: 12, color: Colors.white70)),
                    ),
                  )).toList()),
                ],
                const SizedBox(height: 16),
                Row(children: [
                  ElevatedButton.icon(onPressed: () {}, icon: const Icon(Icons.play_arrow), label: const Text('开始阅读'),
                    style: ElevatedButton.styleFrom(padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12))),
                  ),
                  const SizedBox(width: 12),
                  OutlinedButton.icon(onPressed: () {}, icon: const Icon(Icons.bookmark_border), label: const Text('收藏'),
                    style: OutlinedButton.styleFrom(foregroundColor: Colors.white, side: const BorderSide(color: Colors.white54), padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12))),
                  ),
                ]),
              ],
            ),
          ),
        ],
      ),
    );
  }
}