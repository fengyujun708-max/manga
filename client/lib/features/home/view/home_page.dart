import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'package:get_it/get_it.dart';
import '../../../app/theme/theme.dart';
import '../../../core/network/api_client.dart';
import '../bloc/home_bloc.dart';

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (_) => HomeBloc(apiClient: GetIt.instance<ApiClient>())..add(HomeLoadRequested()),
      child: Scaffold(
        body: BlocBuilder<HomeBloc, HomeState>(
          builder: (ctx, state) {
            return CustomScrollView(
              physics: const BouncingScrollPhysics(),
              slivers: [
                // 顶部 — 渐变透明
                SliverAppBar(
                  floating: true, snap: true,
                  backgroundColor: Colors.transparent, elevation: 0,
                  title: Row(children: [
                    ShaderMask(
                      shaderCallback: (b) => AppTheme.primaryGradient.createShader(b),
                      child: const Text('漫界', style: TextStyle(fontWeight: FontWeight.w800, fontSize: 22, color: Colors.white, letterSpacing: 1)),
                    ),
                    const Spacer(),
                    _iconBtn(ctx, Icons.search_rounded, () => context.push('/search')),
                    const SizedBox(width: 8),
                    _iconBtn(ctx, Icons.notifications_none_rounded, () {}),
                  ]),
                ),
                if (state is HomeLoading)
                  const SliverFillRemaining(child: Center(child: CircularProgressIndicator(color: AppTheme.primary, strokeWidth: 2)))
                else if (state is HomeError)
                  SliverFillRemaining(child: _error(ctx, state.message))
                else if (state is HomeLoaded)
                  _content(ctx, state)
                else
                  const SliverToBoxAdapter(child: SizedBox()),
              ],
            );
          },
        ),
      ),
    );
  }

  Widget _iconBtn(BuildContext ctx, IconData icon, VoidCallback tap) {
    return GestureDetector(
      onTap: tap,
      child: Container(
        width: 40, height: 40,
        decoration: BoxDecoration(color: AppTheme.surfaceLight.withValues(alpha: 0.5), borderRadius: BorderRadius.circular(14)),
        child: Icon(icon, size: 20, color: AppTheme.textPrimary),
      ),
    );
  }

  Widget _error(BuildContext ctx, String msg) {
    return Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
      Icon(Icons.cloud_off_rounded, size: 48, color: AppTheme.textTertiary),
      const SizedBox(height: 12),
      Text(msg, style: TextStyle(color: AppTheme.textSecondary, fontSize: 14)),
      const SizedBox(height: 16),
      GlowButton(width: 160, height: 42,
        onPressed: () => ctx.read<HomeBloc>().add(HomeRefreshRequested()),
        child: const Text('重试', style: TextStyle(fontSize: 14, color: Colors.white))),
    ]));
  }

  Widget _content(BuildContext ctx, HomeLoaded state) {
    final d = state.homeData;
    return SliverList(delegate: SliverChildListDelegate([
      if (d.banner.isNotEmpty) _Hero(banner: d.banner.first),
      ...d.sections.map((s) => _section(ctx, s)).toList(),
      const SizedBox(height: 100),
    ]));
  }

  Widget _section(BuildContext ctx, HomeSection s) {
    if (s.items.isEmpty) return const SizedBox.shrink();
    return Column(children: [
      _SectionHeader(title: s.title, onSeeAll: () => GoRouter.of(ctx).push('/discover')),
      SizedBox(
        height: 250,
        child: ListView.builder(
          scrollDirection: Axis.horizontal, physics: const BouncingScrollPhysics(),
          padding: const EdgeInsets.symmetric(horizontal: 16),
          itemCount: s.items.length,
          itemBuilder: (_, i) {
            final item = s.items[i];
            return _ComicCard(
              title: item.title, author: item.author, rating: item.rating, chapter: item.chapter,
              onTap: () => GoRouter.of(ctx).push('/comic/${item.id}'),
            );
          },
        ),
      ),
    ]);
  }
}

class _SectionHeader extends StatelessWidget {
  final String title; final VoidCallback? onSeeAll;
  const _SectionHeader({required this.title, this.onSeeAll});
  @override
  Widget build(BuildContext ctx) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 28, 16, 12),
      child: Row(children: [
        Container(width: 3, height: 18, decoration: BoxDecoration(gradient: AppTheme.primaryGradient, borderRadius: BorderRadius.circular(2))),
        const SizedBox(width: 10),
        Text(title, style: Theme.of(ctx).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700)),
        const Spacer(),
        if (onSeeAll != null)
          GestureDetector(
            onTap: onSeeAll,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
              decoration: BoxDecoration(color: AppTheme.primary.withValues(alpha: 0.1), borderRadius: BorderRadius.circular(20)),
              child: Text('查看全部', style: TextStyle(color: AppTheme.primary, fontSize: 12, fontWeight: FontWeight.w600)),
            ),
          ),
      ]),
    );
  }
}

