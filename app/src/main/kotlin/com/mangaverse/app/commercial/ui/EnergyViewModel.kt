package com.mangaverse.app.commercial.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangaverse.app.commercial.data.EnergyRepository
import com.mangaverse.app.commercial.data.RewardVideoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchTimeUiEvent(
    val message: String? = null,
)

@HiltViewModel
class EnergyViewModel @Inject constructor(
    private val repository: EnergyRepository,
    private val rewardVideoManager: RewardVideoManager,
) : ViewModel() {

    val uiState: StateFlow<EnergyRepository.WatchTimeState> = repository.state

    /** 广告 SDK 初始化状态（诊断用） */
    val adState: StateFlow<RewardVideoManager.AdState> = rewardVideoManager.state

    private val _events = MutableSharedFlow<WatchTimeUiEvent>()
    val events: SharedFlow<WatchTimeUiEvent> = _events.asSharedFlow()

    init {
        // 倒计时刷新冷却时间
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                repository.refreshCooldown()
            }
        }
    }

    /**
     * 看广告赚时长：
     * 1. 本地校验（次数/冷却/已解锁）
     * 2. 展示激励视频
     * 3. 触发奖励（onRewardVerify）→ 发放时长
     */
    fun watchAd() {
        val state = repository.state.value
        when {
            state.isUnlimitedToday -> {
                _events.tryEmit(WatchTimeUiEvent("今日已解锁无限观看"))
            }
            state.adsWatchedToday >= state.maxAdsPerDay -> {
                _events.tryEmit(WatchTimeUiEvent("今日广告次数已用完"))
            }
            state.cooldownRemainingMs > 0 -> {
                _events.tryEmit(WatchTimeUiEvent("广告冷却中，请稍后再试"))
            }
            else -> {
                rewardVideoManager.showRewardVideo(
                    onReward = {
                        val gained = repository.earnByAd()
                        if (gained > 0) {
                            _events.tryEmit(WatchTimeUiEvent("观看广告成功，获得 +$gained 分钟观看时长"))
                        } else {
                            _events.tryEmit(WatchTimeUiEvent(repository.state.value.let {
                                when {
                                    it.isUnlimitedToday -> "今日已解锁无限观看"
                                    else -> "奖励已到账"
                                }
                            }))
                        }
                    },
                    onFail = { msg ->
                        _events.tryEmit(WatchTimeUiEvent(msg))
                    },
                )
            }
        }
    }
}
