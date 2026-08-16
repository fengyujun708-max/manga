package com.mangaverse.app.commercial.data

import android.app.Activity
import com.mangaverse.app.commercial.data.RewardVideoManager
import com.mangaverse.app.core.ui.DefaultActivityLifecycleCallbacks
import com.mangaverse.app.core.prefs.AppSettings
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 插屏广告生命周期监听。
 *
 * 在漫画详情页 / 阅读器等主要页面退出时低频展示插屏广告。
 * 频率控制：距上次插屏 ≥ 3 分钟、当日插屏 < 5 次，避免骚扰用户。
 */
@Singleton
class InterstitialAdLifecycleCallbacks @Inject constructor(
    private val rewardVideoManager: RewardVideoManager,
    private val settings: AppSettings,
) : DefaultActivityLifecycleCallbacks {

    private val lastShownAt = AtomicLong(0L)

    companion object {
        private const val MIN_INTERVAL_MS = 3 * 60 * 1000L // 3 分钟
        private const val MAX_DAILY = 5 // 每日最多 5 次
        private val TRIGGER_ACTIVITIES = setOf(
            "com.mangaverse.app.details.ui.DetailsActivity",
            "com.mangaverse.app.reader.ui.ReaderActivity",
        )
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (!settings.isWatchTimeUnlimitedToday) return
        if (activity.javaClass.name !in TRIGGER_ACTIVITIES) return
        if (!settings.isMangaVerseLoggedIn) return

        val now = System.currentTimeMillis()
        val last = lastShownAt.get()
        if (now - last < MIN_INTERVAL_MS) return
        if (!lastShownAt.compareAndSet(last, now)) return

        // 当日插屏次数限制（存于 prefs）
        settings.rollWatchTimeIfNeeded()
        val todayCount = settings.interstitialShownToday
        if (todayCount >= MAX_DAILY) return
        settings.interstitialShownToday = todayCount + 1

        rewardVideoManager.showInterstitial()
    }
}
