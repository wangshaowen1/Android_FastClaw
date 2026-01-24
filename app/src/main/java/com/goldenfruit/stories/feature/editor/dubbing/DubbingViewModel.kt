package com.kgmedia.fastfilm.feature.editor.dubbing

import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constant
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.DramasTimbreInfo
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.editor.ShortDramaEditorRepository
import com.kgmedia.fastfilm.feature.payment.points.PointsRechargeActivity
import com.kgmedia.fastfilm.feature.payment.vip.MembershipBenefitsActivity
import com.kgmedia.fastfilm.kit.PlayerFactory
import com.kgmedia.fastfilm.kit.play
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.flow.Flow

/**
 * 短剧配音
 */
class DubbingViewModel : AppRepositoryViewModel<ShortDramaEditorRepository>() {
    private val dramasId by lazy { mBundle.getString(Constants.Extra.DRAMAS_ID) }
    private val timbreId by lazy { mBundle.getString(Constants.Extra.TIMBRE) }
    val currentTimbre by lazy { ObservableField<ItemDubbingTimbreVM>() }
    val buttonText by lazy { ObservableField<String>("使用") }
    val buttonEnabled by lazy { ObservableField<Boolean>() }
    val buyPrice by lazy { ObservableField<Int>() }
    val audioPlayer by lazy {
        PlayerFactory.createPlayer(true, true).apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    currentTimbre.get()?.playing?.set(isPlaying)
                }
            })
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }

    fun select(item: ItemDubbingTimbreVM?) {
        if (currentTimbre.get() == item) return
        currentTimbre.get()?.selected?.set(false)
        currentTimbre.get()?.playing?.set(false)
        item?.selected?.set(true)
        currentTimbre.set(item)
        buttonText.set(null)
        buttonEnabled.set(false)
        buyPrice.set(null)
        item?.data?.get()?.let {
            when {
                it.selectStatus == 1 -> {
                    buttonText.set("当前使用")
                    buttonEnabled.set(true)
                }

                it.genStatus == 2 -> {
                    buttonText.set("使用")
                    buttonEnabled.set(true)
                }

                it.genStatus == 3 -> {
                    buttonText.set("重新配音")
                    buttonEnabled.set(true)
                }

                it.buyStatus == 1 -> {
                    buttonText.set("配音中...")
                    buttonEnabled.set(false)
                }

                else -> {
                    buttonText.set("确认")
                    buttonEnabled.set(true)
                    buyPrice.set(it.timbrePrice)
                }
            }
        }
        val url = item?.data?.get()?.modelAudioUrl
        if (url.isNullOrEmpty()) {
            audioPlayer.stop()
        } else {
            audioPlayer.play(url)
        }
    }

    fun use() {
        currentTimbre.get()?.data?.get()?.let {
            when {
                it.selectStatus == 1 -> {
                    //当前使用
                }

                it.genStatus == 2 -> {
                    //使用
                    use(it.timbreId)
                }

                it.genStatus == 3 -> {
                    retry(it.timbreId)
                }

                it.buyStatus == 1 -> {
                    //配音中...
                }

                else -> {
                    if (it.vipStatus == 1 && User.get().isVip.not()) {
                        MembershipBenefitsActivity.start(scene = MembershipBenefitsActivity.Scene.Dubbing)
                        return
                    }
                    val cost = it.timbrePrice ?: 0
                    if (cost > User.get().scoreBalance) {
                        "当前余额不足，请充值".showToast()
                        PointsRechargeActivity.start(null, false)
                        return
                    }
                    dubbing(it.timbreId)
                }
            }
        }
    }

    private fun use(timbreId: String?) {
        launchWithLoading {
            response(mRepo.shortDramasDubbingUse(dramasId, timbreId)) {
                "使用成功".showToast()
                dismiss()
                FlowEventBus.postSync(Events.UseDubbingEvent(dramasId, timbreId))
            }
        }
    }

    private fun dubbing(timbreId: String?) {
        launchWithLoading {
            response(mRepo.shortDramasDubbing(dramasId, timbreId)) {
                "使用成功".showToast()
                dismiss()
                FlowEventBus.postSync(Events.DubbingEvent(dramasId, timbreId))
            }
        }
    }

    private fun retry(timbreId: String?) {
        launchWithLoading {
            response(mRepo.shortDramasDubbingRetry(dramasId, timbreId)) {
                dismiss()
                FlowEventBus.postSync(Events.DubbingEvent(dramasId, timbreId))
            }
        }
    }

    /**
     * 刷新
     */
    fun timbres(): Flow<PagingData<DramasTimbreInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            pagingSourceFactory = { DubbingTimbrePagingSource(dramasId, timbreId) }
        ).flow.cachedIn(viewModelScope)
    }

}