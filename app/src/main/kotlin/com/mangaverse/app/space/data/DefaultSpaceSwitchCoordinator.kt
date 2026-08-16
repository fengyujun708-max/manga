package com.mangaverse.app.space.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.mangaverse.app.space.domain.SpaceId
import com.mangaverse.app.space.domain.SpaceProgressFlusher
import com.mangaverse.app.space.domain.SpaceRepository
import com.mangaverse.app.space.domain.SpaceSwitchAvailability
import com.mangaverse.app.space.domain.SpaceSwitchCoordinator
import com.mangaverse.app.space.domain.SpaceSwitchOrigin
import com.mangaverse.app.space.domain.SpaceSwitchResult
import com.mangaverse.app.space.domain.SpaceSwitchState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSpaceSwitchCoordinator @Inject constructor(
	private val spaceRepository: SpaceRepository,
) : SpaceSwitchCoordinator {

	private val mutex = Mutex()
	private val mutableState = MutableStateFlow(SpaceSwitchState())
	override val state: StateFlow<SpaceSwitchState> = mutableState.asStateFlow()

	override suspend fun requestSwitch(
		target: SpaceId,
		origin: SpaceSwitchOrigin,
		availability: SpaceSwitchAvailability,
		progressFlusher: SpaceProgressFlusher,
	): SpaceSwitchResult = mutex.withLock {
		if (target == spaceRepository.activeSpace.value) {
			return@withLock SpaceSwitchResult.AlreadyActive(target)
		}
		when (availability) {
			SpaceSwitchAvailability.UNAVAILABLE -> return@withLock SpaceSwitchResult.Unavailable
			SpaceSwitchAvailability.CONFIRM_REQUIRED -> return@withLock SpaceSwitchResult.ConfirmationRequired
			else -> Unit
		}
		mutableState.value = SpaceSwitchState(
			inProgress = true,
			targetSpaceId = target,
			origin = origin,
		)
		try {
			if (availability == SpaceSwitchAvailability.SAVE_AND_SWITCH) {
				progressFlusher.flush()
			}
			spaceRepository.activate(target)
			SpaceSwitchResult.Success(target)
		} catch (error: Throwable) {
			if (error is CancellationException) throw error
			SpaceSwitchResult.Failed(error)
		} finally {
			mutableState.value = SpaceSwitchState()
		}
	}
}
