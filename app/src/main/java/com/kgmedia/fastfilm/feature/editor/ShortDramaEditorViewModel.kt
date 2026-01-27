package com.kgmedia.fastfilm.feature.editor

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.Player
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.common.ext.formatDuration
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaSegmentSubtitleInfo
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.payment.points.FirstTimeRechargeDialog
import com.kgmedia.fastfilm.feature.payment.points.FirstTimeUnlockDialog
import com.kgmedia.fastfilm.kit.media.PlayerFactory
import com.kgmedia.fastfilm.kit.media.play
import com.kgmedia.fastfilm.kit.media.setPlayUrl
import com.kgmedia.fastfilm.widget.LoadStateViewHolder
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 短剧编辑
 */
class ShortDramaEditorViewModel : AppRepositoryViewModel<ShortDramaEditorRepository>() {
    val dramasId by lazy { mBundle.getString(Constants.Extra.DRAMAS_ID) }
    private var unlock = false
    val loadState by lazy { ObservableInt() }
    val shortDrama by lazy { ObservableField<ShortDramaInfo>() }
    val currentSegment by lazy { ObservableField<ItemShortDramaSegmentVM>() }
    val currentSegmentSubtitle by lazy { ObservableField<ShortDramaSegmentSubtitleInfo>() }
    val currentSegmentAudioDuration by lazy { ObservableInt(0) }
    val scrollToItem by lazy { MutableLiveData<Int>(-1) }
    val adapter by lazy { QuickAdapter<ItemShortDramaSegmentVM>(R.layout.item_short_drama_editor_segment, emptyList()) }
    val nextStepEnabled by lazy { ObservableBoolean(false) }
    val firstGenLocked by lazy { ObservableBoolean(false) }
    private var loopGeneratingSegmentsJob: Job? = null