class _Hero extends StatelessWidget {
  final BannerItem banner;
  const _Hero({required this.banner});
  @override
  Widget build(BuildContext ctx) {
    return Container(
      height: 400, margin: const EdgeInsets.fromLTRB(16, 8, 16, 8),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(AppTheme.radiusXl),
        gradient: const LinearGradient(
          begin: Alignment.topLeft, end: Alignment.bottomRight,
          colors: [Color(0xFF6366F1), Color(0xFF1B1B30), Color(0xFF0F0F23)],
        ),
        boxShadow: AppTheme.cardShadow,
      ),
      child: Stack(children: [
        Positioned(right: -60, top: -60, child: _circle(200, 0.05)),
        Positioned(left: -40, bottom: -40, child: _circle(160, 0.03)),
        Positioned(right: 30, top: 40, child: _circle(80, 0.06)),
        Padding(
          padding: const EdgeInsets.all(24),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisAlignment: MainAxisAlignment.end, children: [
            if (banner.badge.isNotEmpty) ...[
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                decoration: BoxDecoration(gradient: AppTheme.accentGradient, borderRadius: BorderRadius.circular(16)),
                child: Text(banner.badge, style: const TextStyle(fontSize: 12, color: Colors.white, fontWeight: FontWeight.w600)),
              ),
              const SizedBox(height: 14),
            ],
            Text(banner.title, style: Theme.of(ctx).textTheme.headlineLarge?.copyWith(
              fontWeight: FontWeight.w800, fontSize: 28,
              shadows: [const Shadow(blurRadius: 12, color: Colors.black54)])),
            if (banner.description.isNotEmpty) ...[
              const SizedBox(height: 10),
              Text(banner.description, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13, height: 1.5), maxLines: 2, overflow: TextOverflow.ellipsis),
            ],
            const SizedBox(height: 20),
            Row(children: [
              GlowButton(
                width: null, height: 46, gradient: AppTheme.accentGradient,
                onPressed: () {},
                child: const Row(mainAxisSize: MainAxisSize.min, children: [
                  Icon(Icons.play_arrow_rounded, size: 20, color: Colors.white),
                  SizedBox(width: 6),
                  Text('开始阅读', style: TextStyle(fontSize: 14, color: Colors.white, fontWeight: FontWeight.w600)),
                ]),
              ),
              const SizedBox(width: 12),
              GestureDetector(
                onTap: () {},
                child: Container(
                  width: 46, height: 46,
                  decoration: BoxDecoration(
                    color: AppTheme.glassFill, borderRadius: BorderRadius.circular(AppTheme.radiusMd),
                    border: Border.all(color: AppTheme.glassBorder, width: 0.5),
                  ),
                  child: const Icon(Icons.bookmark_border_rounded, color: AppTheme.textPrimary, size: 20),
                ),
              ),
            ]),
          ]),
        ),
      ]),
    );
  }
  Widget _circle(double s, double a) =>
    Container(width: s, height: s, decoration: BoxDecoration(shape: BoxShape.circle, color: Colors.white.withValues(alpha: a)));
}

class _ComicCard extends StatelessWidget {
  final String title, author, chapter; final double rating; final VoidCallback onTap;
  const _ComicCard({required this.title, required this.author, this.rating = 0, this.chapter = '', required this.onTap});
  @override
  Widget build(BuildContext ctx) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: 150, margin: const EdgeInsets.only(right: 14),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Expanded(
            child: Container(
              decoration: BoxDecoration(
                gradient: AppTheme.cardGradient,
                borderRadius: BorderRadius.circular(AppTheme.radiusMd),
                boxShadow: AppTheme.cardShadow,
              ),
              child: Stack(children: [
                Center(child: Icon(Icons.menu_book_rounded, size: 40, color: AppTheme.textTertiary)),
                if (rating > 0)
                  Positioned(top: 8, right: 8,
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
                      decoration: BoxDecoration(color: Colors.black.withValues(alpha: 0.55), borderRadius: BorderRadius.circular(10)),
                      child: Row(mainAxisSize: MainAxisSize.min, children: [
                        const Icon(Icons.star_rounded, size: 13, color: AppTheme.accent),
                        const SizedBox(width: 3),
                        Text(rating.toStringAsFixed(1), style: const TextStyle(fontSize: 11, color: Colors.white, fontWeight: FontWeight.w600)),
                      ]),
                    )),
                // 渐变遮罩
                Positioned(bottom: 0, left: 0, right: 0, height: 60,
                  child: Container(
                    decoration: BoxDecoration(
                      borderRadius: const BorderRadius.only(bottomLeft: Radius.circular(AppTheme.radiusMd), bottomRight: Radius.circular(AppTheme.radiusMd)),
                      gradient: LinearGradient(begin: Alignment.topCenter, end: Alignment.bottomCenter, colors: [Colors.transparent, Colors.black.withValues(alpha: 0.4)]),
                    ),
                  )),
              ]),
            ),
          ),
          const SizedBox(height: 8),
          Text(title, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: AppTheme.textPrimary), maxLines: 1, overflow: TextOverflow.ellipsis),
          const SizedBox(height: 2),
          Row(children: [
            Expanded(child: Text(author, style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary), maxLines: 1, overflow: TextOverflow.ellipsis)),
            if (chapter.isNotEmpty)
              Text(chapter, style: const TextStyle(fontSize: 10, color: AppTheme.textTertiary)),
          ]),
        ]),
      ),
    );
  }
}
