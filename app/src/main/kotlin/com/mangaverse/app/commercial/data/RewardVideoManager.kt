package com.mangaverse.app.commercial.data

import android.content.Context
import com.mangaverse.app.BuildConfig
import com.tb.mob.TbManager
import com.tb.mob.bean.Position
import com.ads.admob.enums.SdkEnum
import com.tb.mob.config.TbInitConfig
import com.tb.mob.config.TbInteractionConfig
import com.tb.mob.config.TbSplashConfig
import com.tb.mob.config.TbRewardVideoConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 百益/TB 广告 SDK 封装（激励视频）。
 *
 * 职责：
 * 1. Application 启动时初始化 TbManager（[init]）
 * 2. 提供 [showRewardVideo] 加载并展示激励视频
 * 3. 广告触发奖励（onRewardVerify）后调用 [onRewardVerified] 发放观看时长
 */
@Singleton
class RewardVideoManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val energyRepository: EnergyRepository,
    private val foregroundActivityHolder: com.mangaverse.app.core.ui.util.ForegroundActivityHolder,
) {

    data class AdState(
        val isInitialized: Boolean = false,
        val isAdLoading: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(AdState())
    val state: StateFlow<AdState> = _state.asStateFlow()

    /** 初始化广告 SDK（Application#onCreate 调用）。 */
    fun init(onInit: ((Boolean) -> Unit)? = null) {
        if (_state.value.isInitialized) {
            onInit?.invoke(true)
            return
        }
        val config = TbInitConfig.Builder()
            .appId(BuildConfig.BAIYI_APP_ID)
            .initList(
                java.util.Vector<SdkEnum>().apply {
                    add(SdkEnum.TYPE_CSJ)
                    add(SdkEnum.TYPE_GDT)
                    add(SdkEnum.TYPE_KS)
                    add(SdkEnum.TYPE_BD)
                    add(SdkEnum.TYPE_SigMob)
                    add(SdkEnum.TYPE_HR)
                    add(SdkEnum.TYPE_RichMob)
                    add(SdkEnum.TYPE_BeiZi)
                    add(SdkEnum.TYPE_AdGain)
                },
            )
            .build()
        TbManager.init(context, config, object : TbManager.IsInitListener {
            override fun onFail(p0: String?) {
                _state.value = AdState(isInitialized = false, error = p0)
                android.util.Log.e("AdSdk", "baiyi init fail appId=" + BuildConfig.BAIYI_APP_ID + " error=" + p0)
                onInit?.invoke(false)
            }

            override fun onSuccess() {
                _state.value = AdState(isInitialized = true)
                onInit?.invoke(true)
            }

            override fun onDpSuccess() {
            }
        })
    }

    /**
     * 展示激励视频广告。
     *
     * @param orientation 期望视频方向
     * @param onReward 广告触发激励时回调（发放时长）
     * @param onFail 加载/展示失败回调
     * @param onClose 关闭回调（无论是否触发奖励）
     */
    fun showRewardVideo(
        orientation: TbManager.Orientation = TbManager.Orientation.VIDEO_VERTICAL,
        onReward: () -> Unit,
        onFail: ((String) -> Unit)? = null,
        onClose: (() -> Unit)? = null,
    ) {
        if (!_state.value.isInitialized) {
            init { success ->
                if (success) {
                    loadAndShow(orientation, onReward, onFail, onClose)
                } else {
                    onFail?.invoke("广告 SDK 未初始化")
                }
            }
            return
        }
        loadAndShow(orientation, onReward, onFail, onClose)
    }

    private fun loadAndShow(
        orientation: TbManager.Orientation,
        onReward: () -> Unit,
        onFail: ((String) -> Unit)?,
        onClose: (() -> Unit)?,
    ) {
        _state.value = _state.value.copy(isAdLoading = true, error = null)
        val config = TbRewardVideoConfig.Builder()
            .codeId(BuildConfig.BAIYI_REWARD_VIDEO_ID)
            .userId(energyRepository.currentUserId)
            .orientation(orientation)
            .build()

        val activity = foregroundActivityHolder.current
        if (activity == null) {
            _state.value = _state.value.copy(isAdLoading = false, error = "广告窗口不可用，请重试")
            onFail?.invoke("广告窗口不可用，请重试")
            return
        }
        TbManager.loadRewardVideo(
            config,
            activity,
            object : TbManager.RewardVideoLoadListener {
                override fun onFail(s: String) {
                    _state.value = _state.value.copy(isAdLoading = false, error = s)
                    onFail?.invoke(s)
                }

                override fun onExposure(orderNo: String, position: Position) {
                    _state.value = _state.value.copy(isAdLoading = false)
                }

                override fun getSDKID(integer: Int, s: String) {
                }

                override fun onClick() {
                }

                override fun onClose() {
                    _state.value = _state.value.copy(isAdLoading = false)
                    onClose?.invoke()
                }

                override fun onRewardVideoCached(position: com.ads.admob.bean.RewardPosition) {
                }

                override fun onSkippedVideo() {
                }

                override fun onRewardVerify() {
                    // 激励视频触发激励（观看时长达标或播放完毕）
                    onReward.invoke()
                }
            },
        )
    }


    /**
     * 展示开屏广告（应用启动时调用）。
     *
     * @param activity 当前启动 Activity（需要真实窗口宿主）
     * @param onFinish 广告关闭或失败后回调（进入主界面）
     */
    fun showSplash(
        activity: android.app.Activity,
        container: android.view.ViewGroup?,
        onFinish: () -> Unit,
    ) {
        if (!_state.value.isInitialized) {
            init { success ->
                if (success) {
                    loadAndShowSplash(activity, container, onFinish)
                } else {
                    onFinish()
                }
            }
            return
        }
        loadAndShowSplash(activity, container, onFinish)
    }

    private fun loadAndShowSplash(
        activity: android.app.Activity,
        container: android.view.ViewGroup?,
        onFinish: () -> Unit,
    ) {
        _state.value = _state.value.copy(isAdLoading = true, error = null)
        val config = TbSplashConfig.Builder()
            .codeId(BuildConfig.BAIYI_SPLASH_ID)
            .apply { if (container != null) container(container) }
            .build()
        TbManager.loadSplash(
            config,
            activity,
            object : TbManager.SplashLoadListener {
                override fun onFail(s: String) {
                    _state.value = _state.value.copy(isAdLoading = false, error = s)
                    onFinish()
                }

                override fun onDismiss() {
                    _state.value = _state.value.copy(isAdLoading = false)
                    onFinish()
                }

                override fun onExposure(position: Position) {
                    _state.value = _state.value.copy(isAdLoading = false)
                }

                override fun onClicked() {
                }

                override fun onTick(l: Long) {
                }
            },
        )
    }

    /**
     * 展示插屏广告（如阅读器翻页、返回列表等场景）。
     */
    fun showInterstitial(
        onDismiss: (() -> Unit)? = null,
        onFail: ((String?) -> Unit)? = null,
    ) {
        if (!_state.value.isInitialized) {
            init { success ->
                if (success) {
                    loadAndShowInterstitial(onDismiss, onFail)
                } else {
                    onFail?.invoke("广告 SDK 未初始化")
                }
            }
            return
        }
        loadAndShowInterstitial(onDismiss, onFail)
    }

    private fun loadAndShowInterstitial(
        onDismiss: (() -> Unit)?,
        onFail: ((String?) -> Unit)?,
    ) {
        _state.value = _state.value.copy(isAdLoading = true, error = null)
        val config = TbInteractionConfig.Builder()
            .codeId(BuildConfig.BAIYI_INTERSTITIAL_ID)
            .build()

        val activity = foregroundActivityHolder.current
        if (activity == null) {
            _state.value = _state.value.copy(isAdLoading = false, error = "广告窗口不可用，请重试")
            onFail?.invoke("广告窗口不可用，请重试")
            return
        }
        TbManager.loadInteraction(
            config,
            activity,
            object : TbManager.InteractionLoadListener {
                override fun getSDKID(p0: Int?, p1: String?) {
                }

                override fun onFail(p0: String?) {
                    _state.value = _state.value.copy(isAdLoading = false, error = p0)
                    onFail?.invoke(p0)
                }

                override fun onDismiss() {
                    _state.value = _state.value.copy(isAdLoading = false)
                    onDismiss?.invoke()
                }

                override fun onClicked() {
                }

                override fun onExposure(p0: Position?) {
                    _state.value = _state.value.copy(isAdLoading = false)
                }

                override fun onVideoReady() {
                }

                override fun onVideoComplete() {
                }
            },
        )
    }
}
