import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'package:get_it/get_it.dart';
import '../../../app/theme/theme.dart';
import '../../../core/network/api_client.dart';
import '../../auth/bloc/auth_bloc.dart';
import '../bloc/home_bloc.dart';

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (context) => HomeBloc(apiClient: GetIt.instance<ApiClient>())..add(HomeLoadRequested()),
      child: Scaffold(
        body: BlocBuilder<HomeBloc, HomeState>(
          builder: (context, state) {
            return CustomScrollView(
              physics: const BouncingScrollPhysics(),
              slivers: [
                // 顶部栏 — 透明渐变
                SliverAppBar(
                  floating: true,
                  snap: true,
                  backgroundColor: Colors.transparent,
                  elevation: 0,
                  expandedHeight: 0,
                  title: Row(
                    children: [
                      ShaderMask(
                        shaderCallback: (bounds) => AppTheme.primaryGradient.createShader(bounds),
                        child: const Text('漫界', style: TextStyle(
                          fontWeight: FontWeight.w800, fontSize: 22, color: Colors.white, letterSpacing: 1)),
                      ),
                      const Spacer(),
                      _buildIconBtn(context, Icons.search_rounded, () => context.push('/search')),
                      const SizedBox(width: 8),
                      _buildIconBtn(context, Icons.notifications_none_rounded, () {}),
                    ],
                  ),
                ),

                // 内容
                if (state is HomeLoading)
                  const SliverFillRemaining(
                    child: Center(child: CircularProgressIndicator(color: AppTheme.primary, strokeWidth: 2)),
                  )
                else if (state is HomeError)
                  SliverFillRemaining(
                    child: _buildErrorState(context, state.message),
                  )
                else if (state is HomeLoaded)
                  _buildContent(context, state)
                else
                  const SliverToBoxAdapter(child: SizedBox()),
              ],
            );
          },
        ),
      ),
    );
  }

  Widget _buildIconBtn(BuildContext context, IconData icon, VoidCallback onTap) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: 40, height: 40,
        decoration: BoxDecoration(
          color: AppTheme.surfaceLight.withValues(alpha: 0.6),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Icon(icon, size: 20, color: AppTheme.textPrimary),
      ),
    );
  }

  Widget _buildErrorState(BuildContext context, String message) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.cloud_off_rounded, size: 48, color: AppTheme.textTertiary),
          const SizedBox(height: 12),
          Text(message, style: TextStyle(color: AppTheme.textSecondary, fontSize: 14)),
          const SizedBox(height: 16),
          GlowButton(
            width: 160, height: 42,
            onPressed: () => context.read<HomeBloc>().add(HomeRefreshRequested()),
            child: const Text('重试', style: TextStyle(fontSize: 14, color: Colors.white)),
          ),
        ],
      ),
    );
  }

  Widget _buildContent(BuildContext context, HomeLoaded state) {
    final homeData = state.homeData;
    return SliverList(
      delegate: SliverChildListDelegate([
        if (homeData.banner.isNotEmpty) _HeroBanner(banner: homeData.banner.first),
        ...homeData.sections.map((s) => _buildSection(context, s)).toList(),
        const SizedBox(height: 100),
      ]),
    );
  }

  Widget _buildSection(BuildContext context, HomeSection section) {
    if (section.items.isEmpty) return const SizedBox.shrink();
    return Column(
      children: [
        _SectionHeader(title: section.title, onSeeAll: () => context.push('/discover')),
        SizedBox(
          height: 240,
          child: ListView.builder(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16),
            physics: const BouncingScrollPhysics(),
            itemCount: section.items.length,
            itemBuilder: (_, i) {
              final item = section.items[i];
              return _ComicCard(
                title: item.title,
                author: item.author,
                rating: item.rating,
                chapter: item.chapter,
                onTap: () => GoRouter.of(context).push('/comic/${item.id}'),
              );
            },
          ),
        ),
      ],
    );
  }
}

class _SectionHeader extends StatelessWidget {
  final String title;
  final VoidCallback? onSeeAll;
  const _SectionHeader({required this.title, this.onSeeAll});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 28, 16, 12),
      child: Row(
        children: [
          Container(
            width: 3, height: 18,
            decoration: BoxDecoration(
              gradient: AppTheme.primaryGradient,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(width: 10),
          Text(title, style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700)),
          const Spacer(),
          if (onSeeAll != null)
            GestureDetector(
              onTap: onSeeAll,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
                decoration: BoxDecoration(
                  color: AppTheme.primary.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text('查看全部', style: TextStyle(color: AppTheme.primary, fontSize: 12, fontWeight: FontWeight.w500)),
              ),
            ),
        ],
      ),
    );
  }
}

