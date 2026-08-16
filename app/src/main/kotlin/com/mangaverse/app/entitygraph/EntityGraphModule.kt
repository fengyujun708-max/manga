package com.mangaverse.app.entitygraph

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.mangaverse.app.entitygraph.data.DefaultEntityBindingMatcher
import com.mangaverse.app.entitygraph.data.DefaultEntityGraphSourceAdapter
import com.mangaverse.app.entitygraph.domain.EntityBindingMatcher
import com.mangaverse.app.entitygraph.domain.EntityGraphSourceAdapter
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface EntityGraphModule {

	@Binds
	@Singleton
	fun bindEntityBindingMatcher(
		impl: DefaultEntityBindingMatcher,
	): EntityBindingMatcher

	@Binds
	@Singleton
	fun bindEntityGraphSourceAdapter(
		impl: DefaultEntityGraphSourceAdapter,
	): EntityGraphSourceAdapter
}