    /**
     * 字幕配音中
     */
    val dubbing by lazy { ObservableBoolean(false) }
    private var loopDubbingStatusJob: Job? = null
    val audioPlayer by lazy {
        PlayerFactory.createPlayer(true, autoAudioFocus = true).apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    when (playbackState) {
                        Player.STATE_READY -> {
                            updateAudioDuration()
                        }

                        Player.STATE_ENDED -> {
                            playNext()
                        }
                    }
                }
            })
        }
    }
    val playing by lazy { ObservableBoolean(true) }
    val durationText by lazy { ObservableField<String>() }
    private var startPlay = false

    private var firstTimeRechargeDialog: FirstTimeRechargeDialog? = null
    private var firstTimeUnlockDialog: FirstTimeUnlockDialog? = null

    override fun onCreate() {
        super.onCreate()
        unlock = mBundle.getBoolean(ShortDramaEditorActivity.EXTRA_UNLOCK, false)
        getStoryboardInfo()
    }

    override fun onCleared() {
        super.onCleared()
        loopGeneratingSegmentsJob?.cancel()
        audioPlayer.release()
    }

    fun getStoryboardInfo(dubbingChanged: Boolean = false) {
        loopGeneratingSegmentsJob?.cancel()
        if (shortDrama.get() == null) loadState.set(LoadStateViewHolder.LOADING)
        launch(
            block = {
                val info = withContext(Dispatchers.IO) { mRepo.shortDramasInfo(dramasId).data }
                if (info == null) {
                    updateLoadState()
                    return@launch
                }
                shortDrama.set(info)
                durationText.set("${((info.actualDuration ?: 0) * 1000L).formatDuration()}(${info.shotSegmentList?.size ?: 0}个分镜)")
                if (info.shotSegmentList?.size == adapter.items.size) {
                    info.shotSegmentList.forEachIndexed { index, info -> adapter.getItem(index)?.data?.set(info) }
                } else {
                    info.shotSegmentList?.mapIndexed { index, info ->
                        ItemShortDramaSegmentVM(info, index, firstGenLocked, nextStepEnabled).apply {
                            onItemClick.value = { v ->
                                if (this != currentSegment.get()) {
                                    select()
                                }
                            }
                        }
                    }.let { adapter.submitList(it) }
                }
                firstGenLocked.set(info.firstGenLocked())
                if (currentSegment.get()?.isLocked == true) {
                    adapter.items.firstOrNull()?.select()
                }
                nextStepEnabled.set(allSegmentsGenerated())
                val firstAudioUrl = adapter.items.firstOrNull()?.data?.get()?.shotSegmentSubtitleList?.firstOrNull()?.subtitleAudioUrl
                if (!firstAudioUrl.isNullOrEmpty()) {
                    if (!startPlay) {
                        startPlay = true
                        playNext()
                    } else if (dubbingChanged) {
                        currentSegment.get()?.select()
                    }
                } else if (currentSegment.get() == null) {
                    adapter.items.firstOrNull()?.select()
                }
                if (unlock) {
                    unlock = false
                    val shortDrama = shortDrama.get()
                    if (shortDrama?.firstGenLocked() == true) {
                        calcCost()
                    }
                }
                updateLoadState()
            },
            onError = {
                updateLoadState()
            }
        )
    }

    fun retryGenerate() {
        val shotSegmentId = currentSegment.get()?.data?.get()?.shotSegmentId ?: return
        launchWithLoading {
            response(mRepo.shortDramasSegmentImageGenerateRetry(dramasId, shotSegmentId)) {
                getStoryboardInfo()
            }
        }
    }

    fun synthesis() {
        launchWithLoading {
            val res = mRepo.shortDramasSynthesis(dramasId)
            if (res.isSuccess) {
                if (dubbing.get()) {
                    "配音完成后会自动替换".showToast()
                }
                finish()
                FlowEventBus.postSync(Events.ShortDramasUpdatedEvent(dramasId))
            } else {
                res.msg?.showToast()
            }
        }
    }

    private fun updateLoadState() {
        if (shortDrama.get() == null) {
            loadState.set(LoadStateViewHolder.RETRY)
        } else {
            loadState.set(LoadStateViewHolder.CONTENT)
            loopGeneratingSegments()
            shortDrama.get()?.let {
                if (it.subtitleAudioGenerating) loopDubbingStatus(it.timbreId)
            }
        }
    }

    /**
     * 轮询生成中片段
     */
    private fun loopGeneratingSegments() {
        loopGeneratingSegmentsJob?.cancel()
        if (anySegmentGenerating()) {
            loopGeneratingSegmentsJob = launch {
                delay(3000)
                getStoryboardInfo()
            }
        }
    }

    /**
     * 轮询字幕配音生成状态
     */
    fun loopDubbingStatus(timbreId: String?) {
        loopDubbingStatusJob?.cancel()
        loopDubbingStatusJob = launch {
            val generating = runCatching {
                val genStatus = withContext(Dispatchers.IO) { mRepo.shortDramasDubbingStatus(dramasId, timbreId).data?.genStatus }
                genStatus == 0 || genStatus == 1
            }.getOrNull() == true
            if (generating) {
                delay(3000)
                loopDubbingStatus(timbreId)
            } else if (dubbing.get()) {
                stopPlay()
                getStoryboardInfo(true)
            }
            dubbing.set(generating)
        }
    }

    fun play() {
        if (audioPlayer.isPlaying) {
            audioPlayer.pause()
            playing.set(false)
        } else {
            when (audioPlayer.playbackState) {
                Player.STATE_IDLE -> {
                    val subtitle = currentSegment.get()?.data?.get()?.shotSegmentSubtitleList?.firstOrNull()
                    currentSegmentSubtitle.set(subtitle)
                    if (subtitle?.subtitleAudioUrl.isNullOrEmpty().not()) {
                        audioPlayer.play(subtitle.subtitleAudioUrl)
                        playing.set(true)
                    }
                }

                Player.STATE_ENDED -> {
                    audioPlayer.seekTo(0)
                    audioPlayer.play()
                    playing.set(true)
                }

                else -> {
                    audioPlayer.play()
                    playing.set(true)
                }
            }
        }
    }

    fun stopPlay() {
        audioPlayer.stop()
        playing.set(false)
    }

    private fun updateAudioDuration() {
        currentSegmentAudioDuration.set(audioPlayer.duration.toInt())
    }

    private fun playNext() {
        val current = currentSegment.get()
        val currentSegmentNextSubtitle = current?.data?.get()?.shotSegmentSubtitleList?.indexOf(currentSegmentSubtitle.get())?.let {
            current.data.get()?.shotSegmentSubtitleList?.getOrNull(it + 1)
        }
        if (currentSegmentNextSubtitle?.subtitleAudioUrl.isNullOrEmpty().not()) {
            currentSegmentSubtitle.set(currentSegmentNextSubtitle)
            audioPlayer.play(currentSegmentNextSubtitle.subtitleAudioUrl)
            return
        }
        val currentIndex = adapter.items.indexOf(current)
        val next = if (currentIndex > -1 && currentIndex < adapter.items.size - 1) adapter.getItem(currentIndex + 1) else adapter.getItem(0)
        next.select()
    }

    private fun ItemShortDramaSegmentVM?.select() {
        //首次生成、未付费、且当前分镜在第3个以后，弹窗提示首次充值
        if (this?.isLocked == true) {
            calcCost()
            audioPlayer.pause()
            playing.set(false)
            return
        }
        currentSegment.get()?.selected?.set(false)
        this?.selected?.set(true)
        currentSegment.set(this)
        adapter.items.indexOf(this).takeIf { it > -1 }?.let { scrollToItem.value = it }
        val subtitle = this?.data?.get()?.shotSegmentSubtitleList?.firstOrNull()
        currentSegmentSubtitle.set(subtitle)
        if (subtitle?.subtitleAudioUrl.isNullOrEmpty().not()) {
            if (playing.get()) {
                audioPlayer.play(subtitle.subtitleAudioUrl)
            } else {
                audioPlayer.playWhenReady = false
                audioPlayer.setPlayUrl(subtitle.subtitleAudioUrl)
                audioPlayer.prepare()
            }
        } else {
            audioPlayer.clearMediaItems()
            audioPlayer.stop()
            playing.set(false)
        }
    }

    private fun firstTimeRecharge(cost: Int) {
        if (firstTimeRechargeDialog == null) {
            firstTimeRechargeDialog = FirstTimeRechargeDialog.show(null, dramasId, cost)?.apply {
                setOnDismissListener { firstTimeRechargeDialog = null }
            }
        }
    }

    private fun firstTimeUnlock(cost: Int) {
        if (firstTimeUnlockDialog == null) {
            firstTimeUnlockDialog = FirstTimeUnlockDialog.show(null, dramasId, cost)?.apply {
                setOnDismissListener { firstTimeUnlockDialog = null }
            }
        }
    }

    /**
     * 计算解锁需要消耗的积分
     */
    fun calcCost() {
        launchWithLoading {
            response(mRepo.calculatePrice(dramasId)) {
                val cost = data?.pointAmount ?: 0
                if (User.get().scoreBalance < cost) {
                    firstTimeRecharge(cost)
                } else {
                    firstTimeUnlock(cost)
                }
            }
        }
    }

    private fun allSegmentsGenerated(): Boolean {
        val info = shortDrama.get()
        if (info?.firstGenLocked() == true) {
            //首次创作未解锁，只生成前3张
            return info.shotSegmentList?.take(3)?.all { it.isGenerated() } == true
        }
        return adapter.items.all { it.data.get()?.isGenerated() == true }
    }

    fun anySegmentGenerating(): Boolean {
        return adapter.items.any { it.data.get()?.isGenerating() == true }
    }

    fun anySegmentFailed(): Boolean {
        return adapter.items.any { it.data.get()?.isFailed() == true }
    }
}