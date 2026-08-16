package com.mangaverse.app.work

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.mangaverse.app.work.data.DefaultWorkResolver
import com.mangaverse.app.work.domain.WorkResolver
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface WorkModule {

	@Binds
	@Singleton
	fun bindWorkResolver(
		impl: DefaultWorkResolver,
	): WorkResolver
}
