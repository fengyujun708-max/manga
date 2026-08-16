package com.mangaverse.app.commercial.data

import com.mangaverse.app.core.prefs.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 广告观看时长业务仓库。
 *
 * 商业模式（广告驱动，无能量/无会员）：
 * - 每日免费 30 分钟观看时长
 * - 看 1 次广告 +30 分钟
 * - 每日最多 2 次广告，两次间隔 10 分钟
 * - 看完当日全部广告 → 当天 24 点前免费无限观看
 */
@Singleton
class EnergyRepository @Inject constructor(
    private val settings: AppSettings,
) {

    data class WatchTimeState(
        val freeMinutesRemaining: Int = AppSettings.WATCH_TIME_FREE_DAILY,
        val adsWatchedToday: Int = 0,
        val maxAdsPerDay: Int = AppSettings.WATCH_TIME_MAX_ADS_PER_DAY,
        val adRewardMinutes: Int = AppSettings.WATCH_TIME_AD_REWARD,
        val cooldownRemainingMs: Long = 0L,
        val isUnlimitedToday: Boolean = false,
    ) {
        val canWatchAd: Boolean
            get() = !isUnlimitedToday && adsWatchedToday < maxAdsPerDay && cooldownRemainingMs <= 0L

        val canRead: Boolean
            get() = isUnlimitedToday || freeMinutesRemaining > 0
    }

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<WatchTimeState> = _state.asStateFlow()

    /** 当前登录用户 ID（广告 userId 参数）。 */
    val currentUserId: String
        get() = settings.mangaVerseAuthUserId.ifBlank { "anonymous" }

    init {
        settings.rollWatchTimeIfNeeded()
        _state.value = readState()
    }

    private fun readState(): WatchTimeState {
        val now = System.currentTimeMillis()
        val lastAdAt = settings.watchTimeLastAdAt
        val cooldownRemaining = if (lastAdAt > 0L) {
            (AppSettings.WATCH_TIME_AD_COOLDOWN_MS - (now - lastAdAt)).coerceAtLeast(0L)
        } else {
            0L
        }
        return WatchTimeState(
            freeMinutesRemaining = settings.watchTimeFreeMinutesRemaining,
            adsWatchedToday = settings.watchTimeAdsWatchedToday,
            isUnlimitedToday = settings.isWatchTimeUnlimitedToday,
            cooldownRemainingMs = cooldownRemaining,
        )
    }

    private fun refresh() {
        _state.value = readState()
    }

    /** 刷新冷却倒计时（供 UI 定时调用）。 */
    fun refreshCooldown() {
        _state.value = readState()
    }

    /** 看广告赚时长。返回本次获得的分钟数（冷却/次数用完返回 0）。 */
    fun earnByAd(): Int {
        settings.rollWatchTimeIfNeeded()
        if (settings.isWatchTimeUnlimitedToday) return 0
        if (settings.watchTimeAdsWatchedToday >= AppSettings.WATCH_TIME_MAX_ADS_PER_DAY) return 0
        val now = System.currentTimeMillis()
        if (settings.watchTimeLastAdAt > 0L &&
            now - settings.watchTimeLastAdAt < AppSettings.WATCH_TIME_AD_COOLDOWN_MS
        ) {
            return 0
        }
        settings.watchTimeAdsWatchedToday += 1
        settings.watchTimeLastAdAt = now
        settings.watchTimeFreeMinutesRemaining += AppSettings.WATCH_TIME_AD_REWARD
        refresh()
        return AppSettings.WATCH_TIME_AD_REWARD
    }

    /** 消费阅读时长（每分钟 -1）。看完今日广告后不消耗。返回是否允许继续阅读。 */
    fun consumeForReading(): Boolean {
        settings.rollWatchTimeIfNeeded()
        if (settings.isWatchTimeUnlimitedToday) return true
        val remaining = settings.watchTimeFreeMinutesRemaining
        if (remaining <= 0) return false
        settings.watchTimeFreeMinutesRemaining = remaining - 1
        refresh()
        return true
    }

    /** 当前是否可继续阅读。 */
    fun canRead(): Boolean {
        settings.rollWatchTimeIfNeeded()
        return settings.isWatchTimeUnlimitedToday || settings.watchTimeFreeMinutesRemaining > 0
    }
}