class _HeroBanner extends StatelessWidget {
  final BannerItem banner;
  const _HeroBanner({required this.banner});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 380,
      margin: const EdgeInsets.fromLTRB(16, 8, 16, 8),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(AppTheme.radiusXl),
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF6366F1), Color(0xFF1B1B30), Color(0xFF0F0F23)],
        ),
        boxShadow: AppTheme.cardShadow,
      ),
      child: Stack(
        children: [
          // 装饰圆
          Positioned(right: -60, top: -60, child: _decorCircle(200, 0.06)),
          Positioned(left: -40, bottom: -40, child: _decorCircle(160, 0.04)),

          Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                if (banner.badge.isNotEmpty) ...[
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
                    decoration: BoxDecoration(
                      gradient: AppTheme.accentGradient,
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Text(banner.badge, style: const TextStyle(fontSize: 12, color: Colors.white, fontWeight: FontWeight.w600)),
                  ),
                  const SizedBox(height: 14),
                ],
                Text(banner.title, style: Theme.of(context).textTheme.headlineLarge?.copyWith(
                  fontWeight: FontWeight.w800,
                  fontSize: 26,
                  shadows: [const Shadow(blurRadius: 12, color: Colors.black54)],
                )),
                if (banner.description.isNotEmpty) ...[
                  const SizedBox(height: 10),
                  Text(banner.description,
                    style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13, height: 1.5),
                    maxLines: 2, overflow: TextOverflow.ellipsis),
                ],
                const SizedBox(height: 20),
                Row(
                  children: [
                    GlowButton(
                      width: null,
                      height: 44,
                      gradient: AppTheme.accentGradient,
                      onPressed: () {},
                      child: const Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(Icons.play_arrow_rounded, size: 20, color: Colors.white),
                          SizedBox(width: 6),
                          Text('开始阅读', style: TextStyle(fontSize: 14, color: Colors.white, fontWeight: FontWeight.w600)),
                        ],
                      ),
                    ),
                    const SizedBox(width: 12),
                    GestureDetector(
                      onTap: () {},
                      child: Container(
                        width: 44, height: 44,
                        decoration: BoxDecoration(
                          color: AppTheme.glassFill,
                          borderRadius: BorderRadius.circular(AppTheme.radiusMd),
                          border: Border.all(color: AppTheme.glassBorder, width: 0.5),
                        ),
                        child: const Icon(Icons.bookmark_border_rounded, color: AppTheme.textPrimary, size: 20),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _decorCircle(double size, double alpha) {
    return Container(
      width: size, height: size,
      decoration: BoxDecoration(shape: BoxShape.circle, color: Colors.white.withValues(alpha: alpha)),
    );
  }
}

class _ComicCard extends StatelessWidget {
  final String title;
  final String author;
  final double rating;
  final String chapter;
  final VoidCallback onTap;

  const _ComicCard({
    required this.title,
    required this.author,
    this.rating = 0,
    this.chapter = '',
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: 140,
        margin: const EdgeInsets.only(right: 14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 封面
            Expanded(
              child: Container(
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [Color(0xFF27273B), Color(0xFF1B1B30)],
                  ),
                  borderRadius: BorderRadius.circular(AppTheme.radiusMd),
                  boxShadow: AppTheme.cardShadow,
                ),
                child: Stack(
                  children: [
                    Center(child: Icon(Icons.menu_book_rounded, size: 36, color: AppTheme.textTertiary)),
                    // 评分
                    if (rating > 0)
                      Positioned(
                        top: 8, right: 8,
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                          decoration: BoxDecoration(
                            color: Colors.black.withValues(alpha: 0.6),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.star_rounded, size: 12, color: AppTheme.accent),
                              const SizedBox(width: 2),
                              Text(rating.toStringAsFixed(1),
                                style: const TextStyle(fontSize: 11, color: Colors.white, fontWeight: FontWeight.w600)),
                            ],
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 8),
            Text(title,
              style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: AppTheme.textPrimary),
              maxLines: 1, overflow: TextOverflow.ellipsis),
            const SizedBox(height: 2),
            Text(author,
              style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
              maxLines: 1, overflow: TextOverflow.ellipsis),
          ],
        ),
      ),
    );
  }
}
